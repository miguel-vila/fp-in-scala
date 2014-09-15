### Chapter 4

* Another way of understanding RT is that the meaning of RT expressions _does not depend on context_ and may be reasoned about locally, while the meaning of non-RT expressions is _context dependent_ and requires more global reasoning. 

* The problems with exceptions:

⋅⋅* (...) exceptions break RT and introduce context dependence. (...) This is the source of the folklore advice that
exceptions should be used only for "error handling", not for "control flow".

⋅⋅* _Exceptions are not typesafe_. The type of `failingFn` is `Int => Int`, which tells us nothing about the fact that exceptions may occur, and the compiler will certainly not force callers of `failingFn` to make a decision about whether to reraise or handle any exceptions thrown by `failingFn`. If we accidentally forget to check for an exception in `failingFn`, this won't be detected until runtime.