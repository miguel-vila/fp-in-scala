# Apuntes 'Functional Programming in Scala'

## Chapter 1

* Side effects, by definition, are not tracked in the types, and so the compiler cannot alert us if we forget to perform some action as a side effect.

* In general, we'll learn how this sort of transformation can be applied to any function with side effects to push these effects to the "outer layers" of the program. Functional programmers often speak of implementing programs with a pure core and a thin layer on the outside that handles effects.

* (...) a function has no observable effect on the execution of the program other than to compute a result given its inputs; we say that it has no side effects.

* We can formalize this idea of pure functions using the concept of referential transparency (RT). This is a property of expressions in general and not just functions.

* This is all it means for an expression to be referentially transparent—in any program, the expression can be replaced by its result without changing the meaning of the program.

* This definition should give you some intuition that referential transparency forces all effects to be returned as values, in the result type of the function. 

* RT enables equational reasoning about programs.

* Conversely, the substitution model is simple to reason about since effects of evaluation are purely local (they affect only the expression being evaluated) and we need not mentally simulate sequences of state updates to understand a block of code. **Understanding requires only local reasoning.** 

* A modular program consists of components that can be understood and reused independently
of the whole (...)
* A pure function is modular and composable because it separates the logic of the computation itself from "what to do with the result" and "how to obtain the input"; it is a black box.

## Chapter 2

* (...) the universe of possible implementations is significantly reduced when implementing a polymorphic function.

* If a function is polymorphic in some type A, the only operations that can be performed on that A are those passed into the function as arguments (or that can be defined in terms of these given operations)

* In some cases, you'll find that the universe of possibilities for a given polymorphic type is constrained such that there exists only a single implementation!

* **UNa explicación curiosa de currying:**

```scala
def compose[A,B,C](f: B => C, g: A => B): A => C
```

If you can exchange a banana for a carrot and an apple for a banana, you can exchange an apple for a carrot.

* Polymorphic, higher-order functions often end up being extremely widely applicable, precisely because they say nothing about any particular domain and are simply abstracting over a common pattern that occurs in many contexts. For this reason, programming in the large has very much the same flavor as programming in the small.

## Chapter 4

* Another way of understanding RT is that the meaning of RT expressions _does not depend on context_ and may be reasoned about locally, while the meaning of non-RT expressions is _context dependent_ and requires more global reasoning. 

* The problems with exceptions:

..* (...) exceptions break RT and introduce context dependence. (...) This is the source of the folklore advice that
exceptions should be used only for "error handling", not for "control flow".

..* _Exceptions are not typesafe_. The type of `failingFn` is `Int => Int`, which tells us nothing about the fact that exceptions may occur, and the compiler will certainly not force callers of `failingFn` to make a decision about whether to reraise or handle any exceptions thrown by `failingFn`. If we accidentally forget to check for an exception in `failingFn`, this won't be detected until runtime.

## Chapter 5

* To say a function is _non-strict_ just means that the function may choose not to evaluate one or more of its arguments. In contrast, a _strict_ function always evaluates its arguments.

* **Formal definition of strictness:**

> If the evaluation of an expression runs forever or throws an error instead of returning a definite value, we say that the expression _does not terminate_, or that it evaluates to _bottom_. A function _f_ is _strict_ if the expression _f(x)_ evaluates to _bottom_ for all _x_ that evaluate to _bottom_.

* More generally speaking, laziness lets us separate the description of an expression from the evaluation of that expression. This gives us a powerful ability — we may choose to describe a "larger" expression than we need, then evaluate only a portion of it.

* Note that we don’t fully instantiate the intermediate stream that results from the map. It’s exactly as if we had interleaved the logic using a special-purpose loop. For this reason, people sometimes describe streams as “first-class loops” whose logic can be combined using higher-orderfunctions like map and filter.

* Since intermediate streams aren’t instantiated, it’s easy to reuse existing combinators in novel ways without having to worry that we’re doing more processing of the stream than necessary.

* The incremental nature of stream transformations also has important consequences for memory usage. Because intermediate streams aren’t generated, a transformation of the stream requires only enough working memory to store and transform the current element.

* Being able to reclaim this memory as quickly as possible can cut down on the amount of memory required by your program as a whole.

* Esta definición desacopla los valores del Stream de los datos necesarios para generar esos valores:

```scala
def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A]
```

* Whereas a recursive function consumes data, a corecursive function produces data. And whereas recursive functions terminate by recursing on smaller inputs, corecursive functions need not terminate so long as they remain productive, which just means that we can always evaluate more of the result in a finite amount of time.

* Corecursion is also sometimes called guarded recursion, and productivity is also sometimes called cotermination.

* Non-strictness can be thought of as a technique for recovering some efficiency when writing functional code, but it’s also a much bigger idea—non-strictness can improve modularity by separating the description of an expression from the how-and-when of its evaluation. Keeping these concerns separate lets you reuse a description in multiple contexts, evaluating different portions of your expression to obtain different results. 

## Chapter 6

* The key to recovering referential transparency is to make the state updates explicit. Don’t update the state as a side effect, but simply return the new state along with the value that you’re generating.

* Rather than returning only the generated random number (as is done in `scala.util.Random`) and updating some internal state by mutating it in place, you return the random number and the new state, leaving the old state unmodified.3 In effect, you separate the concern of *computing* what the next state is from the concern of *communicating* the new state to the rest of the program. 

* It’s also possible in some cases to mutate the data in place without breaking referential transparency.

* (You can) Reimplement `map` and `map2` in terms of `flatMap`. The fact that this is possible is what we’re referring to when we say that `flatMap` is more powerful than `map` and `map2`.

* (...) functional programming is simply programming without side effects. Imperative programming is about programming with statements that modify some program state, and as you’ve seen, it’s entirely reasonable to maintain state without side effects.

* Functional programming has excellent support for writing imperative programs, with the added benefit that such programs can be reasoned about equationally because they’re referentially transparent.

* These two simple actions, together with the `State` combinators that we wrote — `unit`, `map`, `map2`, and `flatMap` — are all the tools you need to implement any kind of state machine or stateful program in a purely functional way.