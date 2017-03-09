package soluciones.chapter12.applicative

sealed trait Validation[+E,+A]
case class Failure[E](head: E, tail: Vector[E] = Vector()) extends Validation[E, Nothing]
case class Success[A](a: A) extends Validation[Nothing, A]

object ValidationInstances {

  implicit def ValidationApplicative[E]: Applicative[({ type F[x] = Validation[E,x]})#F] =
    new Applicative[({ type F[x] = Validation[E,x]})#F] {
      def map2[A,B,C](fa: Validation[E,A], fb: Validation[E,B])(f: (A,B) => C): Validation[E,C] =
        (fa,fb) match {
          case (Failure(h1,t1),Failure(h2,t2)) => Failure(h1, t1 ++ Vector(h2) ++ t2)
          case (e: Failure[E], _: Success[B] ) => e
          case (_: Success[A], e: Failure[E] ) => e
          case (Success(a)   , Success(b)    ) => Success(f(a,b))
        }
      def unit[A](a: => A): Validation[E,A] = Success(a)
    }

}
