package soluciones.chapter8.testing

import soluciones.chapter6.state.{ RNG , Simple, State }
import Prop._
import soluciones.chapter5.laziness.Stream

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

  def union[A](g1: Gen[A], g2: Gen[A]): Gen[A] = boolean.flatMap( b => if(b) g1 else g2 )

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

case class Prop(run: (MaxSize,TestCases,RNG) => Result) {

  def &&(other: Prop): Prop = Prop { (max,testCases,rng) =>
    val r1 = run(max,testCases,rng)
    if(r1.isFalsified)
      r1
    else
      other.run(max,testCases,rng)
  }

  def ||(other: Prop): Prop = Prop { (max,testCases,rng) =>
    val r1 = run(max,testCases,rng)
    r1 match {
      case Falsified(failure, successes) => other.run(max,testCases,rng).prependMsg(failure)
      case r => r
    }
  }

}

object Prop {
  type SuccessCount = Int
  type TestCases = Int
  type MaxSize = Int
  type FailedCase = String

  sealed trait Result {
    def isFalsified: Boolean
    def prependMsg(str: String): Result
  }

  case object Passed extends Result {
    def isFalsified = false
    def prependMsg(str: String) = this
  }

  case class Falsified(failure: FailedCase,
                       successes: SuccessCount) extends Result {
    def isFalsified = true
    def prependMsg(str: String) = Falsified(s"$str\n$failure", successes)
  }

  case object Proved extends Result {
    def isFalsified = false
    def prependMsg(str: String) = this
  }

  /* Produce an infinite random stream from a `Gen` and a starting `RNG`. */
  def randomStream[A](g: Gen[A])(rng: RNG): Stream[A] =
    Stream.unfold(rng)(rng => Some(g.sample.run(rng)))

  def forAll[A](as: Gen[A])(f: A => Boolean): Prop = Prop {
    (n, rng) => randomStream(as)(rng).zip(Stream.from(0)).take(n).map {
      case (a, i) => try {
        if (f(a)) Passed else Falsified(a.toString, i)
      } catch {
        case e: Exception => Falsified(buildMsg(a, e), i)
      }
    }.find(_.isFalsified).getOrElse(Passed)
  }

  // String interpolation syntax. A string starting with `s"` can refer to
  // a Scala value `v` as `$v` or `${v}` in the string.
  // This will be expanded to `v.toString` by the Scala compiler.
  def buildMsg[A](s: A, e: Exception): String =
    s"test case: $s\n" +
      s"generated an exception: ${e.getMessage}\n" +
      s"stack trace:\n ${e.getStackTrace.mkString("\n")}"

  def apply(f: (TestCases,RNG) => Result): Prop =
    Prop { (_,n,rng) => f(n,rng) }
}