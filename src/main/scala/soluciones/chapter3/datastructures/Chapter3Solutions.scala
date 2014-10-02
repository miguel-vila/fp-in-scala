package soluciones.chapter3.datastructures

/**
 * Created by mglvl on 9/27/14.
 */
object Chapter3Solutions {

  def foldRight[A,B](as: List[A],z: B)( f: (A,B) => B ): B = {
    as match {
      case Nil => z
      case x :: xs => f(x,foldRight(xs,z)(f))
    }
  }

  def length[A](as: List[A]): Int = as.foldRight(0)( (a,acc) => acc + 1 )

  def foldLeft[A,B](as: List[A],z: B)( f: (B,A) => B ): B = {
    as match {
      case Nil => z
      case x :: xs => foldLeft(as,f(z,x))(f)
    }
  }

  def sum(xs: List[Double]): Double = foldLeft(xs, 0.0)(_ + _)

  def product(xs: List[Int]): Double = foldLeft(xs, 1.0)(_ * _)

  def reverse[A](xs: List[A]): List[A] = {
    foldLeft(xs,Nil:List[A])( (l,a) => a :: l )
  }

  /*
  def foldLeftByFoldRight[A,B](as: List[A],z: B)( f: (B,A) => B ): B = {
    foldRight()
  }
  */
}
