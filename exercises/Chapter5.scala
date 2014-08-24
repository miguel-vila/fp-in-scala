sealed trait Stream[+A]{
    def toList: List[A] = {
        this match {
            case Empty => Nil
            case Cons(h,t) => h ():: t().toList 
        }
    }
    def take(n: Int): Stream[A] = {
        this match {
            case Empty => Empty
            case Cons(h,t) =>
                if(n<=0) {
                    Empty
                } else {
                    Stream.cons(h() , t().take(n-1))
                }
        }
    }
    def drop(n: Int): Stream[A] = {
        this match {
            case Empty => Empty
            case Cons(h,t) =>
                if(n<=0) {
                    this
                } else {
                    t().drop(n-1)
                }
        }
    }
    def takeWhile(p: A => Boolean): Stream[A] = {
        this match {
            case Empty => Empty
            case Cons(h,t) => {
                val hd = h() 
                if(p(hd)){
                    Stream.cons(hd, t().takeWhile(p))
                } else {
                    Empty
                }
            }
        }  
    }
    def foldRight[B](z: => B)(f: (A, => B) => B): B =
        this match {
            case Cons(h,t) => f(h(), t().foldRight(z)(f))
            case _ => z
    }
    def forAll(p: A => Boolean): Boolean = foldRight(true)( (a,b) => p(a) && b )
    def headOption: Option[A] = foldRight(None : Option[A])( (a,_) => Some(a) )
    def map[B](f: A => B): Stream[B] = foldRight(Empty : Stream[B])( (a,b) => Stream.cons(f(a),b) )
    def filter(p: A => Boolean): Stream[A] = foldRight(Empty : Stream[A])( (a,b) => if(p(a)) Stream.cons(a,b) else b )
    def append[AA>:A](other: => Stream[AA]): Stream[AA] = foldRight(other)( (a,b) => Stream.cons(a,b) )
    def flatMap[B](f: A => Stream[B]): Stream[B] = foldRight(Empty : Stream[B])( (a,b) => f(a).append(b) )
    def mapUnfold[B](f: A => B): Stream[B] = Stream.unfold(this) {
        case Cons(h,t) => Some((f(h()),t()))
        case Empty => None
    }
    def takeUnfold(n: Int): Stream[A] = Stream.unfold(this,n) {
        case (_,n) if n<= 0 => None
        case (Empty,_) => None
        case (Cons(h,t),n) => Some(( h(), (t(),n-1) ))
    }
    def takeWhileUnfold(p: A => Boolean): Stream[A] = Stream.unfold(this){
        case Cons(h,t) => if(p(h())) Some((h(),t())) else None
        case Empty => None
    }
    def zipWith[B,C](other: Stream[B])(f: (A,B) => C): Stream[C] = Stream.unfold((this,other)){
        case _ => None
    }
    def zipAll[B](other: Stream[B]): Stream[(Option[A],Option[B])] = Stream.unfold((this,other)){
        case (Cons(h1,t1),Cons(h2,t2)) => Some((( Some( h1() ) , Some ( h2() ) ), ( t1() , t2() ) ) )
        case (Cons(h1,t1),Empty) => Some((( Some( h1() ) , None ), ( t1() , Empty ) ) )       
        case (Empty,Cons(h2,t2)) => Some((( None , Some( h2() ) ), ( Empty , t2() ) ) )
        case _ => None        
    }
    def startsWith[AA>:A](other: Stream[AA]): Boolean = {
        zipAll(other).foldRight(true) { 
            (a,rest) => a match {
                case (Some(x),Some(y)) => x == y && rest
                case (None,None) => true
                case (None,_) => false
                case (_,None) => true
            }
        }
    }
    def tails: Stream[Stream[A]] = {
        Stream.unfold(this) { s =>
            s match {
                case Cons(h,t) => Some((s,t()))
                case _ => None
            }
        } append Stream(Empty)
    }
    def scanRight[B](z: B)(f: (A, => B) => B): Stream[B] = {
        // tails.map(_.foldRight(z)(f)) <- INEFICIENTE!!
        foldRight(Stream.cons(z,Empty)){ (s,rest) =>
            rest match {
                case Cons(b,_) => Stream.cons(f(s,b()), rest)
                case Empty => Stream.cons(z, Empty)
            }
        }
    }
}
case object Empty extends Stream[Nothing]
case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]
object Stream {
    def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
        lazy val head = hd
        lazy val tail = tl
        Cons(() => head, () => tail)
    }
    def empty[A]: Stream[A] = Empty
    def apply[A](as: A*): Stream[A] = if (as.isEmpty) empty else cons(as.head, apply(as.tail: _*))
    def constant[A](a: A): Stream[A] = cons(a,constant(a))
    def from(n: Int): Stream[Int] = cons(n, from(n+1))
    def fibs: Stream[Int] = {
        def f(a: Int, b: Int): Stream[Int] = cons(a, f(b, a+b))
        f(0,1)
    }
    def unfold[A, S](z: S)(f: S => Option[(A,S)]): Stream[A] = {
        f(z) match {
            case Some((a,s)) => cons(a,unfold(s)(f))
            case _ => empty
        }
    }
    def fromUnfold(n: Int): Stream[Int] = unfold(n)( n => Some((n,n+1)) )
    def constantUnfold[A](a: A): Stream[A] = unfold(a)( a => Some((a,a)) )
    def fibsUnfold: Stream[Int] = unfold((0,1)){ case (a,b) => Some((a,(b,a+b))) }
}