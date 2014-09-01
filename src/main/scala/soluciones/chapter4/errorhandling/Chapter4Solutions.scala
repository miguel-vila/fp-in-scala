package soluciones.chapter4.errorhandling

object Chapter4Solutions {

  def sequence[A](xs: List[Option[A]]): Option[List[A]] = {
    xs match {
      case Nil => Some(Nil)
      case x :: xss =>
        for {
          y <- x
          ys <- sequence(xss)
        } yield y :: ys
    }
  }

  def traverse[A, B](xs: List[A])(f: A => Option[B]): Option[List[B]] = {
    xs match {
      case Nil => Some(Nil)
      case x :: xss =>
        for {
          y <- f(x)
          ys <- traverse(xss)(f)
        } yield y :: ys
    }
  }

  def sequence2[A](xs: List[Option[A]]): Option[List[A]] = traverse(xs)(x => x)

  sealed trait Either[+E, +A] {
    def map[B](f: A => B): Either[E, B] = {
      this match {
        case Left(e) => Left(e)
        case Right(a) => Right(f(a))
      }
    }

    def flatMap[EE >: E, B](f: A => Either[EE, B]): Either[EE, B] = {
      this match {
        case Left(e) => Left(e)
        case Right(a) => f(a)
      }
    }

    def orElse[EE >: E, B >: A](other: => Either[EE, B]): Either[EE, B] = {
      this match {
        case Left(_) => other
        case Right(_) => this
      }
    }

    def map2[EE >: E, B, C](other: Either[EE, B])(f: (A, B) => C): Either[EE, C] = {
      for {
        a <- this
        b <- other
      } yield f(a, b)
    }
  }

  case class Left[+E](value: E) extends Either[E, Nothing]
  case class Right[+A](value: A) extends Either[Nothing, A]

  def sequenceEither[E, A](es: List[Either[E, A]]): Either[E, List[A]] = {
    es match {
      case Nil => Right(Nil)
      case e :: ess =>
        for {
          y <- e
          ys <- sequenceEither(ess)
        } yield y :: ys
    }
  }

  def traverseEither[E, A, B](as: List[A])(f: A => Either[E, B]): Either[E, List[B]] = {
    as match {
      case Nil => Right(Nil)
      case a :: ass =>
        for {
          y <- f(a)
          ys <- traverseEither(ass)(f)
        } yield y :: ys
    }
  }
}