package soluciones.chapter7.parallelism

import java.util.concurrent._
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

  def sequence3[A](l: List[Par[A]]): Par[List[A]] = { es =>
    if(l.isEmpty) {
      UnitFuture(Nil)
    } else {
      val half = l.length / 2
      val l1 = l.slice(0, half)
      val l2 = l.slice(half, l.length)
      val listPar = map2(sequence3(l1), sequence3(l2)){ _ ++ _ }
      listPar(es)
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

  /* Gives us infix syntax for `Par`. */
  implicit def toParOps[A](p: Par[A]): ParOps[A] = new ParOps(p)

  class ParOps[A](p: Par[A]) {


  }
}
