package soluciones.chapter10.monoids

import soluciones.chapter8.testing.{Gen, Prop}

object Test extends App {

  val addMonoid = new Monoid[Int] {
    def op(a1: Int, a2: => Int) = a1 + a2
    def zero = 0
  }

  val gen =  Gen.choose(-10000,10000)
  Prop.run(Monoid.monoidLaws(addMonoid, gen))

}
