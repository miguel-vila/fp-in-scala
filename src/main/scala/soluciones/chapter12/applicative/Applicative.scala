package soluciones.chapter12.applicative

/**
 * Created by mglvl on 16/07/15.
 */
trait Functor[F[_]] {
  def map[A,B](fa: F[A])(f: A => B): F[B]
}

trait Applicative[F[_]] extends Functor[F]{ self =>
  def map2[A,B,C](fa: F[A], fb: F[B])(f: (A,B) => C): F[C]
  def unit[A](a: => A): F[A]

  def map[A,B](fa: F[A])(f: A => B): F[B] = map2(fa, unit(()))( (a,_) => f(a) )

  def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]] =
    as.foldRight(unit(List.empty[B]))( (a, fbs) => map2(f(a), fbs)(_ :: _) )

  def sequence[A](as: List[F[A]]): F[List[A]] =
    as.foldRight(unit(List.empty[A]))( (a, fbs) => map2(a, fbs)(_ :: _) )

  def replicateM[A](n: Int, fa: F[A]): F[List[A]] = {
    if(n == 0) {
      unit(List.empty)
    } else {
      val rest = replicateM(n-1, fa)
      map2(fa,rest)(_ :: _)
    }
  }

  def apply[A,B](fab: F[A => B])(fa: F[A]): F[B] = map2(fab, fa)(_(_))

  def map3[A,B,C,D](fa: F[A], fb: F[B], fc: F[C])(f: (A,B,C) => D): F[D] = {
    apply(
      apply(
        apply(unit(f.curried))(fa)
      )(fb)
    )(fc)
  }

  def product[G[_]](G: Applicative[G]): Applicative[({ type f[x] = (F[x],G[x]) })#f] =
    new Applicative[({ type f[x] = (F[x],G[x]) })#f] {
      def map2[A,B,C](fga: (F[A],G[A]), fgb: (F[B],G[B]))(f: (A,B) => C): (F[C],G[C]) =
        (fga,fgb) match {
          case ((fa,ga),(fb,gb)) => (self.map2(fa,fb)(f), G.map2(ga,gb)(f))
        }
      def unit[A](a: => A): (F[A],G[A]) = ( self.unit(a), G.unit(a) )
    }

  def compose[G[_]](G: Applicative[G]): Applicative[({ type f[x] = F[G[x]] })#f] =
    new Applicative[({ type f[x] = F[G[x]] })#f] {
      def map2[A,B,C](fga: F[G[A]], fgb: F[G[B]])(f: (A,B) => C): F[G[C]] =
        self.map2(fga,fgb)( (ga,gb) => G.map2(ga,gb)(f) )
      def unit[A](a: => A): F[G[A]] = self.unit(G.unit(a))
    }

}

trait ApplicativeInTermsOfApply[F[_]] {

  def apply[A,B](fab: F[A => B])(fa: F[A]): F[B]
  def unit[A](a: => A): F[A]

  def map[A,B](fa: F[A])(f: A => B): F[B] = apply(unit(f))(fa)
  def map2[A,B,C](fa: F[A], fb: F[B])(f: (A,B) => C): F[C] = {
    apply(
      apply(unit(f.curried))(fa)
    )(fb)
  }

}
