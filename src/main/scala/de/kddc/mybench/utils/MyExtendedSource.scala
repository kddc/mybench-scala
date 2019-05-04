package de.kddc.mybench.utils

import akka.stream.scaladsl.Source

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

class MyExtendedSource[T, M](val inner: Source[T, M]) extends AnyVal {
  def mapAsyncChunked(n: Int, d: FiniteDuration)(fn: Seq[T] => Future[Seq[T]]): Source[T, M] = {
    inner
      .groupedWithin(n, d)
      .mapAsync(1)(fn)
      .flatMapConcat(chunks => Source(chunks.toList))
  }
}

object MyExtendedSource {
  implicit def toMyExtendedSource[T, M](inner: Source[T, M]): MyExtendedSource[T, M] = new MyExtendedSource(inner)
}