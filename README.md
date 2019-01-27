parallelizer
====

A library offering tiny utilities for parallelization.

For example, each element of source is proceeded in parallel in the following example.

```scala
import com.github.takezoe.parallelizer.Parallelizer

val source: Seq[Int] = Seq(1, 2, 3)

// Run each element in parallel, but this call is blocked. Result order is preserved.
val result: Seq[Try[Int]] = Parallelizer.run(source){ i: Int =>
  ...
}

// Result type is Try[R] because some elements might fail to process asynchronously.
result.foreach { r =>
  r match {
    case Success(i) => ...
    case Failure(e) => ...
  }
}
```

Parallelism can be specified as a second parameter. The default value is a number of available processors.

```scala
// Run with 100 threads. Result order is preserved.
val result: Seq[Try[Int]] = Parallelizer.run(source, 100){ i: Int =>
  ...
}
```

You can use `Iterator` instead of `Seq` as a source. This version is useful to handle a very large data.

```scala
val source: Iterator[Int] = Seq(1, 2, 3).toIterator

// Read from iterator one by one, and this call is not blocked. Result order is not preserved.
val result: Iterator[Try[Int]] = Parallelizer.iterate(source){ i: Int =>  
  ...
}

// Blocked here until all elements are proceeded. Elements come in order of completion.
result.foreach { r: Try[Int] =>
  r match {
    case Success(i) => ...
    case Failure(e) => ...
  }
}
```
