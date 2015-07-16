package soluciones.chapter10.monoids

import soluciones.chapter7.parallelism.Par
import soluciones.chapter7.parallelism.Par.Par
import soluciones.chapter7.parallelism.Par.Par

/**
 * Created by mglvl on 17/04/15.
 */
trait Monoid[A] {

  def op(a: A, b : => A): A

  def zero: A

}

object Monoid {

  def foldMap[A, B](as: List[A], m: Monoid[B])(f: A => B): B =
    as.foldLeft(m.zero)((b, a) => m.op(b, f(a)))

  /*
  def foldLeft[A,B](as: List[A])(z: B)(f: (B,A) => B)(m: Monoid[B]): B = {
    val g: A => B = f(z,_)
    val m = new Monoid[B] {
      def op(b1: B, b2: => B): B = f(f(b1,???),???)
    }
  }
  */

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
  case class SingleElement[A](a: A) extends Ordered[A]
  case class OrderedSequence[A](first: A, last: A) extends Ordered[A]
  case object Unordered extends Ordered[Nothing]

  def ordered[A](v: IndexedSeq[A])(implicit comp: Ordering[A]): Boolean = {
    val m = new Monoid[Ordered[A]] {
      override def op(a: Ordered[A], b: => Ordered[A]): Ordered[A] = (a,b) match {
        case (SingleElement(a),SingleElement(b)) if comp.compare(a,b) <= 0 => OrderedSequence(a,b)
        case (OrderedSequence(fa,la), OrderedSequence(fb,lb)) if comp.compare(la,fb) <= 0 => OrderedSequence(fa,lb)
        case (SingleElement(a),OrderedSequence(fb,lb)) if comp.compare(a,fb) <= 0 => OrderedSequence(a,lb)
        case (OrderedSequence(fa,la), SingleElement(b)) if comp.compare(la,b) <= 0 => OrderedSequence(fa,b)
        case (Empty,x) => x
        case (x, Empty) => x
        case _ => Unordered
      }
      override def zero: Ordered[A] = Empty
    }
    val ordered = foldMapV(v,m)(a => SingleElement(a))
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

  def wc(s: String): (Option[String], Int, Option[String]) = {
    var insideWord_> = false
    var insideWord_< = false
    var words = 0
    var startWord = ""
    var wordIsAtTheStart = false
    var endWord = ""
    var wordIsAtTheEnd = false
    for (i <- 0 to s.length - 1) {
      val c_> = s(i)
      if(insideWord_> && !isWordChar(c_>)) {
        insideWord_> = false
      } else if(insideWord_> && isWordChar(c_>)) {
        endWord = s"$endWord${c_>}"
      } else if(!insideWord_> && isWordChar(c_>)) {
        words+=1
        insideWord_> = true
        endWord = c_>.toString
      }
      if(i == s.length-1 && insideWord_>){
        wordIsAtTheEnd = true
      }

      val j = s.length - 1 - i
      val c_< = s(j)
//      println(s"c_< = ${c_<}")
      if(insideWord_< && !isWordChar(c_<)) {
        insideWord_< = false
      } else if(insideWord_< && isWordChar(c_<)) {
        startWord = s"${c_<}$startWord"
      } else if(!insideWord_< && isWordChar(c_<)) {
        insideWord_< = true
        startWord = c_<.toString
      }
//      println(s"i = $i")
//      println(s"insideWord_< = ${insideWord_<}")
      if(j == 0 && insideWord_<){
        wordIsAtTheStart = true
      }
    }
//    println(s"wordIsAtTheStart = $wordIsAtTheStart")
//    println(s"wordIsAtTheEnd = $wordIsAtTheEnd")
    val sw = if(wordIsAtTheStart) Some(startWord) else None
    val fw = if(wordIsAtTheEnd) Some(endWord) else None
    val totalWords = words - (if(sw.isDefined) 1 else 0 ) - (if(fw.isDefined) 1 else 0 )
    (sw, totalWords,fw)
  }
/*
  val wcMonoid = new Monoid[WC] {
    override def op(a: WC, b: => WC): WC = (a,b) match {
      case (Stub(sa), Stub(sb)) =>
        val (l,w,r) = wc(sa + sb)
        Part(l.getOrElse(""), w, r.getOrElse(""))
      case (Stub(sa), Part(lb,w,rb)) =>
        val (la,wa,ra) = wc(sa)
        Part(la.getOrElse(""), wa + (if(ra.isDefined || lb.length>0) 1 else 0) + w, rb)
      case (Part(la,w,ra),Stub(sb)) =>
        val (lb,wb,rb) = wc(sb)
        Part(la, w + (if(ra.length>0 || lb.isDefined) 1 else 0) + wb, rb.getOrElse(""))
      case (Part(la,wa,ra),Part(lb,wb,rb)) =>
        Part(la,wa + (if(ra.length>0 || lb.length >0) 1 else 0) + wb, rb)
    }

    override def zero: WC = Stub("")
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


}