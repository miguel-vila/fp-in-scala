package soluciones.chapter7.parallelism

import java.util.concurrent._

import scalaz.Ordering.{EQ, GT}
import scalaz.{Order, Monoid}

/**
 * Created by mglvl on 9/11/14.
 */
object Chapter7Solutions {

}

object Par {
  type Par[A] = ExecutorService => Future[A]

  def run[A](s: ExecutorService)(a: Par[A]): Future[A] = a(s)

  def unit[A](a: A): Par[A] = (es: ExecutorService) => UnitFuture(a) // `unit` is represented as a function that returns a `UnitFuture`, which is a simple implementation of `Future` that just wraps a constant value. It doesn't use the `ExecutorService` at all. It's always done and can't be cancelled. Its `get` method simply returns the value that we gave it.

  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))

  def asyncF[A,B](f: A => B): A => Par[B] = { a => lazyUnit(f(a)) }

  def sequence[A](l: List[Par[A]]): Par[List[A]] = { es =>
    l match {
      case Nil => UnitFuture(Nil)
      case p :: ps =>
        val listPar = map2(p, sequence(ps)) { _ :: _ }
        listPar(es)
    }
  }

  def sequence2A[A](l: List[Par[A]]): Par[List[A]] = { es =>
    l.foldRight(UnitFuture(Nil): Future[List[A]])( (p,pl) => map2(p,{es: ExecutorService => pl})( _ :: _)(es) )
  }

  def sequence2B[A](l: List[Par[A]]): Par[List[A]] = {
    l.foldRight(unit(Nil): Par[List[A]])( (p,pl) => map2(p,pl)( _ :: _ ) )
  }

  def sequence3[A](l: List[Par[A]]): Par[List[A]] = map(sequenceIndexed(l.toIndexedSeq))(_.toList)

  def sequenceIndexed[A](is: IndexedSeq[Par[A]]): Par[IndexedSeq[A]] = fork {
    if(is.isEmpty) {
      unit(Vector())
    } else if(is.length == 1) {
      map( is.head ) { a => Vector(a) }
    } else {
      val (is1, is2) = is.splitAt( is.length / 2 )
      val p1 = sequenceIndexed(is1)
      val p2 = sequenceIndexed(is2)
      map2(p1,p2)( _ ++ _ )
    }
  }

  private case class UnitFuture[A](get: A) extends Future[A] {
    def isDone = true
    def get(timeout: Long, units: TimeUnit) = get
    def isCancelled = false
    def cancel(evenIfRunning: Boolean): Boolean = false
  }

  private case class Map2Future[A,B,C](a: Future[A], b: Future[B], f: (A,B) => C) extends Future[C] {
    var c: Option[C] = None
    def isDone = c.isDefined
    def get(timeout: Long, units: TimeUnit) = {
      execute(TimeUnit.MILLISECONDS.convert(timeout, units))
    }
    def get = get(Long.MaxValue, TimeUnit.MILLISECONDS)
    def isCancelled = a.isCancelled || b.isCancelled
    def cancel(evenIfRunning: Boolean) = a.cancel(evenIfRunning) || b.cancel(evenIfRunning)
    private def execute(timeout: Long) = {
      c match {
        case None =>
          val start = System.currentTimeMillis()
          val aVal = a.get(timeout, TimeUnit.MILLISECONDS)
          val afterA = System.currentTimeMillis()
          val aDuration = afterA - start
          val bVal = b.get(timeout - aDuration, TimeUnit.MILLISECONDS)
          val cVal = f(aVal, bVal)
          c = Some(cVal)
          cVal
        case Some(cVal) => cVal
      }
    }
  }

  def map2[A,B,C](a: Par[A], b: Par[B])(f: (A,B) => C): Par[C] = // `map2` doesn't evaluate the call to `f` in a separate logical thread, in accord with our design choice of having `fork` be the sole function in the API for controlling parallelism. We can always do `fork(map2(a,b)(f))` if we want the evaluation of `f` to occur in a separate thread.
    (es: ExecutorService) => {
      val af = a(es)
      val bf = b(es)
      Map2Future(af, bf, f)
    }

  def map3[A,B,C,D](a: Par[A], b: Par[B], c: Par[C])(f: (A,B,C) => D): Par[D] = {
    map2(map2(a,b){ (a,b) => (a,b) },c){ case ((a,b),c) => f(a,b,c) }
  }

  def map4[A,B,C,D,E](a: Par[A], b: Par[B], c: Par[C], d: Par[D])(f: (A,B,C,D) => E): Par[E] = {
    map2(map3(a,b,c){ (a,b,c) => (a,b,c) },d){ case((a,b,c),d) => f(a,b,c,d) }
  }

  def map5[A,B,C,D,E,F](a: Par[A], b: Par[B], c: Par[C], d: Par[D], e: Par[E])(f: (A,B,C,D,E) => F): Par[F] = {
    map2(map4(a,b,c,d){ (a,b,c,d) => (a,b,c,d) },e){ case((a,b,c,d),e) => f(a,b,c,d,e) }
  }

  def parFold[A,B](l: IndexedSeq[A])(z: B)( f: A => B, g: (B,B) => B ): Par[B] = fork {
    if(l.isEmpty){
      unit(z)
    } else if(l.length == 1) {
      unit(f(l.head))
    } else {
      val (l1,l2) = l.splitAt( l.length/2 )
      val b1 = parFold(l1)(z)(f,g)
      val b2 = parFold(l2)(z)(f,g)
      map2(b1,b2)(g)
    }
  }

  def parWordCount(paragraphs: List[String]): Par[Int] = {
    parFold(paragraphs.toIndexedSeq)(0)( _.length, _ + _ )
  }

  /**
    * Same as before but using scalaz.Monoid
   */
  def parFold2[A,B: Monoid](l: IndexedSeq[A])( f: A => B): Par[B] = fork {
    val M = implicitly[Monoid[B]]
    if(l.isEmpty){
      unit(M.zero)
    } else if(l.length == 1) {
      unit(f(l.head))
    } else {
      val (l1,l2) = l.splitAt( l.length/2 )
      val b1 = parFold2(l1)(f)
      val b2 = parFold2(l2)(f)
      map2(b1,b2)(M.append(_,_))
    }
  }

  def parMax[A: Order](minValue: A)(l: List[A]): Par[A] = {
    implicit val M = new Monoid[A] {
      def zero = minValue
      def append(a1: A, a2: => A) = {
        val cmp = implicitly[Order[A]].order(a1,a2)
        cmp match {
          case GT => a1
          case _ => a2
        }
      }
    }
    parFold2(l.toIndexedSeq)( x => x )
  }

  def fork[A](a: => Par[A]): Par[A] = // This is the simplest and most natural implementation of `fork`, but there are some problems with it--for one, the outer `Callable` will block waiting for the "inner" task to complete. Since this blocking occupies a thread in our thread pool, or whatever resource backs the `ExecutorService`, this implies that we're losing out on some potential parallelism. Essentially, we're using two threads when one should suffice. This is a symptom of a more serious problem with the implementation, and we will discuss this later in the chapter.
    es => es.submit(new Callable[A] {
      def call = a(es).get
    })

  def map[A,B](pa: Par[A])(f: A => B): Par[B] =
    map2(pa, unit(()))((a,_) => f(a))

  def sortPar(parList: Par[List[Int]]) = map(parList)(_.sorted)

  def equal[A](e: ExecutorService)(p: Par[A], p2: Par[A]): Boolean =
    p(e).get == p2(e).get

  def delay[A](fa: => Par[A]): Par[A] =
    es => fa(es)

  def choice[A](cond: Par[Boolean])(t: Par[A], f: Par[A]): Par[A] =
    es =>
      if (run(es)(cond).get) t(es) // Notice we are blocking on the result of `cond`.
      else f(es)

  def parMap[A,B](ps: List[A])(f: A => B): Par[List[B]] = fork {
    val fbs: List[Par[B]] = ps.map(asyncF(f))
    sequence(fbs)
  }

  def parFilter1[A](as: List[A])(f: A => Boolean): Par[List[A]] = fork {
    val filtered = parMap(as)( a => if(f(a)) Some(a) else None)
    map(filtered)(_.flatten)
  }

  def parFilter2[A](as: List[A])(f: A => Boolean): Par[List[A]] = map(parFilterIndexedSeq(as.toIndexedSeq)(f))(_.toList)

  def parFilterIndexedSeq[A](is: IndexedSeq[A])(f: A => Boolean): Par[IndexedSeq[A]] = fork {
    if(is.isEmpty) {
      unit(Vector())
    } else if(is.length == 1) {
      unit {
        val a = is.head
        if(f(a))
          Vector(a)
        else
          Vector()
      }
    } else {
      val (is1, is2) = is.splitAt( is.length / 2 )
      val p1 = parFilterIndexedSeq(is1)(f)
      val p2 = parFilterIndexedSeq(is2)(f)
      map2(p1,p2)( _ ++ _ )
    }
  }

  /* Gives us infix syntax for `Par`. */
  implicit def toParOps[A](p: Par[A]): ParOps[A] = new ParOps(p)

  class ParOps[A](p: Par[A]) {


  }
}
