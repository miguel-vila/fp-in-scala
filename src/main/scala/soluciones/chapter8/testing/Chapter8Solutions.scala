package soluciones.chapter8.testing

import java.util.concurrent.{Executors, ExecutorService}

import soluciones.chapter6.state.{ RNG , Simple, State }
import Prop._
import soluciones.chapter5.laziness.Stream
import soluciones.chapter7.parallelism.Par
import soluciones.chapter7.parallelism.Par.Par
import soluciones.chapter7.parallelism.Par.Par

object ** {
  def unapply[A,B](p: (A,B)) = Some(p)
}

case class Gen[+A](sample: State[RNG, A]) {
  def flatMap[B](f: A => Gen[B]): Gen[B] = Gen(sample.flatMap(a => f(a).sample))
  def map[B](f: A => B): Gen[B] = Gen(sample.map(f))
  def listOfN(size: Gen[Int]): Gen[List[A]] = size.flatMap(listOfN)
  def listOfN(n: Int): Gen[List[A]] = Gen.listOfN(n,this)
  def unsized: SGen[A] = SGen(_ => this)
  def **[B](other: Gen[B]): Gen[(A,B)] = {
    for {
      a <- this
      b <- other
    } yield (a,b)
  }
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

  def sequence[A](list: List[Gen[A]]): Gen[List[A]] = {
    list match {
      case Nil => unit(Nil)
      case g :: gtl => for {
        a <- g
        atl <- sequence(gtl)
      } yield a :: atl
    }
  }
}

case class SGen[+A](forSize: Int => Gen[A]) {
  def flatMap[B](f: A => SGen[B]): SGen[B] = SGen { n =>
    forSize(n).flatMap{
      a => f(a).forSize(n)
    }
  }
  def map[B](f: A => B): SGen[B] = SGen(n => forSize(n).map(f) )
  def **[B](other: SGen[B]): SGen[(A,B)] = SGen { n => forSize(n) ** other.forSize(n) }
}

object SGen {
  def unit[A](a: => A): SGen[A] = SGen(_ => Gen.unit(a))
  def listOf[A](g: Gen[A]): SGen[List[A]] = SGen(g.listOfN)
  def listOfAtLeast1[A](g: Gen[A]): SGen[List[A]] = SGen(n => g.listOfN(n max 1))
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
    (_,n, rng) => randomStream(as)(rng).zip(Stream.from(0)).take(n).map {
      case (a, i) => try {
        if (f(a)) Passed else Falsified(a.toString, i)
      } catch {
        case e: Exception => Falsified(buildMsg(a, e), i)
      }
    }.find(_.isFalsified).getOrElse(Passed)
  }

  def forAll[A](g: SGen[A])(f: A => Boolean): Prop =
    forAll(g.forSize)(f)

  def forAll[A](g: Int => Gen[A])(f: A => Boolean): Prop = Prop {
    (max, n, rng) =>
      val casesPerSize = (n + (max - 1)) / max
      val props: Stream[Prop] = Stream.from(0).take((n min max) + 1).map(i => forAll(g(i))(f))
      val prop: Prop = props.map(p => Prop { (max, _, rng) =>
        p.run(max, casesPerSize, rng)
      }).toList.reduce(_ && _)
      val x = props.map(p => Prop { (max, _, rng) =>
        p.run(max, casesPerSize, rng)
      }).toList
      prop.run(max, n, rng)
  }

  // String interpolation syntax. A string starting with `s"` can refer to
  // a Scala value `v` as `$v` or `${v}` in the string.
  // This will be expanded to `v.toString` by the Scala compiler.
  def buildMsg[A](s: A, e: Exception): String =
    s"test case: $s\n" +
      s"generated an exception: ${e.getMessage}\n" +
      s"stack trace:\n ${e.getStackTrace.mkString("\n")}"

  def run(p: Prop,
           maxSize: Int = 100,
           testCases: Int = 100,
           rng: RNG = Simple(System.currentTimeMillis())): Unit = {
    p.run(maxSize,testCases,rng) match {
      case Falsified(msg, n) =>
        println(s"! Falsified after $n passed tests:\n $msg")
      case Passed =>
        println(s"+ OK, passed $testCases tests.")
      case Proved =>
        println("+ OK, proved property.")
    }
  }

  def check(p: => Boolean): Prop = Prop { (_,_,_) =>
    if(p) Proved else Falsified("()",0)
  }

  val S = Gen.weighted(
    Gen.choose(1,4).map(Executors.newFixedThreadPool) -> .75,
    Gen.unit(Executors.newCachedThreadPool()) -> .25)

  def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop = {
    forAll(S ** g) { case s ** a => f(a)(s).get() }
  }

  def forAllPar[A](g: SGen[A])(f: A => Par[Boolean]): Prop = {
    forAll(S.unsized ** g) { case s ** a => f(a)(s).get() }
  }

  def equalPar[A](p: Par[A], p2: Par[A]): Par[Boolean] = Par.map2(p, p2)(_ == _)

  def checkPar(p: => Par[Boolean]): Prop = {
    forAll(S){ s => p(s).get() }
  }

}

object Test extends App {
  import Prop._
  import SGen._

  val smallInt = Gen.choose(-10,10)
  val maxProp = forAll(listOfAtLeast1(smallInt)) { ns =>
    val max = ns.max
    !ns.exists(_ > max)
  }

  def sorted(ns: List[Int]): Boolean = {
    if(ns.length<=1) {
      true
    } else {
      val init = ns.toStream
      val tail = init.tail
      (init zip tail).foldLeft(true){ case (acc,(a,b)) => a <= b && acc }
    }
  }

  def sameElementsWithSameTimes(xs: List[Int], ys: List[Int]): Boolean = {
    xs.groupBy(x => x).mapValues(_.length) == ys.groupBy(x => x).mapValues(_.length)
  }

  val sortedProp = forAll(listOf(Gen.choose(-100000,100000))) { ns =>
    val sortedNs = ns.sorted
    sorted(sortedNs) && sameElementsWithSameTimes(ns, sortedNs)
  }

  println("MAX PROP:")
  run(maxProp)
  println("SORTED PROP:")
  run(sortedProp)

  val smallParInt = smallInt.map(Par.unit)
  val smallParInt2Sum = (smallParInt ** smallParInt).map { case a ** b => Par.fork( Par.map2(a,b){_+_} ) }
  val smallParIntListSum = listOf(smallInt).map(l => Par.map(Par.sequence(l.map(Par.unit))){_.sum} )
  val forkProp = forAllPar(smallParInt) { x =>
    Prop.equalPar(Par.fork(x), x)
  }

}