# Why Functional Programming Matters

* In this paper we show that two features of functional languages in particular, higher-order functions and lazy evaluation, can contribute significantly to modularity.

* We conclude that since modularity is the key to successful programming, functional programming offers important advantages for software development.

* A function call can have no effect other than to compute its result.

* Since expressions can be evaluated at any time, one can freely replace variables by their values and vice versa — that is, programs are “referentially transparent”

* The functional programmer sounds rather like a mediæval monk, denying himself the pleasures of life in the hope that it will make him virtuous. To those more interested in material benefits, these “advantages” are totally unconvincing.

* (...) a functional programmer is an order of magnitude more productive than his or her conventional counterpart, because functional programs are an order of magnitude shorter. 

* Modular design brings with it great productivity improvements. First of all, small modules can be coded quickly and easily. Second, general-purpose modules can be reused, leading to faster development of subsequent programs. Third, the modules of a program can be tested independently, helping to reduce the time spent debugging.

* Therefore, to increase one’s ability to modularize a problem conceptually, one must provide new kinds of glue in the programming language. Complicated scope rules and provision for separate compilation help only with clerical details — they can never make a great contribution to modularization.

* We shall argue in the remainder of this paper that functional languages provide two new, very important kinds of glue.

* Examining this definition, we see that only the boxed parts (`0` and `+`) below are specific to computing a sum.

```miranda
sum Nil = 0
sum (Cons n list) = n + sum list
```

* This means that the computation of a sum can be modularized by gluing together a general recursive pattern and the boxed parts. This recursive pattern is conventionally called `foldr` and so `sum` can be expressed as

```miranda
sum = foldr (+) 0
```
* The definition of `foldr` can be derived just by parameterizing the definition of `sum`, giving

```miranda
(foldr f x) Nil = x
(foldr f x) (Cons a l ) = f a ((foldr f x) l )
```

* The most interesting part is `foldr`, which can be used to write down a function for multiplying together the elements of a list with no further programming:

```miranda
product = foldr (∗) 1
```

* It can also be used to test whether any of a list of booleans is `true`

```miranda
anytrue = foldr (∨) False
```

* or whether they are all `true`

```miranda
alltrue = foldr (∧) True
```

* One way to understand `(foldr f a)` is as a function that replaces all occurrences of `Cons` in a list by `f` , and all occurrences of `Nil` by `a`. Taking the list `[1, 2, 3]` as an example, since this means `Cons 1 (Cons 2 (Cons 3 Nil ))` then `(foldr (+) 0)` converts it into `(+) 1 ((+) 2 ((+) 3 0)) = 6` and `(foldr (∗) 1)` converts it into `(∗) 1 ((∗) 2 ((∗) 3 1)) = 6`

* `map` se puede expresar con `foldr`:

```miranda
map f = foldr (Cons . f ) Nil
```
* All this can be achieved because functional languages allow functions that are indivisible in conventional programming languages to be expressed as a combinations of parts — a general higher-order function and some particular specializing functions. Once defined, such higher-order functions allow many operations to be programmed very easily.

* Since this method of evaluation runs f as little as possible, it is called “lazy evaluation”. It makes it practical to modularize a program as a generator that constructs a large number of possible answers, and a selector that chooses the appropriate one.

* Lazy evaluation is perhaps the most powerful tool for modularization in the functional programmer’s repertoire.

* Can lazy evaluation and side-effects coexist? Unfortunately, they cannot: Adding lazy evaluation to an imperative notation is not actually impossible, but the combination would make the programmer’s life harder, rather than easier.

* (...) lazy evaluation’s power depends on the programmer giving up any direct control over the order in which the parts of a program are executed, it would make programming with side effects rather difficult, because predicting in what order —or even whether— they might take place would require knowing a lot about the context in which they are embedded. Such global interdependence would defeat the very modularity that —in functional languages— lazy evaluation is designed to enhance.

* (...) modularity means more than modules. Our ability to decompose a problem into parts depends directly on our ability to glue solutions together.

* Functional programming languages provide two new kinds of glue — higher-order functions and lazy evaluation.

* This paper provides further evidence that lazy evaluation is too important to be relegated to second class citizenship. It is perhaps the most powerful glue functional programmers possess. One should not obstruct access to such a vital tool.