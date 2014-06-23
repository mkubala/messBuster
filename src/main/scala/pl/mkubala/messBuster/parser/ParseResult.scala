package pl.mkubala.messBuster.parser

import scala.collection.immutable.Seq
import scala.util.{ Failure, Success, Try }

object ParseResult {

  type Problem = String

  type ParseResult[T] = Either[Seq[Problem], T]

  def success[T](r: T): ParseResult[T] = Right(r)

  def fail[T](cause: String): ParseResult[T] = Left(cause :: Nil)

  def from[F, T](f: F)(implicit p: Parser[F, ParseResult[T]]): Seq[ParseResult[T]] =
    p.parse(f)

  def convert[T](tryF: Traversable[Try[T]]): Seq[ParseResult[T]] =
    tryF.map {
      case Success(v) => success(v)
      case Failure(cause) => fail(cause.toString)
    }(collection.breakOut)

}