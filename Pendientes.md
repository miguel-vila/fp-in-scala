# Pendientes

## Part I: Introduction to functional programming

### Chapter 3
- [ ] Agregar apuntes
- [ ] Ejercicio de implementar `foldRight` en términos de `foldLeft`

Los siguientes enlaces:

- [x] [The Algebra of Algebraic Data Types](http://chris-taylor.github.io/blog/2013/02/10/the-algebra-of-algebraic-data-types/) by Chris Taylor.
- [ ] [Species and Functors and Types, Oh My!](http://www.cis.upenn.edu/~byorgey/papers/species-pearl.pdf) by Brent Yorgey
- [ ] [Clowns to the left of me, jokers to the right](http://personal.cis.strath.ac.uk/~conor/Dissect.pdf) by Conor McBride

#### Zippers ####

Since an algebraic data type is a type-level function involving sums and products, we can take the derivative of such a function, yielding a data structure called a [zipper](http://en.wikipedia.org/wiki/Zipper_%28data_structure%29). The zipper for a given structure is like the original structure, but with a movable "focus" or pointer into the structure. This can be used to insert, remove, and modify elements under the focus.

For example, a [list zipper](http://eed3si9n.com/learning-scalaz/Zipper.html) consists of one element under focus together with two lists: one enumerating all the elements to the left of the focus, and another enumerating all the elements to the right of the focus. The focus can me moved left or right (like a zipper on a piece of clothing) and elements will move through the focus. The element under focus can then be removed or modified, and we can insert a new element by consing onto the lists to its left or right.

- [ ] [Object-Oriented Programming Versus Abstract Data Types](http://www.cs.utexas.edu/users/wcook/papers/OOPvsADT/CookOOPvsADT90.pdf)

###Chapter 5

#### Corecursion and codata

The [Wikipedia article on corecursion](http://en.wikipedia.org/wiki/Corecursion) is a good starting point for understanding the concept.

The article on [Coinduction](http://en.wikipedia.org/wiki/Coinduction) has some further links. Dan Piponi's article ["Data and Codata"](http://blog.sigfpe.com/2007/07/data-and-codata.html) talks about corecursion as "guarded" recursion.

Ralf Hinze's paper ["Reasoning about Codata"](http://www.cs.ox.ac.uk/ralf.hinze/publications/CEFP09.pdf) brings equational reasoning to corecursive programs by employing applicative functors. Hinze's paper will be more comprehensible to readers who have finished part 3 of our book.

#### Tying the knot

Non-strictness allows us to create cyclic streams such as:

``` scala
val cyclic: Stream[Int] = 0 #:: 1 #:: cyclic
```

This may seem like it shouldn't work. The stream is referencing itself in its own tail! But the trick is that the `#::` constructor is non-strict in its second argument. The evaluation of `cyclic` will stop without expanding the expression `1 #:: cyclic`. It's not until somebody takes the `tail` of the `tail` of `cyclic` that the recursive reference is expanded, and again it expands only one element at a time, allowing for an infinite, cyclic stream.

Note that the `cyclic` stream is reusing its own structure. `cyclic.tail.tail` is not a new stream that looks like `cyclic`. It really is the same object as `cyclic` in every sense:

```
scala> cyclic.tail.tail eq cyclic
res0: Boolean = true
```

This technique is sometimes called "Tying the Knot". For more information see the [Haskell.org article](http://www.haskell.org/haskellwiki/Tying_the_Knot).

However, be careful of creating such structures using the Scala standard library's `Stream`. They can be quite fragile.

###Chapter 6
#### `State` in Scalaz

The [Scalaz library](http://github.com/scalaz/scalaz) supplies a [`State` data type](http://docs.typelevel.org/api/scalaz/stable/7.1.0-M3/doc/#scalaz.package$$State$) that is a specialization of a more general type [`IndexedStateT`](http://docs.typelevel.org/api/scalaz/stable/7.1.0-M3/doc/#scalaz.IndexedStateT), where `State[S,A]` = `IndexedStateT[Id, S, S, A]` and `Id[A]` = `A`.

You do not need to understand this more general type if you just want to use `State[S,A]`. But the general type has two additional features:

  1. The start state and end state of a state transition can have different types. That is, it's not necessarily a transition `S => (S, A)`, but `S1 => (S2, A)`. The ordinary `State` type is where `S1` and `S2` are fixed to be the same type.
  2. It is a [_monad transformer_](http://en.wikipedia.org/wiki/Monad_transformer) (see chapter 12). The type of a state transition is not `S => (S, A)`, but `S => F[(S, A)]` for some [monad](http://en.wikipedia.org/wiki/Monad_%28functional_programming%29) `F` (see chapter 11). The monad transformer allows us to bind across `F` and `State` in one operation. The ordinary `State` type is where this monad is fixed to be the identity monad `Id`.

#### Pseudorandom number generation

The [Wikipedia article on pseudorandom number generators](http://en.wikipedia.org/wiki/Pseudorandom_number_generator) is a good place to start for more information about such generators. It also makes the distinction between _random_ and _pseudo_-random generation.

There's also a good page on [Linear congruential generators](http://en.wikipedia.org/wiki/Linear_congruential_generator), including advantages and disadvantages and links to several implementations in various languages.

#### Deterministic finite state automata

The `State` data type can be seen as a model of [Mealy Machines](http://en.wikipedia.org/wiki/Mealy_machine) in the following way. Consider a function `f` of a type like `A => State[S, B]`. It is a transition function in a Mealy machine where

* The type `S` is the set of states
* `State[S, B]`'s representation is a function of type `S => (B, S)`. Then the argument to that function is the initial state.
* The type `A` is the input alphabet of the machine.
* The type `B` is the output alphabet of the machine.

The function `f` itself is the transition function of the machine. If we expand `A => State[S, B]`, it is really `A => S => (B, S)` under the hood. If we uncurry that, it becomes `(A, S) => (B, S)` which is identical to a transition function in a Mealy machine. Specifically, the output is determined both by the state of type `S` and the input value of type `A`.

Contrast this with a [Moore machine](http://en.wikipedia.org/wiki/Moore_machine), whose output is determined solely by the current state. A Moore machine could be modeled by a data type like the following:

``` scala
case class Moore[S, I, A](t: (S, I) => S, g: S => A)
```

Together with an initial state `s` of type `S`. Here:

* `S` is the set of states.
* `I` is the input alphabet.
* `A` is the output alphabet.
* `t` is the transition function mapping the state and an input value to the next state.
* `g` is the output function mapping each state to the output alphabet.

As with Mealy machines, we could model the transition function and the output function as a single function:

``` scala
type Moore[S, I, A] = S => (I => S, A)
```

Since both the transition function `t` and the output function `g` take a value of type `S`, we can take that value as a single argument and from it determine the transition function of type `I => S` as well as the output value of type `A` at the same time.

Mealy and Moore machines are related in a way that is interesting to explore.

#### Lenses

If we specialize `Moore` so that the input and output types are the same, we get a pair of functions `t: (S, A) => S` and `g: S => A`. We can view these as (respectively) a "getter" and a "setter" of `A` values on the type `S`:

```
get: S => A
set: (S, A) => S
```

Imagine for example where `S` is `Person` and `A` is `Name`.

``` scala
type Name = String

case class Person(name: Name, age: Int)
```

A function `getName` would have the type `Person => Name`, and `setName` would have the type `(Person, Name) => Person`. In the latter case, given a `Person` and a `Name`, we can set the `name` of the `Person` and get a new `Person` with the new `name`.

The getter and setter together form what's called a _lens_. A lens "focuses" on a part of a larger structure, and allows us to modify the value under focus. A simple model of lenses is:

``` scala
case class Lens[A, B](get: A => B, set: (A, B) => A)
```

Where `A` is the larger structure, and `B` is the part of that structure that is under focus.

Importantly, lenses _compose_. That is, if you have a `Lens[A,B]`, and a `Lens[B,C]`, you can get a composite `Lens[A,C]` that focuses on a `C` of a `B` of an `A`.

Lenses are handy to use with the `State` data type. Given a `State[S,A]`. If we're interested in looking at or modifying a portion of the state, and the portion has type `T`, it can be useful to focus on a portion of the state that we're interested in using a `Lens[S,T]`. The getter and setter of a lens can be readily converted to a `State` action:

```scala
def getS[S,A](l: Lens[S, A]): State[S,A] = State(s => (l.get(s), s))
def setS[S,A](l: Lens[S, A], a: A): State[S,Unit] = State(s => (l.set(s, a), ()))
```

We cannot, however, turn a `State` action into a `Lens`, for the same reason that we cannot convert a Moore machine into a Mealy machine.

See the [Scalaz library's lenses](http://eed3si9n.com/learning-scalaz/Lens.html), the [Monocle library for Scala](https://github.com/julien-truffaut/Monocle), and the [Lens library for Haskell](https://www.fpcomplete.com/school/to-infinity-and-beyond/pick-of-the-week/a-little-lens-starter-tutorial), for more information about how to take advantage of lenses.

#### Stack overflow issues in State

The `State` data type as represented in chapter 6 suffers from a problem with stack overflows for long-running state machines. The problem is that `flatMap` contains a function call that is in tail position, but this tail call is not eliminated on the JVM.

The solution is to use a _trampoline_. Chapter 13 gives a detailed explanation of this technique. See also Rúnar's paper ["Stackless Scala With Free Monads"](http://blog.higher-order.com/assets/trampolines.pdf).

Using the trampolining data type `TailRec` from chapter 13, a stack-safe `State` data type could be written as follows:

``` scala
case class State[S,A](run: S => TailRec[(A, S)])
```

This is identical to the `State` data type we present in chapter 6, except that the result type of `run` is `TailRec[(S,A)]` instead of just `(S,A)`. See chapter 13 for a thorough discussion of `TailRec`. The important part is that the _result type_ of the `State` transition function needs to be a data type like `TailRec` that gets run at a later time by a tail recursive trampoline function.

## Part II: Functional design and combinator libraries
##Chapter 7

- [ ] Anotación 13 (página 112)
- [ ] Revisar implementación [actor](https://github.com/fpinscala/fpinscala/blob/master/exercises/src/main/scala/fpinscala/parallelism/Actor.scala#L39)
- [ ] Ejercicio 7.10

**Clases para revisar**:

- [ ] AtomicReference.java
- [ ] CountDownLatch.java


FP has a long history of using combinator libraries for expressing parallelism, and there are a lot of variations of the general idea. The main design choices in this sort of library are around how explicit to make the _forking_ and _joining_ of parallel computations. That is, should the API force the programmer to be fully explicit about when parallel tasks are being forked off into a separate logical thread, or should this be done automatically? And similarly, when waiting for the results of multiple logical threads (say, for the implementation of `map2`), should the order of these joins be something the programmer explicitly specifies or chosen by the framework? 

The library we developed in this chapter sits somewhere in the middle--it is explicit about where tasks are forked, but not when tasks are joined (notice that `map2` picks the order it waits for the two tasks whose results it is combining). The join order can be made more explicit. Simon Marlow, one of the [GHC Haskell](http://www.haskell.org/ghc/) developers, discusses this in [Parallel programming in Haskell with explicit futures](http://ghcmutterings.wordpress.com/2010/08/20/parallel-programming-in-haskell-with-explicit-futures/). Also see the full paper, [Seq no more: Better Strategies for Parallel Haskell](http://www.haskell.org/~simonmar/papers/strategies.pdf), which does a nice job of explaining some of the history of approaches for parallelism in Haskell.

Note that because Scala is a strict-by-default language, being more explicit about the join order isn't necessarily as beneficial as in Haskell. That is, we can get away with reasoning about join order much like we think about evaluation in normal strict function application.

This style of library isn't particularly good at expressing _pipeline parallelism_ that's possible when transforming streams of data. For instance, if we have a `Stream[Int]` of 10000 items, and we wish to square each value, then compute a running sum of the squared values, there is a potential for parallelism--as we are squaring values, we can be passing the squared values off to another consumer that is emitting the running sum of the values it receives. We'll be discussing stream processing and pipeline parallelism more in part 4. 

#### Notes about map fusion ####

We noted in this chapter that one of our laws for `Par`, sometimes called _map fusion_, can be used as an optimization:

``` scala
map(map(y)(g))(f) == map(y)(f compose g)
```

That is, rather than spawning a separate parallel computation to compute the second mapping, we can fold it into the first mapping. We mentioned that our representation of `Par` doesn't allow for this, as it's too 'opaque'. If we make `Par` a proper data type and give it constructors that we can pattern match on, then it's easy to implement map fusion:

``` scala
trait Par[+A] {
  def map[B](f: A => B): Par[B] = this match {
    case MapPar(p, g) => MapPar(p, g andThen f)
    case _ => MapPar(
  }
case class MapPar[A,+B](par: Par[A], f: A => B) extends Par[B]
```

Baking ad hoc optimization rules like this into our data type works, but it can sometimes get unwieldy, and it's not very modular (we don't get to reuse code if there's some other data type needing similar optimizations). There are various ways of factoring out these sorts of optimizations so our core data type (be it `Par` or some other type) stays clean, and the optimization is handled as a separate concern. Edward Kmett has a nice [blog series discussing this approach](http://comonad.com/reader/2011/free-monads-for-less/). Before embarking on that series you'll need to be familiar with the content in part 3 of this book, and you should read [the Haskell appendix](https://github.com/pchiusano/fpinscala/wiki/A-brief-introduction-to-Haskell,-and-why-it-matters) as well.

###Chapter 8

- [ ] Ej 8.19
- [ ] Ej 8.20

The style of combinator library for testing we developed in this chapter was introduced in a 2000 paper by Koen Claessen and John Hughes, [QuickCheck: A Lightweight Tool for Random Testing of Haskell Programs](http://www.eecs.northwestern.edu/~robby/courses/395-495-2009-fall/quick.pdf) (PDF). In that paper, they presented a Haskell library, called [QuickCheck](http://en.wikipedia.org/wiki/QuickCheck), which became quite popular in the FP world and has inspired similar libraries in other languages, including [ScalaCheck](https://github.com/rickynils/scalacheck/wiki/User-Guide). Many programmers who adopt this style of testing find it to be extraordinarily effective (see, for instance, this [experience report](http://blog.moertel.com/pages/seven-lessons-from-the-icfp-programming-contest) on [Tom Moertel's blog](http://blog.moertel.com/)).

The wikipedia page on [QuickCheck](http://en.wikipedia.org/wiki/QuickCheck) and the [Haskell wiki page](http://www.haskell.org/haskellwiki/Introduction_to_QuickCheck) are good places to start if you're interested in learning more about these sorts of libraries. QuickCheck sparked a number of variations, including the Haskell library [SmallCheck](https://github.com/feuerbach/smallcheck), which is focused on exhaustive enumeration.

Although property-based testing works quite well for testing pure functions, it can also be used for testing imperative code. The general idea is to generate _lists of instructions_, which are then fed to an interpreter of these actions. We then check that the pre and post-conditions are as expected. Here's a simple example of testing the mutable stack implementation from Scala's standard library ([API docs](http://www.scala-lang.org/api/current/scala/collection/mutable/ArrayStack.html)):

``` scala
forAll(Gen.listOf(Gen.choose(1,10))) { l => 
  val buf = new collection.mutable.ArrayStack[Int]
  val beforeSize = buf.size 
  l.foreach(buf.push)
  buf.beforeSize == 0 && buf.size == l.size
}
```

In this case, the "interpreter" is the `push` method on `ArrayStack`, which modifies the stack in place, and the "instructions" are simply the integers from the input list. But the basic idea can be extended to testing richer interfaces--for instance, we could generate instructions that could either `push` or `pop` elements from an `ArrayStack` (perhaps represented as a `List[Option[Int]]`), and write a property that sequences of `push` and `pop` preserve the invariants of `ArrayStack` (for instance, the final size of the stack should be the number of `push` calls minus the number of `pop` calls). Care must be taken to craft generators that produce valid sequences of instructions (for instance, `pop` without a corresponding prior `push` is not a valid input).

Similar ideas have been used for testing thread safety of concurrent programs. (See [Finding Race Conditions in Erlang with QuickCheck and PULSE](http://www.protest-project.eu/upload/paper/icfp070-claessen.pdf) (PDF)) The key insight here is that thread-safe code does not allow the nondeterminism of thread scheduling to be _observable_. That is, for any _partial order_ of instructions run concurrently, we ought to able to find some single-threaded linear sequence of these instructions with the same observable behavior (this criteria is often called _linearizability_). For instance, if our `ArrayStack` were thread-safe, we would expect that if 2 `push` operations were performed sequentially, followed by two `pop` operations and two `push` operations performed concurrently, this should yield the same result as some deterministic linear sequence of these `push` and `pop` operations). There are some subtleties and interesting questions about how to model this and how to report and minimize failing test cases. In particular, doing it the "obvious" way ends up being intractable due to having to search through a combinatorial number of interleavings to find one that satisfies the observation. The Erlang paper linked above has more details, in particular see section 4. You may be interested to explore how to incorporate these ideas into the library we developed, possibly building on the parallelism library we wrote last chapter. 

Lastly, we mention that one design goal of some libraries in this style is to avoid having to _explicitly_ construct generators. The QuickCheck library makes use of a Haskell type class to provide instances of `Gen` "automatically", and this idea has also been borrowed by [ScalaCheck](https://github.com/rickynils/scalacheck/wiki/User-Guide). This can certainly be convenient, especially for simple examples, though we often find that explicit generators are necessary to capture all the interesting constraints on the shape or form of the inputs to a function.
