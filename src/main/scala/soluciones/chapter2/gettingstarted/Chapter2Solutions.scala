package soluciones.chapter2.gettingstarted

/**
 * Created by mglvl on 9/27/14.
 */
object Chapter2Solutions {

  def fib(n: Int): Int = {
    @annotation.tailrec
    def f(n: Int, nm2: Int, nm1: Int): Int = {
      if(n==0)
        nm2
      else if(n==1)
        nm1
      else
        f(n-1, nm1, nm1 + nm2)
    }
    f(n,0,1)
  }

  @annotation.tailrec
  def isSorted[A](as: Array[A], ordered: (A,A) => Boolean): Boolean = {
    if(as.isEmpty)
      true
    else if(as.length == 1)
      true
    else {
      val h1 = as.head
      val tail = as.tail
      val h2 = tail.head
      ordered(h1,h2) && isSorted(tail,ordered)
    }
  }

  def curry[A,B,C](f: (A,B) => C): A => (B => C) = a => f(a,_)

  def uncurry[A,B,C](f: A => (B => C)): (A,B) => C = f(_)(_)

  def compose[A,B,C](f: B => C, g: A => B): A => C = a => f(g(a))

}
