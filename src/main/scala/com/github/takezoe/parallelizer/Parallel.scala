package com.github.takezoe.parallelizer

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, Executors, LinkedBlockingQueue, TimeoutException}

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import scala.util.control.ControlThrowable
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

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

  private class BreakException extends ControlThrowable

  // Parallel execution can be stopped by calling this method.
  def break: Unit = throw new BreakException()

  /**
    * Process all elements of the source by the given function then wait for completion.
    *
    * @param source Source collection
    * @param parallelism Parallelism, the default value is a number of available processors
    * @param f Function which process each element of the source collection
    * @return Collection of results
    */
  def run[T, R: ClassTag](
      source: Seq[T],
      parallelism: Int = Runtime.getRuntime.availableProcessors(),
      timeout: Duration = Duration.Inf
  )(f: T => R): Seq[R] = {
    val requestQueue = new LinkedBlockingQueue[Worker[T, R]](parallelism)
    val resultQueue  = new LinkedBlockingQueue[R]()
    val interrupted  = new AtomicBoolean(false)

    Range(0, parallelism).foreach { _ =>
      val worker = new Worker[T, R](requestQueue, resultQueue, interrupted, f)
      requestQueue.put(worker)
    }

    val executor = Executors.newFixedThreadPool(parallelism)

    try {
      // Process all elements of source
      val it = source.iterator
      while (it.hasNext && !interrupted.get()) {
        val worker = requestQueue.take()
        if (!interrupted.get()) {
          worker.message.set(it.next())
          executor.execute(worker)
        } else {
          requestQueue.put(worker)
        }
      }

      // Wait for completion
      while (requestQueue.size() != parallelism) {
        try {
          Thread.sleep(10)
        } catch {
          case _: InterruptedException => ()
        }
      }

      resultQueue.asScala.toSeq
    } catch {
      case _: InterruptedException => throw new TimeoutException()

    } finally {
      // Cleanup
      executor.shutdown()
      requestQueue.clear()
    }
  }

  private class Worker[T, R](
      requestQueue: BlockingQueue[Worker[T, R]],
      resultQueue: BlockingQueue[R],
      interrupted: AtomicBoolean,
      f: T => R
  ) extends Runnable {

    val message: AtomicReference[T] = new AtomicReference[T]()

    override def run: Unit = {
      try {
        resultQueue.put(f(message.get()))
      } catch {
        case _: BreakException =>
          interrupted.set(true)
      } finally {
        requestQueue.put(this)
      }
    }
  }
}
