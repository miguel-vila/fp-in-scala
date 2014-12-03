## Chapter 7

* **(About Library Design):** (...) we want to encourage the view that no existing library is authoritative or beyond reexamination, even if designed by experts and labeled “standard.”

* `Thread` also has the disadvantage that it maps directly onto operating system threads, which are a scarce resource. It would be preferable to create as many “logical threads” as is natural for your problem, and later deal with mapping these onto actual OS threads.

* We’ll use the term **logical thread** somewhat informally throughout this chapter to mean a computation that runs concurrently with the main execution thread of our program. There need not be a one-to-one correspondence between **logical threads** and **OS threads**. We may have a large number of logical threads mapped onto a smaller number of OS threads via thread pooling, for instance.


* **(About Library Design):** (...) note what we’ve done. First, we conjured up a simple, almost trivial example. We next explored this example a bit to uncover a design choice. Then, via some experimentation, we discovered an interesting consequence of one option and in the process learned something fundamental about the nature of our problem domain! The overall design process is a series of these little adventures

* **(About Library Design):** You don’t need any special license to do this sort of exploration, and you don’t need to be an expert in functional programming either. Just dive in and see what you find.

* (...) That is, our current API is very _inexplicit_ about when computations get forked off the main thread—the programmer doesn’t get to specify where this forking should occur. What if we make the forking more explicit? We can do that by inventing another function, `def fork[A](a: => Par[A]): Par[A]`, which we can take to mean that the given `Par` should be run in a separate logical thread.

* With fork, we can now make `map2` strict, leaving it up to the programmer to wrap arguments if they wish. (...) it puts the parallelism explicitly under programmer control. We’re addressing two concerns here:

⋅⋅* The first is that we need some way to indicate that the results of the two parallel tasks should be combined.

⋅⋅* Separate from this, we have the choice of whether a particular task should be performed asynchronously.

* **(About Library Design):** (...) avoid having any sort of global policy for parallelism attached to `map2` and other combinators we write, **(-> avoid attaching global or implicit policies to your combinator functions)** which would mean making tough (and ultimately arbitrary) choices about what global policy is best.

* **(About Library Design):** When you’re unsure about a meaning to assign to some function in your API, you can always continue with the design process—at some point later the trade-offs of different choices of meaning may become clear. Here we make use of a helpful trick—we’ll think about what sort of _information_ is required to implement `fork` and `get` with various meanings.

* (...) (we might like for each subsystem of a large application to get its own thread pool with different parameters, for example). It seems much more appropriate to give get the responsibility of creating threads and submitting execution tasks.

* **(About Library Design):** At any point while sketching out an API, you can start thinking about possible representations for the abstract types that appear.

* **(About Library Design):** The simplest possible model for `Par[A]` might be `ExecutorService => A`. **(-> Queríamos algo que codificase una computación paralela. Una función que recibe los recursos necesarios (el _executor service_) y que devuelve un valor `A` codifica esto)**

* This would obviously make `run` trivial to implement. But it might be nice to defer the decision of how long to wait for a computation, or whether to cancel it, to the caller of `run`. So `Par[A]` becomes `ExecutorService => Future[A]`, and run simply returns the `Future`. **(-> Esta decisión difiere y desacopla ciertas decisiones)**

* **(About Library Design):** here aren’t such clear boundaries between designing your API and choosing a representation, and one doesn’t necessarily precede the other. 

* **(About Library Design):** before we add any new primitive operations, let’s try to learn more about what’s expressible using those we already have.

* (...) we often get far just by writing down the type signature for an operation we want, and then “following the types” to an implementation. (...) This isn’t cheating; it’s a natural style of reasoning, analogous to the reasoning one does when simplifying an algebraic equation.

* Up until now, we’ve been reasoning somewhat informally about our API. There’s nothing wrong with this, but it can be helpful to take a step back and formalize what laws you expect to hold (or would like to hold) for your API.

* **Paralelos entre leyes y funciones:** Laws and functions share much in common. Just as we can generalize functions, we can generalize laws. (...) Much like we strive to define functions in terms of simpler functions, each of which _do_ just one thing, we can define laws in terms of simpler laws that each _say_ just one thing.

* Laws often start out this way, as concrete examples of **identities** we expect to hold.

* For now, let’s say two `Par` objects are equivalent if for any valid `ExecutorService` argument, their `Future` results have the same value.

* **Leyes simples "rechazan" implementaciones sin sentido:**
```scala
map(unit(x))(f) == unit(f(x))
```
(...) This places some constraints on our implementation. Our implementation of unit can’t, say, inspect the value it receives and decide to return a parallel computation with a result of 42 when the input is 1—it can only pass along whatever it receives.

* Simplificando la anterior ley:
```scala
map(unit(x))(f) == unit(f(x)) // <- Initial law
map(unit(x))(id) == unit(id(x)) // <- Substitute identity function for f.
map(unit(x))(id) == unit(x) // <- Simplify.
map(y)(id) == y // <- Substitute y for unit(x) on both sides.
```
Our new, simpler law talks only about `map`—apparently the mention of `unit` was an extraneous detail. To get some insight into what this new law is saying, let’s think about what map can’t do. It can’t, say, throw an exception and crash the computation before applying the function to the result.

