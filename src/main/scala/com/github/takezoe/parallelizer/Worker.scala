package com.github.takezoe.parallelizer

import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

private[parallelizer] class Worker[T, R](
  requestQueue: BlockingQueue[Worker[T, R]],
  resultQueue: BlockingQueue[Option[R]],
  f: T => R) extends Runnable {

  val message: AtomicReference[T] = new AtomicReference[T]()

  override def run: Unit = {
    try {
      val result = f(message.get())
      resultQueue.put(Some(result))
    } finally {
      requestQueue.put(this)
    }
  }
}
