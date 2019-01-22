parallelizer
====

Provides tiny utilities for parallelization.

For example, each element of source is proceeded in parallel in the following example.

```scala
import com.github.takezoe.parallelizer.Parallelizer

val source: Seq[Int] = Seq(1, 2, 3)
val result: Seq[Int] = Parallelizer.run(source){ i =>
  ...
}
```

Parallelism can be specified as a second parameter. The default value is a number of available processors.

```scala
val result: Seq[Int] = Parallelizer.run(source, 100){ i =>
  ...
}
```

You can use `Iterator` instead of `Seq` as a source. This version is useful to handle a very large data.

```scala
val source: Iterator[Int] = ...
val result: Iterator[Int] = Parallelizer.iterate(source){ i =>
  ...
}
```
