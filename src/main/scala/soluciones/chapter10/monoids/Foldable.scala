package soluciones.chapter10.monoids

import soluciones.chapter3.datastructures.{Branch, Leaf, Tree}

/**
 * Created by mglvl on 24/04/15.
 */
trait Foldable[F[_]] {
  def foldRight[A,B](as: F[A])(z: B)(f: (A,B) => B): B
  def foldLeft[A,B](as: F[A])(z: B)(f: (B,A) => B): B
  def foldMap[A,B](as: F[A])(f: A => B)(monoid: Monoid[B]): B
  def concatenate[A](as: F[A])(monoid: Monoid[A]): A =
    foldLeft(as)(monoid.zero)( (a1,a2) => monoid.op(a1,a2) )
  def toList[A](as: F[A]): List[A] = foldRight(as)(Nil: List[A])(_ :: _)
}

object FoldableInstances {

  implicit val listFoldable = new Foldable[List] {
    override def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B): B = as match {
      case Nil => z
      case a :: tl => f(a,foldRight(tl)(z)(f))
    }

    override def foldLeft[A, B](as: List[A])(z: B)(f: (B, A) => B): B = {
      def _f(as: List[A], acc: B): B = as match {
        case Nil => acc
        case a :: tl => _f(tl, f(acc,a))
      }
      _f(as, z)
    }

    override def foldMap[A, B](as: List[A])(f: (A) => B)(monoid: Monoid[B]): B =
      foldLeft(as)(monoid.zero)( (b,a) => monoid.op(b,f(a)) )

  }

  implicit val indexedSeqFoldable: Foldable[IndexedSeq] = ???

  implicit val treeFoldable = new Foldable[Tree] {
    override def foldRight[A, B](as: Tree[A])(z: B)(f: (A, B) => B): B = ???

    override def foldLeft[A, B](as: Tree[A])(z: B)(f: (B, A) => B): B = {
      as match {
        case Leaf(a) => f(z,a)
        case Branch(l,r) =>
          val acc = foldLeft(l)(z)(f)
          foldLeft(r)(acc)(f)
      }
    }

    override def foldMap[A, B](as: Tree[A])(f: (A) => B)(monoid: Monoid[B]): B = ???
  }

  implicit val optionFoldable = new Foldable[Option] {

    override def foldRight[A, B](as: Option[A])(z: B)(f: (A, B) => B): B = ???

    override def foldLeft[A, B](as: Option[A])(z: B)(f: (B, A) => B): B = ???

    override def foldMap[A, B](as: Option[A])(f: (A) => B)(monoid: Monoid[B]): B = as match {
      case None => monoid.zero
      case Some(a) => f(a)
    }
  }

}
