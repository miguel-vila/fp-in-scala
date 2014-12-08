### Chapter 5

> To say a function is _non-strict_ just means that the function may choose not to evaluate one or more of its arguments. In contrast, a _strict_ function always evaluates its arguments.

___
**Formal definition of strictness:**

> If the evaluation of an expression runs forever or throws an error instead of returning a definite value, we say that the expression _does not terminate_, or that it evaluates to _bottom_. A function _f_ is _strict_ if the expression _f(x)_ evaluates to _bottom_ for all _x_ that evaluate to _bottom_.

___
> More generally speaking, laziness lets us separate the description of an expression from the evaluation of that expression. This gives us a powerful ability — we may choose to describe a "larger" expression than we need, then evaluate only a portion of it.

___
> Note that we don’t fully instantiate the intermediate stream that results from the map. It’s exactly as if we had interleaved the logic using a special-purpose loop. For this reason, people sometimes describe streams as “first-class loops” whose logic can be combined using higher-orderfunctions like map and filter.

___
> Since intermediate streams aren’t instantiated, it’s easy to reuse existing combinators in novel ways without having to worry that we’re doing more processing of the stream than necessary.

___
> The incremental nature of stream transformations also has important consequences for memory usage. Because intermediate streams aren’t generated, a transformation of the stream requires only enough working memory to store and transform the current element.

___
> Being able to reclaim this memory as quickly as possible can cut down on the amount of memory required by your program as a whole.

___
Esta definición desacopla los valores del Stream de los datos necesarios para generar esos valores:

```scala
def unfold[A, S](z: S)(f: S => Option[(A, S)]): Stream[A]
```

___
> Whereas a recursive function consumes data, a corecursive function produces data. And whereas recursive functions terminate by recursing on smaller inputs, corecursive functions need not terminate so long as they remain productive, which just means that we can always evaluate more of the result in a finite amount of time.

___
> Corecursion is also sometimes called guarded recursion, and productivity is also sometimes called cotermination.

___
> Non-strictness can be thought of as a technique for recovering some efficiency when writing functional code, but it’s also a much bigger idea—non-strictness can improve modularity by separating the description of an expression from the how-and-when of its evaluation. Keeping these concerns separate lets you reuse a description in multiple contexts, evaluating different portions of your expression to obtain different results. 