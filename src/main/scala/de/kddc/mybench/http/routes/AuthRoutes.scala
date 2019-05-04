package de.kddc.mybench.http.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.{ AuthenticationFailedRejection, Route }
import akka.http.scaladsl.server.AuthenticationFailedRejection.{ CredentialsMissing, CredentialsRejected }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.AuthenticationResult
import com.typesafe.scalalogging.LazyLogging
import de.kddc.mybench.http.{ HttpProtocol, HttpRoutes }
import de.kddc.mybench.providers.AuthProvider
import de.kddc.mybench.repositories.UserRepository.User
import de.kddc.mybench.utils.JsonWebToken
import play.api.libs.json.Json

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

//final case class AuthTokenResponse(
//  access_token: String,
//  refresh_token: String,
//  token_type: String,
//  expires_in: Long
//)
//
//final case class AuthFailure(
//  error: String,
//  description: String
//)

class AuthRoutes(implicit ec: ExecutionContext, authProvider: AuthProvider) extends HttpRoutes
  with HttpProtocol
  with LazyLogging {
  import AuthProvider._
  //  implicit val AuthTokenResponseJsonFormat = Json.format[AuthTokenResponse]

  def routes = path("auth" / "token" / "create") {
    post {
      formField('grant_type.as[String]) {
        case "password" => {
          formFields('username, 'password) {
            case (username, password) =>
              onSuccess(passwordGrant(username, password)) {
                case Right(authTokenResponse) => complete(authTokenResponse)
                case Left(authRejection) => reject(authRejection)
              }
          }
        }
        case "refresh_token" => {
          formField('refresh_token.as[String]) { refreshToken =>
            onSuccess(refreshTokenGrant(refreshToken)) {
              case Right(authTokenResponse) => complete(authTokenResponse)
              case Left(authRejection) => reject(authRejection)
            }
          }
        }
      }
    }
  }

  private def passwordGrant(username: String, password: String): Future[Either[AuthenticationFailedRejection, AuthTokenResponse]] = {
    authProvider.verifyUserCredentials(username, password).map {
      case Right(user) => Right(authProvider.createAuthTokenResponse(user))
      case Left(failure) => Left(authProvider.createAuthTokenRejection(failure))
    }
  }

  private def refreshTokenGrant(token: String): Future[Either[AuthenticationFailedRejection, AuthTokenResponse]] = {
    authProvider.verifyRefreshToken(token).map {
      case Right(user) => Right(authProvider.createAuthTokenResponse(user))
      case Left(failure) => Left(authProvider.createAuthTokenRejection(failure))
    }
  }

  //  private def createAuthTokenResponse(user: User): AuthTokenResponse = {
  //    val accessTokenLifeTime = 300.seconds
  //    val refreshTokenLifeTime = 30.days
  //    val accessToken = JsonWebToken.createToken(user, JsonWebToken.AccessToken, accessTokenLifeTime)
  //    val refreshToken = JsonWebToken.createToken(user, JsonWebToken.RefreshToken, refreshTokenLifeTime)
  //    AuthTokenResponse(accessToken, refreshToken, "Bearer", accessTokenLifeTime.toSeconds - 1)
  //  }
  //
  //  private def createRejection(failure: AuthFailure): AuthenticationFailedRejection = {
  //    AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", JsonWebToken.realm, Map("error" -> failure.error, "error_description" -> failure.description)))
  //  }
}