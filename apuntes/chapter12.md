## Chapter 12

### Laws

* Identity

`map2(unit(()), fa)((_,a) => a) == fa`
`map2(fa, unit(()))((a,_) => a) == fa`

* Associativity

```scala
def assoc[A,B,C](p: (A,(B,C))): ((A,B), C) =
    p match { case (a, (b, c)) => ((a,b), c) }

product(product(fa,fb),fc) == map(product(fa, product(fb,fc)))(assoc)
```

* Naturality of product

`map2(a,b)( (a,b) => (f(a),g(b)) ) == product(map(a)(f), map(b)(g))`

### Exercises

Prove that all monads are applicative functors by showing that if the monad laws hold, the Monad implementations of map2 and map satisfy the applicative laws.

* Identity laws

```scala
map2(unit(()), fa)((_,a) => a) ==
    // map2 impl by monad
unit(()).flatMap( unit => fa.map( a => {(_,a) => a}(unit,a) ) ) ==
    // identity law for monad: unit(y).flatMap(f) == f(y)
{ unit => fa.map( a => {(_,a) => a}(unit,a) ) }( () ) ==
    // applying ()
fa.map( a => {(_,a) => a}((),a) ) ==
    // applying innermost function call
fa.map( a => a ) ==
    // definition of identity
fa.map(identity) ==
    // identity for functors: v.map(identity) == v
fa
```

The other identity law is probably something symmetrical.

* Associativity

```scala
product(product(fa,fb),fc) == product(fa, product(fb,fc)).map(assoc)
```

Definition of `map2` in terms of `flatMap` and `map`:

```scala
def map2[A,B,C](fa: F[A], fb: F[B])(f: (A,B) => C ): F[C] =
  fa.flatMap( a =>  fb.map( b => f(a,b) ) )
```

Proof:

```scala
product(product(fa,fb),fc) ==
  // definition of product (innermost)
product(map2(fa,fb){ (a,b) => (a,b) },fc) ==
  // definition of product
map2(map2(fa,fb){ (a,b) => (a,b) },fc){ (ab,c) => (ab,c) } ==
  // definition of map2 (outermost) and function application
(map2(fa,fb){ (a,b) => (a,b) }).flatMap { ab => fc.map { c => (ab,c) } } ==
  // definition of map2 and function application
(fa.flatMap { a => fb.map { b => (a,b) } }).flatMap { ab => fc.map { c => (ab,c) } } ==
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g)) AND function application
fa.flatMap { a => fb.map { b => (a,b) }.flatMap { ab => fc.map { c => (ab,c) } } }  ==
  // definition of map
fa.flatMap { a => fb.flatMap { b => unit((a,b)) }.flatMap { ab => fc.map { c => (ab,c) } } }  ==
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))  
fa.flatMap { a => fb.flatMap { b => unit((a,b)).flatMap { ab => fc.map { c => (ab,c) } } } }  ==
  // monad's identity law: unit(y).flatMap(f) == f(y)
fa.flatMap { a => fb.flatMap { b => fc.map { c => ((a,b),c) } } }  ==  
  // definition of map
fa.flatMap { a => fb.flatMap { b => fc.flatMap { c => unit(((a,b),c))  } } }
  // definition of assoc
fa.flatMap { a => fb.flatMap { b => fc.flatMap { c => unit(assoc((a,(b,c))))  } } }
  // monad's identity law: unit(y).flatMap(f) == f(y)
fa.flatMap { a => fb.flatMap { b => fc.flatMap { c => unit((b,c)).flatMap { bc => unit(assoc((a,bc))) } } } }
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
fa.flatMap { a => fb.flatMap { b => fc.flatMap { c => unit((b,c)) }.flatMap { bc => unit(assoc((a,bc))) } } }
  // monad's identity law: unit(y).flatMap(f) == f(y) AND function definition
fa.flatMap { a => fb.flatMap { b => fc.flatMap { c => unit((b,c)) }.flatMap { bc => unit((a,bc)).flatMap { a_bc => unit(assoc(a_bc))  } } } }
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
fa.flatMap { a => fb.flatMap { b => fc.flatMap { c => unit((b,c)) }.flatMap { bc => unit((a,bc)) }.flatMap { a_bc => unit(assoc(a_bc))  } } }
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
fa.flatMap { a => fb.flatMap { b => fc.flatMap { c => unit((b,c)) }.flatMap { bc => unit((a,bc)) } }.flatMap { a_bc => unit(assoc(a_bc))  } }
  // function definition
fa.flatMap { a => (fb.flatMap { b => { b => fc.flatMap { c => unit((b,c)) }.flatMap { bc => unit((a,bc)) } } }(b).flatMap { a_bc => unit(assoc(a_bc))  } ) }
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
fa.flatMap { a => (fb.flatMap { b => fc.flatMap { c => unit((b,c)) }.flatMap { bc => unit((a,bc)) } } ).flatMap { a_bc => unit(assoc(a_bc))  } }
  // function definition
fa.flatMap { a => (fb.flatMap { b => { b => fc.flatMap { c => unit((b,c)) } }(b).flatMap { bc => unit((a,bc)) } } ).flatMap { a_bc => unit(assoc(a_bc))  } }
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
fa.flatMap { a => (fb.flatMap { b => fc.flatMap { c => unit((b,c)) } }).flatMap { bc => unit((a,bc)) }.flatMap { a_bc => unit(assoc(a_bc))  } }
  // definition of map
fa.flatMap { a => (fb.flatMap { b => fc.flatMap { c => unit((b,c)) } }).map { bc => (a,bc) }.flatMap { a_bc => unit(assoc(a_bc))  } }
  // definition of map
fa.flatMap { a => (fb.flatMap { b => fc.map { c => (b,c) } }).map { bc => (a,bc) }.flatMap { a_bc => unit(assoc(a_bc))  } }
  // function definition
fa.flatMap { a => { a => (fb.flatMap { b => fc.map { c => (b,c) } }).map { bc => (a,bc) } }(a).flatMap { a_bc => unit(assoc(a_bc))  } }
  // monad's associative law: x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
fa.flatMap { a => (fb.flatMap { b => fc.map { c => (b,c) } }).map { bc => (a,bc) } }.flatMap { a_bc => unit(assoc(a_bc))  }
  // definition of product
product(fa, fb.flatMap { b => fc.map { c => (b,c) } }).flatMap { a_bc => unit(assoc(a_bc))  }
  // definition of product
product(fa, product(fb,fc)).flatMap { a_bc => unit(assoc(a_bc))  }
  // definition of map
product(fa, product(fb,fc)).map(assoc)
```
