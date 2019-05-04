package de.kddc.mybench.providers

import java.util.UUID

import akka.http.scaladsl.model.headers.{ HttpChallenge, OAuth2BearerToken }
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, Directive1 }
import com.github.t3hnar.bcrypt._
import play.api.libs.json.OFormat
//import de.kddc.mybench.http.routes.{AuthFailure, AuthTokenResponse}
import de.kddc.mybench.repositories.UserRepository
import de.kddc.mybench.repositories.UserRepository.User
import de.kddc.mybench.utils.JsonWebToken
import play.api.libs.json.Json
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

object AuthProvider {
  final case class AuthTokenResponse(
    access_token: String,
    refresh_token: String,
    token_type: String,
    expires_in: Long)
  final case class AuthFailure(
    error: String,
    description: String)
  implicit val AuthTokenResponseJsonFormat: OFormat[AuthTokenResponse] = Json.format[AuthTokenResponse]
  type AuthResult[P] = Either[AuthFailure, P]
  val InvalidCredentials = AuthFailure("invalid_credentials", "Invalid Credentials")
  val InvalidToken = AuthFailure("invalid_token", "Could not find user provided in the token")
}

class AuthProvider(userRepository: UserRepository)(implicit ex: ExecutionContext, config: Config) {
  import AuthProvider._

  def extractToken: Directive1[Option[String]] = {
    extractCredentials.flatMap {
      case Some(OAuth2BearerToken(token)) =>
        provide(Some(token))
      case _ =>
        provide(None)
    }
  }

  def extractUser: Directive1[User] = {
    extractToken.flatMap {
      case Some(token) => {
        JsonWebToken.validateToken(token) match {
          case Right(jwtClaim) => {
            val userId = (Json.parse(jwtClaim.toJson) \ "id").as[UUID]
            onSuccess(userRepository.findById(userId)).flatMap {
              case Some(user) =>
                provide(user)
              case None =>
                reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "mybench", Map("error" -> InvalidToken.error, "error_description" -> InvalidToken.description))))
            }
          }
          case Left(failure) =>
            reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "mybench", Map("error" -> failure.error, "error_description" -> failure.description))))
        }
      }
      case None => reject(AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "mybench")))
    }
  }

  def verifyUserCredentials(username: String, password: String): Future[AuthResult[User]] = {
    userRepository.findByUsername(username).map {
      case Some(user) => {
        password.isBcryptedSafe(user.password) match {
          case Success(true) => Right(user)
          case _ => Left(InvalidCredentials)
        }
      }
      case None => Left(InvalidCredentials)
    }
  }

  def verifyRefreshToken(token: String): Future[AuthResult[User]] = {
    JsonWebToken.validateRefreshToken(token) match {
      case Right(jwtClaim) => {
        val userId = (Json.parse(jwtClaim.toJson) \ "id").as[UUID]
        userRepository.findById(userId).map {
          case Some(user) => Right(user)
          case None => Left(InvalidToken)
        }
      }
      case Left(failure) =>
        Future.successful(Left(AuthFailure(failure.error, failure.description)))
    }
  }

  def createAuthTokenResponse(user: User): AuthTokenResponse = {
    val accessTokenLifeTime = 300.seconds
    val refreshTokenLifeTime = 30.days
    val accessToken = JsonWebToken.createToken(user, JsonWebToken.AccessToken, accessTokenLifeTime)
    val refreshToken = JsonWebToken.createToken(user, JsonWebToken.RefreshToken, refreshTokenLifeTime)
    AuthTokenResponse(accessToken, refreshToken, "Bearer", accessTokenLifeTime.toSeconds - 1)
  }

  def createAuthTokenRejection(failure: AuthFailure): AuthenticationFailedRejection = {
    AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", JsonWebToken.realm, Map("error" -> failure.error, "error_description" -> failure.description)))
  }
}