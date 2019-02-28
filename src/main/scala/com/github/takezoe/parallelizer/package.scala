package com.github.takezoe

import com.github.takezoe.parallelizer.Parallel.Stoppable

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

package object parallelizer {

  implicit class ParallelSeq[T](source: Seq[T]) {
    def parallelMap[R: ClassTag](parallelism: Int)(f: T => R): Seq[R] = {
      Parallel.run(source, parallelism)(f)
    }

    def parallelRepeat(interval: Duration)(f: T => Unit): Stoppable = {
      Parallel.repeat(source, interval)(f)
    }
  }

  implicit class ParallelIterator[T](source: Iterator[T]) {
    def parallelMap[R: ClassTag](parallelism: Int)(f: T => R): Iterator[R] = {
      Parallel.iterate(source, parallelism)(f)
    }
  }

}
