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

  def foldLeftByUsingFoldRight[A,B](as: List[A],z: B)( f: (B,A) => B ): B = {
    foldRight(as.reverse, z)( (a,b) => f(b,a) )
  }

  def foldRightByUsingFoldLeft[A,B](as: List[A],z: B)( f: (A,B) => B ): B = ??? // @TODO

  def append[A](xs: List[A], ys: List[A]): List[A] = foldRight(xs, ys)( (x,tl) => x :: tl )

  def flatten[A](xss: List[List[A]]): List[A] = foldLeft(xss, Nil:List[A])(append)

  def addOne(xs: List[Int]): List[Int] = foldRight(xs, Nil:List[Int])( (a,tl) => (a+1)::tl )

  def toStringDoubleList(xs: List[Double]): List[String] = foldRight(xs, Nil:List[String])( (a,tl) => a.toString::tl )

  def map[A,B](xs: List[A])(f: A => B): List[B] = foldRight(xs, Nil:List[B])((a,tl) => f(a) :: tl)

  def filter[A](xs: List[A])(f: A => Boolean): List[A] = foldRight(xs, Nil:List[A])((a,tl) => if(f(a)) a :: tl else tl)

  def removeOdd(xs: List[Int]): List[Int] = filter(xs)(_ % 2 == 0)

  def flatMap[A,B](xs: List[A])(f: A => List[B]): List[B] = foldRight(xs, Nil:List[B])( (a,b) => append(f(a),b) )

  def filterUsingFlatMap[A](xs: List[A])(f: A => Boolean): List[A] = flatMap(xs)( a => if(f(a)) List(a) else Nil )

  def addLists(xs: List[Int], ys: List[Int]): List[Int] = (xs, ys) match {
    case (x :: xtl, y :: ytl) => (x + y) :: addLists(xtl, ytl)
    case _ => Nil
  }

  def zipWith[A,B,C](xs: List[A], ys: List[B])(f: (A,B) => C): List[C] = (xs,ys) match {
    case (x :: xtl, y :: ytl) => f(x, y) :: zipWith(xtl, ytl)(f)
    case _ => Nil
  }

  def startsWith[A](xs: List[A], start: List[A]): Boolean = {
    (xs, start) match {
      case (_,Nil) => true
      case (Nil,_) => false
      case (x::xtl,s::stl) => x == s && startsWith(xtl, stl)
    }
  }

  def tails[A](xs: List[A]): List[List[A]] =
    xs match {
      case Nil => Nil
      case x :: xtl => xs :: tails(xtl)
    }

  def hasSubsequence[A](sup: List[A], sub: List[A]): Boolean = {
    val isSubseq = tails(sup).map(tl => startsWith(tl,sub) )
    foldLeft(isSubseq, false)( (acc,bool) => bool || acc )
  }

  def treeSize[A](tree: Tree[A]): Int = {
    tree match {
      case Leaf(_) => 1
      case Branch(l,r) => 1 + treeSize(l) + treeSize(r)
    }
  }

  def treeMax(tree: Tree[Int]): Int = {
    tree match {
      case Leaf(i) => i
      case Branch(l,r) => Math.max(treeMax(l), treeMax(r))
    }
  }

  def depth[A](tree: Tree[A]): Int = {
    tree match {
      case Leaf(_) => 1
      case Branch(l,r) => Math.max(depth(l), depth(r))
    }
  }

  def mapTree[A,B](tree: Tree[A])(f: A => B): Tree[B] = {
    tree match {
      case Leaf(a) => Leaf(f(a))
      case Branch(l,r) => Branch(mapTree(l)(f), mapTree(r)(f))
    }
  }

  def foldTree[A,B](tree: Tree[A])(z: A => B)(f: (B,B) => B): B ={
    tree match {
      case Leaf(a) => z(a)
      case Branch(l,r) => f(foldTree(l)(z)(f),foldTree(r)(z)(f))
    }
  }

  def treeSizeFold[A](tree: Tree[A]): Int = foldTree(tree)(_ => 1)(_ + _)

  def treeMaxFold(tree: Tree[Int]): Int = foldTree(tree)(i => i)(Math.max)

  def depthFold[A](tree: Tree[A]): Int = foldTree(tree)(_ => 1)(Math.max)

  def mapTreeFold[A,B](tree: Tree[A])(f: A => B): Tree[B] = foldTree(tree)(a => Leaf(f(a)) : Tree[B] )( Branch.apply )
}

sealed trait Tree[+A]
case class Leaf[A](value: A) extends Tree[A]
case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]