package soluciones.chapter11.monads

import soluciones.chapter6.state.State

/**
 * Created by mglvl on 3/05/15.
 */
trait Monad[F[_]] {
  def unit[A](a: A): F[A]
  def flatMap[A,B](fa: F[A])(f: A => F[B]): F[B]

  def map[A,B](fa: F[A])(f: A => B): F[B] = flatMap(fa)( a => unit(f(a)) )

  def map2[A,B,C](fa: F[A], fb: F[B])(f: (A,B) => C): F[C] = flatMap(fa)(a => map(fb)( b => f(a,b) ))

  def traverse[A,B](la: List[A])(f: A => F[B]): F[List[B]] = la match {
    case Nil      => unit(Nil)
    case a::tla   =>
      val rest = traverse(tla)(f)
      flatMap(f(a))(b => map(rest)(tl => b::tl) )
  }

  def sequence[A](lma: List[F[A]]): F[List[A]] = traverse(lma)(fa => fa)

  def replicateM[A](n: Int, ma: F[A]): F[List[A]] = sequence(List.fill(n)(ma))

  def filterM[A](ms: List[A])(f: A => F[Boolean]): F[List[A]] = ms match {
    case Nil      => unit(Nil)
    case a::tla   =>
      val rest = filterM(tla)(f)
      flatMap(f(a)) { p =>
        if(p)
          map(rest)(tl => a :: tl)
        else
          rest
    }
  }

  def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] = { a =>
    flatMap(f(a))(g)
  }

  def flatMapByCompose[A,B](fa: F[A])(f: A => F[B]): F[B] =
    compose[F[A],A,B](identity, f)(fa)

  def join[A](mma: F[F[A]]): F[A] = flatMap(mma)(identity)

  def flatMapByJoin[A,B](fa: F[A])(f: A => F[B]): F[B] =
    join(map(fa)(f))

}

case class Id[A](value: A) extends AnyVal

object Monad {

  val listMonad = new Monad[List]{
    override def unit[A](a: A): List[A] = List(a)

    override def flatMap[A, B](fa: List[A])(f: (A) => List[B]): List[B] = fa.flatMap(f)
  }

  val optionMonad = new Monad[Option]{
    override def unit[A](a: A): Option[A] = Some(a)

    override def flatMap[A, B](fa: Option[A])(f: (A) => Option[B]): Option[B] = fa.flatMap(f)
  }

  def stateMonad[S]: Monad[({ type l[a] = State[S,a] })#l] = new Monad[({type l[a] = State[S, a]})#l] {
    override def flatMap[A, B](fa: State[S, A])(f: (A) => State[S, B]): State[S, B] = fa.flatMap(f)

    override def unit[A](a: A): State[S, A] = State.unit(a)
  }

  val idMonad = new Monad[Id] {
    override def unit[A](a: A) = Id(a)
    override def flatMap[A, B](ida: Id[A])(f: A => Id[B]): Id[B] = f(ida.value)
  }

}
