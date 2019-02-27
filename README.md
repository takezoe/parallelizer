parallelizer [![Build Status](https://travis-ci.org/takezoe/parallelizer.svg?branch=master)](https://travis-ci.org/takezoe/parallelizer) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.takezoe/parallelizer_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.takezoe/parallelizer_2.12) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/takezoe/parallelizer/blob/master/LICENSE)
====

A library offering tiny utilities for parallelization.

## Installation

```scala
libraryDependencies += "com.github.takezoe" %% "parallelizer" % "0.0.3"
```

## Usage

For example, each element of source can be proceeded in parallel as the following example.

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

You can use `Iterator` instead of `Seq` as a source. This version is useful to handle a very large data.

```scala
val source: Iterator[Int] = Seq(1, 2, 3).toIterator

// Read from iterator one by one, and this call is not blocked. Result order is not preserved.
val result: Iterator[Int] = Parallel.iterate(source){ i: Int =>
  ...
}

// Blocked here until all elements are proceeded. Elements come in order of completion.
result.foreach { r: Int =>
  ...
}
```

`Parallel` also has a method to run a given function with each element of the source repeatedly.

```scala
val source: Seq[Int] = Seq(1, 2, 3)

// Run each element every 10 seconds
val cancellable = Parallel.repeat(source, 10 seconds){ i =>
  ...
}

// Stop running
cancellable.cancel()
```

Implicit classes which offers syntax sugar to use these methods easily are also available.

```scala
import com.github.takezoe.parallelizer._

val source: Seq[Int] = Seq(1, 2, 3)

source.parallelMap(parallelism = 2){ i =>
  ...
}

source.parallelRepeat(interval = 10 seconds){ i =>
  ...
}
```
