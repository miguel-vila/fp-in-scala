package soluciones.chapter8.testing

import soluciones.chapter6.state.{ RNG , Simple, State }

case class Gen[A](sample: State[RNG, A]) {
  def flatMap[B](f: A => Gen[B]): Gen[B] = Gen(sample.flatMap(a => f(a).sample))
  def map[B](f: A => B): Gen[B] = Gen(sample.map(f))
  def listOfN(size: Gen[Int]): Gen[List[A]] = size.flatMap(Gen.listOfN(_,this))
}

object Gen {
  def bet0and1: Gen[Double] = Gen( State(RNG.double) )
  def choose(start: Int, stopExclusive: Int): Gen[Int] = {
    assert(start<stopExclusive)
    val st = State(RNG.double).map { d => ( start + (stopExclusive-start)*d ).toInt }
    Gen(st)
  }
  def unit[A](a: => A): Gen[A] = Gen(State.unit[RNG,A](a))
  def boolean: Gen[Boolean] = Gen( State(RNG.double) map { _ < .5 } )
  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]] = {
    val st = State.sequence((1 to n).map(_ => g.sample).toList)
    Gen(st)
  }
  def generatePair[A,B](genA: Gen[A], genB: Gen[B]): Gen[(A,B)] = {
    Gen(genA.sample.map2(genB.sample){case t: (A,B) => t})
  }
  def option[A](gen: Gen[A]): Gen[Option[A]] = {
    Gen(boolean.sample.flatMap( b => if(b) gen.sample.map(Some.apply) else State.unit(None) ))
  }
  def char: Gen[Char] = Gen(choose(0,256).sample.map(_.toChar))

  def stringOfLength(n: Int): Gen[String] = Gen( listOfN(n, char).sample.map(_.foldLeft("")(_ + _)) )

  def union[A](g1: Gen[A], g2: Gen[A]): Gen[A] = {
    for {
      b <- boolean
      g <- if(b) g1 else g2
    } yield g
  }

  def weighted[A](g1: (Gen[A],Double), g2: (Gen[A],Double)): Gen[A] = {
    val (gen1,w1) = g1
    val (gen2,w2) = g2
    bet0and1.flatMap { d =>
      if(d*(w1+w2)<w1)
        gen1
      else
        gen2
    }
  }

}