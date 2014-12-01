package soluciones.chapter8.testing

import soluciones.chapter6.state.{ RNG , Simple, State }

case class Gen[A](sample: State[RNG, A])

object Gen {
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
}