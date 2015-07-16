package soluciones.chapter9.parsing

import soluciones.chapter8.testing._
import soluciones.chapter8.testing.Prop._

import scala.util.matching.Regex

/**
 * Created by mglvl on 29/03/15.
 */
trait Parsers[ParserError, Parser[+_]] { self =>

  def run[A](p: Parser[A])(input: String): Either[ParserError, A]

  def char(c: Char): Parser[Char]

  def or[A](s1: Parser[A], s2: Parser[A]): Parser[A]
  implicit def string(s: String): Parser[String]
  implicit def operators[A](p: Parser[A]): ParserOps[A] = ParserOps(p)
  implicit def asStringParser[A](a: A)(implicit f: A => Parser[String]): ParserOps[String] = ParserOps(f(a))

  def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]]

  def map[A,B](p: Parser[A])(f: A => B): Parser[B]

  /*
  def countOne(c: Char): Parser[Int] = char(c) map {_ => 1}

  def countZeroOrMore(c: Char): Parser[Int]

  def countOneOrMore(c: Char): Parser[Int] = countOne(c) or countZeroOrMore(c)

  def andThen[A,B](p1: Parser[A], p2: Parser[B]): Parser[(A,B)]

  countZeroOrMore('a') andThen countOneOrMore('b')
  */

  def many[A](p: Parser[A]): Parser[List[A]]

  def succeed[A](a: A): Parser[A] = string("").map(_ => a)

  def product[A,B](p1: Parser[A], p2: => Parser[B]): Parser[(A,B)]

  def slice[A](p: Parser[A]): Parser[A]

  //9.1
  def map2[A,B,C](p: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C] =
    (p product p2) map { case (a,b) => f(a,b) }

  def many1[A](p: Parser[A]): Parser[List[A]] =
    map2(p, many(p))(_ :: _)

  //9.3
  def manyImpl[A](p: Parser[A]): Parser[List[A]] =
    map2(p,manyImpl(p))(_ :: _) or succeed(List())

  //9.4
  def listOfNImpl[A](n: Int, p: Parser[A]): Parser[List[A]] =
    if(n == 0)
      succeed(List())
    else
      map2(p,listOfNImpl(n-1,p))(_ :: _)

  //9.5 @TODO

  //9.6
  def flatMap[A,B](p: Parser[A])(f: A => Parser[B]): Parser[B]

  implicit def regex(r: Regex): Parser[String]

  "[0-9]".r.map(_.toInt).flatMap( d => listOfN(d, char('a')) )

  //9.7
  def productInTermsOfFlatMap[A,B](p1: Parser[A], p2: => Parser[B]): Parser[(A,B)] =
    for {
      a <- p1
      b <- p2
    } yield (a,b)

  def map2InTermsOfFlatmap[A,B,C](p1: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C] =
    for {
      a <- p1
      b <- p2
    } yield f(a,b)

  //9.8
  def mapInTermsOfFlatmap[A,B](p: Parser[A])(f: A => B): Parser[B] =
    p.flatMap( a => succeed(f(a)) )

  def betweenSpaces[A](p: Parser[A]): Parser[A] =
    (spaces ** p ** spaces).map { case ((_,a),_) => a }

  def first[A,B](p: Parser[(A,B)]): Parser[A] = p.map(_._1)
  def second[A,B](p: Parser[(A,B)]): Parser[B] = p.map(_._2)

  def listOfElements[A](elem: => Parser[A], separator: Parser[_]): Parser[IndexedSeq[A]] = {
    lazy val atLeastOneElemListParser = (elem *-* second(separator.slice *-* elem).many).map { case (e, le) => (e :: le).toIndexedSeq }
    atLeastOneElemListParser or succeed(IndexedSeq())
  }

  def listOfElementsWithStartAndEnd[A](elem: => Parser[A], separator: Parser[_], start: Parser[_], end: Parser[_]): Parser[IndexedSeq[A]] = {
    (start *-* listOfElements(elem,separator) *-* end).map { case ((_,list),_) => list }
  }

  val spaces = regex("\\s".r).many.slice

  def spaceProduct[A,B](p1: Parser[A], p2: Parser[B]): Parser[(A,B)] = {
    (p1 ** spaces ** p2).map { case ((a,_),b) => (a,b) }
  }

  case class ParserOps[A](p: Parser[A]) {
    def |[B>:A](p2: Parser[B]): Parser[B] = self.or(p,p2)
    def or[B>:A](p2: => Parser[B]) = self.or(p,p2)
    def map[B](f: A => B): Parser[B] = self.map(p)(f)
    def **[B](p2: Parser[B]) = self.product(p,p2)
    def slice = self.slice(p)
    def product[B](p2: Parser[B]) = self.product(p,p2)
    //def andThen[B](p2: Parser[B]) = self.andThen(p,p2)
    def flatMap[B](f: A => Parser[B]): Parser[B] = self.flatMap(p)(f)
    def many = self.many(p)
    def *-*[B](p2: Parser[B]) = self.spaceProduct(p,p2)
    def betweenSpaces = self.betweenSpaces(p)
  }

  object Laws {
    def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop =
      forAll(in)(s => run(p1)(s) == run(p2)(s))
    def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop =
      equal(p, p.map(a => a))(in)
    // 9.2 @TODO
    def productMapLaw[A,B,C](p1: Parser[A], p2: Parser[B], f: A => C)(in: Gen[String]): Prop ={
      equal(
        (p1 ** p2) map { case (a,b) => (f(a),b) },
        ((p1 map f) ** p2)
      )(in)
    }
    def productAssociativityLaw[A,B,C](p1: Parser[A], p2: Parser[B], p3: Parser[C])(in: Gen[String]): Prop =
      equal((p1 ** p2) ** p3, p1 ** (p2 ** p3))(in)
    /*
    def productIdentityLaw[A](p: Parser[A], a: A)(in: Gen[String]): Prop =
      equal(succeed(a) ** p, p ** succeed(a))(in) && equal(p ** succeed(a), p)(in)
    */



  }

}

