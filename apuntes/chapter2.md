### Chapter 2

> (...) the universe of possible implementations is significantly reduced when implementing a polymorphic function.

___
> If a function is polymorphic in some type A, the only operations that can be performed on that A are those passed into the function as arguments (or that can be defined in terms of these given operations)

___
> In some cases, you'll find that the universe of possibilities for a given polymorphic type is constrained such that there exists only a single implementation!

___
**Una explicación curiosa de currying:**

```scala
def compose[A,B,C](f: B => C, g: A => B): A => C
```

If you can exchange a banana for a carrot and an apple for a banana, you can exchange an apple for a carrot.

___
> Polymorphic, higher-order functions often end up being extremely widely applicable, precisely because they say nothing about any particular domain and are simply abstracting over a common pattern that occurs in many contexts. For this reason, programming in the large has very much the same flavor as programming in the small.