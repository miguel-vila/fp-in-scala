## Part I: Introduction to functional programming

### Chapter 1

> Side effects, by definition, are not tracked in the types, and so the compiler cannot alert us if we forget to perform some action as a side effect.

___
> In general, we'll learn how this sort of transformation can be applied to any function with side effects to push these effects to the "outer layers" of the program. Functional programmers often speak of implementing programs with a pure core and a thin layer on the outside that handles effects.

___
> (...) a function has no observable effect on the execution of the program other than to compute a result given its inputs; we say that it has no side effects.

___
> We can formalize this idea of pure functions using the concept of referential transparency (RT). This is a property of expressions in general and not just functions.

___
> This is all it means for an expression to be referentially transparent—in any program, the expression can be replaced by its result without changing the meaning of the program.

___
> This definition should give you some intuition that referential transparency forces all effects to be returned as values, in the result type of the function. 

___
> RT enables equational reasoning about programs.

___
> Conversely, the substitution model is simple to reason about since effects of evaluation are purely local (they affect only the expression being evaluated) and we need not mentally simulate sequences of state updates to understand a block of code. **Understanding requires only local reasoning.** 

___
> A modular program consists of components that can be understood and reused independently of the whole (...)

___
> A pure function is modular and composable because it separates the logic of the computation itself from "what to do with the result" and "how to obtain the input"; it is a black box.

### Chapter Notes

* [Paper: Why Functional Programming Matters](https://github.com/miguel-vila/fp-in-scala/blob/master/apuntes/WhyFunctionalProgrammingMatters.md)