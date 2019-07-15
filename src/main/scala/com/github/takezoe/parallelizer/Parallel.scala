package com.github.takezoe.parallelizer

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, Executors, LinkedBlockingQueue, TimeoutException}

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/**
 * Provides tiny utilities for parallelization.
 *
 * For example, each element of source is proceeded in parallel in the following example.
 *
 * {{{
 * val source: Seq[Int] = Seq(1, 2, 3)
 * val result: Seq[Int] = Parallelizer.run(source){ i =>
 *   ...
 * }
 * }}}
 *
 * Parallelism can be specified as a second parameter. The default value is a number of available processors.
 *
 * {{{
 * val result: Seq[Int] = Parallelizer.run(source, 100){ i =>
 *   ...
 * }
 * }}}
 */
object Parallel {

  /**
   * Process all elements of the source by the given function then wait for completion.
   *
   * @param source Source collection
   * @param parallelism Parallelism, the default value is a number of available processors
   * @param f Function which process each element of the source collection
   * @return Collection of results
   */
  def run[T, R: ClassTag](source: Seq[T],
    parallelism: Int = Runtime.getRuntime.availableProcessors(),
    timeout: Duration = Duration.Inf
  )(f: T => R): Seq[R] = {
    val requestQueue = new LinkedBlockingQueue[Worker[T, R]](parallelism)
    val resultArray = new Array[R](source.length)

    Range(0, parallelism).foreach { _ =>
      val worker = new Worker[T, R](requestQueue, resultArray, f)
      requestQueue.put(worker)
    }

    val executor = Executors.newFixedThreadPool(parallelism)

    try {
      // Process all elements of source
      val it = source.zipWithIndex.iterator
      while(it.hasNext) {
        val worker = requestQueue.take()
        worker.message.set(it.next())
        executor.execute(worker)
      }

      // Wait for completion
      while(requestQueue.size() != parallelism){
        try {
          Thread.sleep(10)
        } catch {
          case _: InterruptedException => ()
        }
      }

      resultArray.toSeq

    } catch {
      case _: InterruptedException => throw new TimeoutException()

    } finally {
      // Cleanup
      executor.shutdown()
      requestQueue.clear()
    }
  }

}
