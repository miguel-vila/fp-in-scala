## Chapter 11

### Functor laws

For Functor , we’ll stipulate the familiar law we first introduced in chapter 7 for our Par data type:

`map(x)(a => a) == x`

In other words, mapping over a structure x with the identity function should itself be an identity. This law is quite natural, and we noticed later in part 2 that this law was satisfied by the map functions of other types besides Par . This law (and its corollaries given by parametricity) capture the requirement that map(x) “preserves the structure” of x . Implementations satisfying this law are restricted from doing strange things like throwing exceptions, removing the first element of a List , converting a Some to None , and so on. Only the elements of the structure are modified by map ; the shape or structure itself is left intact. Note that this law holds for List , Option , Par , Gen , and most other data types that define map!

### Monad laws

* Associative law

Nice example of the practical significance of the associative law:

```scala
case class Order(item: Item, quantity: Int)
case class Item(name: String, price: Double)
val genOrder: Gen[Order] = for {
    name <- Gen.stringN(3)
    price <- Gen.uniform.map(_ * 10)
    quantity <- Gen.choose(1,100)
} yield Order(Item(name, price), quantity)
```

Here we’re generating the `Item` inline (from `name` and `price`), but there might be places where we want to generate an `Item` separately. So we could pull that into its own generator:

```scala
val genItem: Gen[Item] = for {
    name <- Gen.stringN(3)
    price <- Gen.uniform.map(_ * 10)
} yield Item(name, price)
```

Then we can use that in `genOrder` :

```scala
val genOrder: Gen[Order] = for {
    item <- genItem
    quantity <- Gen.choose(1,100)
} yield Order(item, quantity)
```

And that should do exactly the same thing, right? It seems safe to assume that. But not so fast. How can we be sure? It’s not exactly the same code.

We assume the associative law:

`x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))`

An easier way to see this is with `compose`:

```scala
def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
    { a => flatMap(f(a))(g) }
```

Restating the associative laws :

`compose(compose(f,g),h) == compose(f,compose(g,h))`

Both formulations are the same.

Proof:

Applying `x` to `compose(compose(identity,f),g)`:

```scala
compose(compose(identity,f),g)(x) ==
    // Impl of compose
compose({ b => identity(b).flatMap(f) }, g)(x) ==
    // Impl of compose
{ a => ({ b => identity(b).flatMap(f) }(a)).flatMap(g) }(x) ==
    // Applying x
({ b => identity(b).flatMap(f) }(x)).flatMap(g) ==
    // Applying x
identity(x).flatMap(f).flatMap(g) ==
    // Definition of identity
x.flatMap(f).flatMap(g)
```

```scala
compose(identity,compose(f,g))(x) ==
    // Impl of compose
compose(identity,{ a => f(a).flatMap(g) })(x) ==
    // Impl of compose
{ b => identity(b).flatMap( a => f(a).flatMap(g) ) }(x) ==
    // Applying x
identity(x).flatMap( a => f(a).flatMap(g) ) ==
    // Definition of identity
x.flatMap( a => f(a).flatMap(g) )
```

* Identity laws

Using `compose`:

`compose(identity, unit) == f`

`compose(unit, f) == f`

Using `flatMap`:

`flatMap(x)(unit) == x`

`flatMap(unit(y))(f) == f(y)`

Proof that both law definitions are equivalent:

The first one:

Having `f(y) == x` and starting by applying `y` to `compose(f, unit)`:

```scala
compose(f, unit)(y) ==
    // Compose impl.
{ a => flatMap(f(a))(unit) }(y) ==
    // Applying y
flatMap(f(y))(unit) ==
    // f(y) == x
flatMap(x)(unit)
```

Doing the same with the right side `f`:

```scala
f(y) ==
    // f(y) == x
x
```

The second one:

Applying `y` to `compose(unit,f)`:

```scala
compose(unit,f)(y) ==
    // Compose impl.
{ a => flatMap(unit(a))(f) }(y) ==
    // Applying y
flatMap(unit(y))(f)
```

Applying `y` to `f` we get `f(y)`.
