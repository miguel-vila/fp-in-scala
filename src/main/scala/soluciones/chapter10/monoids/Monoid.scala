package soluciones.chapter10.monoids

import soluciones.chapter7.parallelism.Par
import soluciones.chapter7.parallelism.Par.Par
import soluciones.chapter8.testing.{ Gen, Prop }

/**
 * Created by mglvl on 17/04/15.
 */
trait Monoid[A] {

  def op(a: A, b : => A): A

  def zero: A

}

object Monoid {

  def apply[A](_op: (A, A) => A, _zero: A): Monoid[A] = new Monoid[A] {
    def op(a: A, b: => A): A = _op(a,b)
    def zero: A = _zero
  }

  val sumIntMonoid = apply[Int](_+_,0)

  def endoMonoid[A]: Monoid[A => A] = new Monoid[A => A] {
    def op(f1: A => A, f2: => (A => A)): A => A = f2.andThen(f1)(_)
    def zero = identity
  }

  def monoidLaws[A](m: Monoid[A], G: Gen[A]): Prop = {
    val triples = for {
      a1 <- G
      a2 <- G
      a3 <- G
    } yield (a1,a2,a3)
    val assoc = Prop.forAll(triples) { case (a1,a2,a3) =>
      m.op(a1, m.op(a2, a3)) == m.op(m.op(a1,a2), a3)
    }
    val identity = Prop.forAll(G)(a => m.op(a,m.zero) == a && m.op(m.zero,a) == a)
    assoc && identity
  }

  def foldMap[A, B](as: List[A], m: Monoid[B])(f: A => B): B =
    as.foldLeft(m.zero)((b, a) => m.op(b, f(a)))

  def foldLeft[A,B](as: List[A])(z: B)(f: (B,A) => B): B = {
    ???
  }

  def foldMapV[A, B](v: IndexedSeq[A], m: Monoid[B])(f: A => B): B = {
    if(v.length == 0) {
      m.zero
    } else if(v.length == 1){
      f(v(0))
    } else {
      val (v1,v2) = v.splitAt(v.length/2)
      m.op(foldMapV(v1,m)(f),foldMapV(v2,m)(f))
    }
  }

  def par[A](m: Monoid[A]): Monoid[Par[A]] = new Monoid[Par[A]] {
    def op(p1: Par[A], p2: => Par[A]): Par[A] = Par.map2(p1,p2)( (a1,a2) => m.op(a1,a2) )
    def zero: Par[A] = Par.unit(m.zero)
  }

  def parFoldMap[A,B](v: IndexedSeq[A], m: Monoid[B])(f: A => B): Par[B] = {
    val pm = par(m)
    if(v.length == 0) {
      pm.zero
    } else if(v.length == 1){
      Par.lazyUnit(f(v(0)))
    } else {
      val (v1,v2) = v.splitAt(v.length/2)
      pm.op(parFoldMap(v1,m)(f),parFoldMap(v2,m)(f))
    }
  }

  trait Ordered[+A]
  case object Empty extends Ordered[Nothing]
  case class OrderedSequence[A](first: A, last: A) extends Ordered[A]
  case object Unordered extends Ordered[Nothing]

  def ordered[A](v: IndexedSeq[A])(implicit comp: Ordering[A]): Boolean = {
    val m = new Monoid[Ordered[A]] {
      override def op(a: Ordered[A], b: => Ordered[A]): Ordered[A] = (a,b) match {
        case (OrderedSequence(fa,la), OrderedSequence(fb,lb)) if comp.compare(la,fb) <= 0 => OrderedSequence(fa,lb)
        case (Empty,x) => x
        case (x, Empty) => x
        case _ => Unordered
      }
      override def zero: Ordered[A] = Empty
    }
    val ordered = foldMapV(v,m)(a => OrderedSequence(a,a))
    ordered match {
      case Unordered => false
      case _ => true
    }
  }
  sealed trait WC
  case class Stub(chars: String) extends WC
  case class Part(lStub: String, words: Int, rStub: String) extends WC

  def isWordChar(c: Char): Boolean = {
    ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')
  }

  val wcMonoid: Monoid[WC] = new Monoid[WC] {
    override def zero = Stub("")
    override def op(x: WC, y: => WC): WC = (x,y) match {
      case (Stub(xs), Stub(ys)) => Stub(xs ++ ys)
      case (Stub(xs), Part(lStub,words,rStub)) => Part(xs++lStub,words,rStub)
      case (Part(lStub,words,rStub), Stub(ys)) => Part(lStub,words,rStub++ys)
      case (Part(lStubx,wordsx,_), Part(_,wordsy,rStuby)) =>
        Part(lStubx,wordsx+wordsy,rStuby)
    }
  }

  /*
  def toWC(str: String): WC = {
    val parts = str.split(" ")
    if(parts.size() <= 1)
      Stub(str)
    else
      Part(parts.head, parts.size - 2, parts.last)
  }

  def wordCount(str: String): WC = {
    if(str == "")
      wcMonoid.zero
    else {
      val (left,right) = str.splitAt(str.length / 2)
      wcMonoid.op()
    }
  }
  */


  def productMonoid[A,B](ma: Monoid[A], mb: Monoid[B]): Monoid[(A,B)] = new Monoid[(A, B)] {
    override def op(x: (A, B), y: => (A, B)): (A, B) = (x,y) match {
      case ((a1,b1),(a2,b2)) => (ma.op(a1,a2),mb.op(b1,b2))
    }

    override def zero: (A, B) = (ma.zero,mb.zero)
  }

  def functionMonoid[A,B](B: Monoid[B]): Monoid[A=>B] = new Monoid[(A) => B] {
    override def op(f1: (A) => B, f2: => (A) => B): (A) => B = a => B.op(f1(a),f2(a))

    override def zero: (A) => B = _ => B.zero
  }

  def mapMergeMonoid[K,V](V: Monoid[V]): Monoid[Map[K,V]] =
    new Monoid[Map[K,V]] {
      def zero = Map[K,V]()
      def op(a: Map[K,V], b: => Map[K,V]): Map[K,V] =
        (a.keySet ++ b.keySet).foldLeft(zero) { (acc,k) =>
          acc.updated(k,
                      V.op(
                        a.getOrElse(k, V.zero),
                        b.getOrElse(k, V.zero)
                      )
          )
        }
    }

  def bag[A](as: IndexedSeq[A]): Map[A, Int] =
    FoldableInstances.indexedSeqFoldable.foldMap(as)(a => Map(a -> 1))(mapMergeMonoid(sumIntMonoid))


}
