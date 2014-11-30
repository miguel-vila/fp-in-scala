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

* **EXERCISE 7.7:** Given `map(y)(id) == y`, it's a free theorem that `map(map(y)(g))(f) == map(y)(f compose g)`. (This is sometimes called map fusion, and it can be used as an optimization—rather than spawning a separate parallel computation to compute the second mapping, we can fold it into the first mapping.) Can you prove it?

Si se tienen unas funciones `f`, `g`, `p` y `q` tales que para cualquier argumento `y`:

```scala
	f(g(y)) == p(q(y)) // entonces se tiene que
	map(map(y)(g))(f) == map(map(y)(p))(q)
```

Luego partiendo de `p == id` y `q == f compose g` entonces tenemos que para cualquier argumento `y`:

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
