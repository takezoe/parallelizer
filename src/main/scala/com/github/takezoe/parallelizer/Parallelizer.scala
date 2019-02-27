package com.github.takezoe.parallelizer

import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Timer, TimerTask}
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
 *
 * You can use `Iterator` instead of `Seq` as a source. This version is useful to handle a very large data.
 *
 * {{{
 * val source: Iterator[Int] = ...
 * val result: Iterator[Int] = Parallelizer.iterate(source){ i =>
 *   ...
 * }
 * }}}
 */
object Parallelizer {

  private class ResultIterator[R](queue: LinkedBlockingQueue[Option[R]]) extends Iterator[R] {

    private var nextMessage: Option[R] = None

    override def hasNext: Boolean = {
      nextMessage = queue.take()
      nextMessage.isDefined
    }

    override def next(): R = {
      nextMessage.get
    }
  }

  private class TimeoutTimerTask(thread: Thread, executor: ExecutorService) extends TimerTask {
    override def run(): Unit = {
      executor.shutdownNow()
      thread.interrupt()
    }
  }

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
    val requestQueue = new LinkedBlockingQueue[WithIndexWorker[T, R]](parallelism)
    val resultArray = new Array[R](source.length)

    Range(0, parallelism).foreach { _ =>
      val worker = new WithIndexWorker[T, R](requestQueue, resultArray, f)
      requestQueue.put(worker)
    }

    val executor = Executors.newFixedThreadPool(parallelism)

    if(timeout != Duration.Inf){
      new Timer().schedule(new TimeoutTimerTask(Thread.currentThread(), executor), timeout.toMillis)
    }

    try {
      // Process all elements of source
      val it = source.zipWithIndex.toIterator
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

  /**
   * Process all elements of the source by the given function then don't wait completion.
   * The result is an iterator which is likely a stream which elements are pushed continuously.
   *
   * @param source the source iterator
   * @param parallelism Parallelism, the default value is a number of available processors
   * @param f Function which process each element of the source collection
   * @return Iterator of results
   */
  def iterate[T, R](source: Iterator[T],
    parallelism: Int = Runtime.getRuntime.availableProcessors(),
    timeout: Duration = Duration.Inf
  )(f: T => R): Iterator[R] = {
    val requestQueue = new LinkedBlockingQueue[Worker[T, R]](parallelism)
    val resultQueue = new LinkedBlockingQueue[Option[R]]()

    Range(0, parallelism).foreach { _ =>
      val worker = new Worker[T, R](requestQueue, resultQueue, f)
      requestQueue.put(worker)
    }

    new Thread {
      override def run(): Unit = {
        val executor = Executors.newFixedThreadPool(parallelism)

        if(timeout != Duration.Inf){
          new Timer().schedule(new TimeoutTimerTask(Thread.currentThread(), executor), timeout.toMillis)
        }

        try {
          // Process all elements of source
          while(source.hasNext) {
            val worker = requestQueue.take()
            worker.message.set(source.next())
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

        } catch {
          case _: InterruptedException => throw new TimeoutException()

        } finally {
          // Terminate
          resultQueue.put(None)

          // Cleanup
          executor.shutdown()
          requestQueue.clear()
        }
      }
    }.start()

    new ResultIterator[R](resultQueue)
  }

  def repeat[T](source: Seq[T], interval: Duration)(f: T => Unit): Cancelable = {
    val requestQueue = new LinkedBlockingQueue[WithIndexWorker[T, Unit]](source.size)
    val resultArray = new Array[Unit](source.size)
    val executor = Executors.newFixedThreadPool(source.size)
    val cancelable = new Cancelable(executor)

    Range(0, source.size).foreach { _ =>
      val repeatedFunction = (arg: T) => {
        while(!cancelable.isCancelled){
          val start = System.currentTimeMillis()
          f(arg)
          val duration = System.currentTimeMillis() - start
          val wait = math.max(0, interval.toMillis - duration)
          try {
            Thread.sleep(wait)
          } catch {
            case _: InterruptedException => ()
          }
        }
      }

      val worker = new WithIndexWorker[T, Unit](requestQueue, resultArray, repeatedFunction)
      requestQueue.put(worker)
    }

    source.zipWithIndex.foreach { case (e, i) =>
      val worker = requestQueue.take()
      worker.message.set(e, i)
      executor.execute(worker)
    }

    cancelable
  }

  class Cancelable(executor: ExecutorService) {
    private val cancelled = new AtomicBoolean(false)
    def isCancelled: Boolean = cancelled.get()
    def cancel(): Unit = {
      executor.shutdownNow()
      cancelled.set(true)
    }
  }

}
