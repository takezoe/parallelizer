package com.github.takezoe

import scala.reflect.ClassTag

package object parallelizer {

  implicit class ParallelSeq[T](source: Seq[T]) {
    def parallelMap[R: ClassTag](parallelism: Int)(f: T => R): Seq[R] = {
      Parallel.run(source, parallelism)(f)
    }
  }

}