// 9.9 @TODO
trait JSON
object JSON {
  case object JNull extends JSON
  case class JNumber(get: Double) extends JSON
  case class JString(get: String) extends JSON
  case class JBool(get: Boolean) extends JSON
  case class JArray(get: IndexedSeq[JSON]) extends JSON
  case class JObject(get: Map[String, JSON]) extends JSON

  def nullJsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[JNull.type] = {
    import P._
    string("null").slice.map(_ => JNull).betweenSpaces
  }

  def numberJsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[JNumber] = {
    import P._
    regex("\\-?\\d+(\\.\\d+(e\\d+)?)?".r).map(s => JNumber(s.toDouble)).betweenSpaces
  }

  def stringJsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[JString] = {
    import P._
    regex("\".*\"".r).map(s => JString(s.substring(1,s.length-1))).betweenSpaces
  }

  def booleanJsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[JBool] = {
    import P._
    ((string("true").slice.map(_ => JBool(true))) or (string("false").slice.map(_ => JBool(false)))).betweenSpaces
  }

  def arrayJsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[JArray] = {
    import P._
    listOfElementsWithStartAndEnd(jsonParser(P), char(','), char('['), char(']')).map( jsons => JArray(jsons) )
  }

  def nameObjectPairJsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[(String, JSON)] = {
    import P._
    val name = regex("\".*\"".r).map(s => s.substring(1,s.length-1))
    (name ** char(':') ** jsonParser(P)).map { case ((name,_),json) => (name,json) }
  }

  def objectJsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[JObject] = {
    import P._
    listOfElementsWithStartAndEnd(nameObjectPairJsonParser(P), char(','), char('{'), char('}')).map {
      nameJsonPairs => JObject(nameJsonPairs.foldLeft(Map[String,JSON]())( (map,pair) => map + pair ) )
    }
  }

  def jsonParser[Err, Parser[+_]](P: Parsers[Err,Parser]): Parser[JSON] = {
    import P._

    nullJsonParser(P) or
    numberJsonParser(P) or
    stringJsonParser(P) or
    booleanJsonParser(P) or
    arrayJsonParser(P) or
    objectJsonParser(P)
  }

}