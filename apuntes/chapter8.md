## Chapter 8

> we should consider no library to be the final word on any subject.

___

> (...) this `check` method has a side effect of printing to the console. It’s fine to expose this as a convenience function, but it’s not a basis for composition. For instance, we couldn’t implement `&&` for `Prop` if its representation were just the `check` method:

```scala
trait Prop {
	def check: Unit
	def &&(p: Prop): Prop = ???
}
```
> Since `check` has a side effect, the only option for implementing `&&` in this case would be to run both `check` methods. So if `check` prints out a test report, then we would get two of them, and they would print failures and successes independently of each other. That’s likely not a correct implementation. 

**¿Desechar información no se puede ver como un _side-effect_?:**
> The problem is not so much that `check` has a side effect, but more generally that it throws away information.

> In order to combine `Prop` values using combinators like `&&`, we need `check` (or whatever function “runs” properties) to return some meaningful value.

___
> (...) Should we add a type parameter to `Prop` and make it `Prop[A]`? Then check could return `Either[A,Int]`. Before going too far down this path, let’s ask ourselves whether we really care about the type of the value that caused the property to fail. We don’t really. We would only care about the type if we were going to do further computation with the failure. Most likely we’re just going to end up printing it to the screen for inspection by the person running the tests.

**¿Colapsar valores a un tipo (e.g. `String`) cuando las particulartidades no importan?:**
> (...)  As a general rule, we shouldn’t use `String` to represent data that we want to compute with. But for values that we’re just going to show to human beings, a `String` is absolutely appropriate. 

___
> (...) we’re interested in understanding what operations are _primitive_ and what operations are _derived_, and in finding a small yet expressive set of primitives. A good way to explore what is expressible with a given set of primitives is to pick some concrete examples you’d like to express, and see if you can assemble the functionality you want. As you do so, **look for patterns, try factoring out these patterns into combinators, and refine your set of primitives.** 

___
> You don’t have to wait around for a concrete example to force exploration of the design space. 

> (...)  We don’t want to overfit our design to the particular examples we happen to think of right now. We want to reduce the problem to its essence, and sometimes the best way to do this is play. Don’t try to solve important problems or produce useful functionality. 

___
> Let’s look at a law we introduced for `Par` in chapter 7:
```scala
map(x)(id) == x
```
>Does this law hold for our implementation of `Gen.map`? What about for `Stream`, `List`, `Option`, and `State`? Yes, it does! Try it and see. This indicates that not only do these functions share similar-looking signatures, they also **in some sense have analogous meanings in their respective domains**. It appears there are deeper forces at work! We’re uncovering some fundamental patterns that **cut across domains**.

___
> First, we saw that oscillating between the abstract algebra and the concrete representation lets the two inform each other. This avoids overfitting the library to a particular representation, and also avoids ending up with a floating abstraction disconnected from the end goal.

___
> There are a great many seemingly distinct _problems_ being solved in the world of software, yet the space of functional _solutions_ is much smaller. Many libraries are just simple combinations of certain fundamental structures that appear over and over again across a variety of different domains. 
