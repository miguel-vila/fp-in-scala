### Chapter 6

> The key to recovering referential transparency is to make the state updates explicit. Don’t update the state as a side effect, but simply return the new state along with the value that you’re generating.

___
> Rather than returning only the generated random number (as is done in `scala.util.Random`) and updating some internal state by mutating it in place, you return the random number and the new state, leaving the old state unmodified.3 In effect, you separate the concern of *computing* what the next state is from the concern of *communicating* the new state to the rest of the program. 

___
> It’s also possible in some cases to mutate the data in place without breaking referential transparency.

___
> (You can) Reimplement `map` and `map2` in terms of `flatMap`. The fact that this is possible is what we’re referring to when we say that `flatMap` is more powerful than `map` and `map2`.

___
> (...) functional programming is simply programming without side effects. Imperative programming is about programming with statements that modify some program state, and as you’ve seen, it’s entirely reasonable to maintain state without side effects.

___
> Functional programming has excellent support for writing imperative programs, with the added benefit that such programs can be reasoned about equationally because they’re referentially transparent.

___
> These two simple actions, together with the `State` combinators that we wrote — `unit`, `map`, `map2`, and `flatMap` — are all the tools you need to implement any kind of state machine or stateful program in a purely functional way.