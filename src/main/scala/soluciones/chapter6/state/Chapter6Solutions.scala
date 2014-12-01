package soluciones.chapter6.state

trait RNG {
  def nextInt: (Int, RNG)
}

object RNG {
  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (n1,rng2) = rng.nextInt
    val n2 = if(n1<0) -(n1 + 1) else n1
    (n2,rng2)
  }
  def double(rng: RNG): (Double,RNG) = {
    val (n1,rng2) = nonNegativeInt(rng)
    (n1.toDouble / (Int.MaxValue + 1) , rng2)
  }
  def intDouble(rng: RNG): ((Int,Double), RNG) = {
    val (n,rng1) = rng.nextInt
    val (d,rng2) = double(rng1)
    ((n,d),rng2)
  }
  def doubleInt(rng: RNG): ((Double,Int), RNG) = {
    val ((n,d),rng1) = intDouble(rng)
    ((d,n),rng1)
  }
  def double3(rng: RNG): ((Double,Double,Double), RNG) = {
    val(d1,rng1) = double(rng)
    val(d2,rng2) = double(rng1)
    val(d3,rng3) = double(rng2)
    ((d1,d2,d3),rng3)
  }
  def ints(count: Int)(rng: RNG): (List[Int], RNG) = {
    if(count == 0) {
      (Nil, rng)
    } else {
      val (x,rng1) = rng.nextInt
      val (xs,rng2) = ints(count-1)(rng1)
      (x :: xs , rng2)
    }
  }
  def intsTailRec(count: Int)(rng: RNG): (List[Int], RNG) = {
    def go(n: Int, acc: List[Int])(rng: RNG): (List[Int], RNG) = {
      if(n==0){
        (acc, rng)
      } else {
        val (x, rng1) = rng.nextInt
        go(n-1, x :: acc)(rng1)
      }
    }
    go(count,Nil)(rng)
  }
  type Rand[+A] = RNG => (A, RNG)
  val int: Rand[Int] = _.nextInt
  def unit[A](a: A): Rand[A] = rng => (a, rng)
  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }
  def _double: Rand[Double] = {
    map(nonNegativeInt)(n1 => n1.toDouble / (Int.MaxValue + 1))
  }
  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] = {
    rng => {
      val (a,rng1) = ra(rng)
      val (b,rng2) = rb(rng1)
      (f(a,b),rng2)
    }
  }
  def both[A,B](ra: Rand[A], rb: Rand[B]): Rand[(A,B)] = map2(ra, rb)((_, _))
  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] = {
    fs match {
      case Nil => unit(Nil)
      case rnd :: rnds =>
        rng => {
          val (a,rng1) = rnd(rng)
          val sequenceRnds = sequence(rnds)
          val (as,rng2) = sequenceRnds(rng1)
          (a::as, rng2)
        }
    }
  }
  def sequenceTailrec[A](fs: List[Rand[A]]): Rand[List[A]] = {
    def go(fs: List[Rand[A]], acc: List[A]): Rand[List[A]] = rng => {
      fs match {
        case Nil => (acc,rng)
        case rnd :: rnds =>
          val rndLst = map(rnd)( _ :: acc )
          val (lst,rng1) = rndLst(rng)
          go(rnds, lst)(rng1)
      }
    }
    go(fs, Nil)
  }
  def sequenceFold[A](fs: List[Rand[A]]): Rand[List[A]] = {
    fs.foldRight(unit(List[A]()))( (r,acc) => map2(r,acc)( _ :: _ ) )
  }
  def repeat[A](rand: Rand[A])(count: Int): Rand[List[A]] = {
    sequenceTailrec( List.fill(count)(rand) )
  }
  def _ints: Int => Rand[List[Int]] = repeat(int)
  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] = {
    rng =>
      val (a,rng1) = f(rng)
      g(a)(rng1)
  }
  def nonNegativeLessThan(n: Int): Rand[Int] = {
    flatMap(nonNegativeInt) { i =>
      val mod = i % n
      if(i + (n-1) - mod >= 0) {
        unit(mod)
      } else {
        nonNegativeLessThan(n)
      }
    }
  }
  def mapWithFlatMap[A,B](s: Rand[A])(f: A => B): Rand[B] = {
    flatMap(s)( a => unit(f(a)) )
  }
  def map2WithFlatMap[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] = {
    flatMap(ra) { a =>
      map(rb) { b =>
        f(a,b)
      }
    }
  }

}

case class Simple(seed: Long) extends RNG {
  def nextInt: (Int, RNG) = {
    val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
    val nextRNG = Simple(newSeed)
    val n = (newSeed >>> 16).toInt
    (n, nextRNG)
  }
}

case class State[S, +A](run: S => (A,S)) {
  def map[B](f: A => B): State[S,B] = {
    State { s : S =>
      val (a,s2) = run(s)
      (f(a),s2)
    }
  }
  def flatMap[B](f: A => State[S,B]): State[S,B] = {
    State { s : S =>
      val (a,s2) = run(s)
      f(a).run(s2)
    }
  }
  def map2[B,C](other: State[S,B])(f: (A, B) => C): State[S,C] = {
    flatMap { a =>
      other map { b =>
        f(a,b)
      }
    }
  }
}

object State {
  def get[S]: State[S, S] = State(s => (s, s))
  def set[S](s: S): State[S, Unit] = State(_ => ((), s))
  def modify[S](f: S => S): State[S, Unit] = for {
    s <- get
    _ <- set(f(s))
  } yield ()
  def unit[S,A](a: A) = new State[S,A]( s => (a,s) )
  def sequence[S,A](fs: List[State[S,A]]): State[S,List[A]] = {
    fs.foldRight(unit(List()): State[S,List[A]])( (st,acc) => st.map2(acc)( _ :: _ ) )
  }

}


object Chapter6Solutions {
  import State._

  sealed trait Input
  case object Coin extends Input
  case object Turn extends Input
  case class Machine(locked: Boolean, candies: Int, coins: Int) {
      def insertCoin: Machine = {
          if(locked && candies >0)
              this.copy(locked = false, coins=coins+1)
          else
              this.copy(coins=coins+1)
      }
      def turn: Machine = {
          if(!locked && candies >0)
              this.copy(locked=true, candies=candies-1)
          else
              this
      }
      def receive(input: Input): Machine ={
          input match {
              case Coin => insertCoin
              case Turn => turn
          }
      }
  }
  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] = {
      val inputSequence = inputs.map(input => modify{ m: Machine => m.receive(input) } )
      for {
          _ <- State.sequence(inputSequence)
          m <- get
      } yield (m.coins, m.candies)
  }
  simulateMachine(List(Coin,Turn,Coin,Turn,Coin,Turn,Coin,Turn)).run(Machine(true,5,10))
}