Even more interestingly, given `map(y)(id) == y`, we can perform the substitutions in the other direction to get back our original, more complex law. (Try it!) **Logically, we have the freedom to do so because map can’t possibly behave differently for different function types it receives**. Thus, given `map(y)(id)== y`, it must be true that `map(unit(x))(f) == unit(f(x))`. Since we get this second law or theorem for free, simply because of the **parametricity** of map, it’s sometimes called a **free theorem**.

* **About library design:** After you’ve written down a law like this, take off your implementer hat, put on your debugger hat, and try to break your law. Think through any possible corner cases, try to come up with counterexamples, and even construct an informal proof that the law holds—at least enough to convince a skeptical fellow programmer.

* **Side effects** hurt compositionality, but more generally, **any hidden or out-of-band assumption or behavior that prevents us from treating our components (be they functions or anything else) as black boxes** makes composition difficult or impossible.

* When you find counterexamples like this, you have two choices—you can try to fix your implementation such that the law holds, or you can refine your law a bit, to state more explicitly the conditions under which it holds (you could simply stipulate that you require thread pools that can grow unbounded). Even this is a good exercise—it forces you to document invariants or assumptions that were previously implicit.

* (...) we’re making use of a common technique of using **side effects** as an implementation detail for a purely functional API. We can get away with this because the side effects we use are not observable to code that uses `Par`. Note that `Future.apply` is protected and can’t even be called by outside code.

* An Actor is essentially a concurrent process **that doesn’t constantly occupy a thread**. Instead, **it only occupies a thread when it receives a message**. Importantly, although multiple threads may be concurrently sending messages to an actor, **the actor processes only one message at a time, queueing other messages for subsequent processing**. This makes them useful as a concurrency primitive when writing tricky code that must be accessed by multiple threads, and which would otherwise be prone to race conditions or deadlocks.

* The main trickiness in an actor implementation has to do with the fact that multiple threads may be messaging the actor simultaneously. The implementation needs to ensure that messages are processed only one at a time, and also that all messages sent to the actor will be processed eventually rather than queued indefinitely.

* **About library design:** In general, there are multiple approaches you can consider when choosing laws for your API. You can think about your conceptual model, and reason from there to postulate laws that should hold. You can also just _invent_ laws you think might be useful or instructive (like we did with our `fork` law), and see if it’s possible and sensible to ensure that they hold for your model. And lastly, you can look at your _implementation_ and come up with laws you expect to hold based on that.

* **¿Subutilizar un API es un signo de que uno puede generalizar más?:** If we look at our implementation of `choiceMap`, we can see we aren’t really using much of the API of `Map`.

* As you practice more functional programming, one of the skills you’ll develop is the ability to recognize _what functions are expressible from an algebra_, and what the limitations of that algebra are. (...) As a practical consideration, being able to reduce an API to a minimal set of primitive functions is extremely useful. As we noted earlier when we implemented `parMap` in terms of existing combinators, it’s frequently the case that primitive combinators encapsulate some rather tricky logic, and reusing them means we don’t have to duplicate this logic.

* **EXERCISE 7.7:** Given `map(y)(id) == y`, it's a free theorem that `map(map(y)(g))(f) == map(y)(f compose g)`. (This is sometimes called map fusion, and it can be used as an optimization—rather than spawning a separate parallel computation to compute the second mapping, we can fold it into the first mapping.) Can you prove it?

Si se tienen unas funciones `f`, `g`, `p` y `q` tales que para cualquier argumento `y`:

```scala
	f(g(y)) == p(q(y)) // entonces se tiene que
	map(map(y)(g))(f) == map(map(y)(p))(q)
```

Luego reemplazando `p == id` y `q == f compose g` entonces tenemos que para cualquier argumento `y`:

```scala
	map(map(y)(g))(f) == 
	map(map(y)(id))(f compose g) == // Ya que map(y)(id) == y
	map(y)(f compose g)
```

* **EXERCISE 7.9:** Show that any fixed-size thread pool can be made to deadlock given this implementation of fork.

```scala
def fork[A](a: => Par[A]): Par[A] =
	es => es.submit(new Callable[A] {
		def call = a(es).get
	})
```

Suponga un _pool_ de _threads_ de tamaño fijo igual a _n_ y que se llama el método `fork` _n_ veces concurrentemente. Cada llamada empezará a ejecutar el `es.submit(...)` y por lo tanto cada una va a ocupar un _thread_. Luego los _n threads_ del _pool_ se van a ocupar en ese momento. Después, cada llamada, que ha ocupado un _thread_, deberá ejecutar `a(es).get` para completarse. Pero para hacer esto debe ser capaz de reclamar uno de los _n threads_ que han sido ocupados. Cada uno de estos _threads_ ha sido ocupado esperando la liberación de otro _thread_. Por lo tanto se produce un _deadlock_.
