parallelizer [![Scala CI](https://github.com/takezoe/parallelizer/actions/workflows/scala.yml/badge.svg)](https://github.com/takezoe/parallelizer/actions/workflows/scala.yml) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.takezoe/parallelizer_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.takezoe/parallelizer_2.12) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/takezoe/parallelizer/blob/master/LICENSE)
====

A library offering tiny utilities for parallelization.

## Installation

```scala
libraryDependencies += "com.github.takezoe" %% "parallelizer" % "0.0.6"
```

## Usage

For example, each element of the source collection can be proceeded in parallel as the following example.

```scala
import com.github.takezoe.parallelizer.Parallel

val source: Seq[Int] = Seq(1, 2, 3)

// Run each element in parallel, but this call is blocked. Result order is preserved.
val result: Seq[Int] = Parallel.run(source){ i: Int =>
  ...
}
```

Parallelism can be specified as a second parameter. The default parallelism is a number of available processors.

```scala
// Run with 100 threads. Result order is preserved.
val result: Seq[Int] = Parallel.run(source, parallelism = 100){ i: Int =>
  ...
}
```

Parallel execution can be broken immediately by calling `Parallel.break` in the closure. It waits for the completion is ongoing elements, but drops all elements remaining when it called.

```scala
// Break parallel execution if terminated flag is true
val result: Seq[Int] = Parallel.run(source, parallelism = 100){ i: Int =>
  if (terminated) {
    Parallel.break
  }
  ...
}
```

Implicit class which offers syntax sugar to use these methods easily is also available.

```scala
import com.github.takezoe.parallelizer._

val source: Seq[Int] = Seq(1, 2, 3)

source.parallelMap(parallelism = 2){ i: Int =>
  ...
}
```
