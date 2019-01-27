package com.github.takezoe.parallelizer

import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

import scala.util.Try

private[parallelizer] class Worker[T, R](
  requestQueue: BlockingQueue[Worker[T, R]],
  resultQueue: BlockingQueue[Option[Try[R]]],
  f: T => R) extends Runnable {

  val message: AtomicReference[T] = new AtomicReference[T]()

  override def run: Unit = {
    try {
      val t = Try {
        f(message.get())
      }
      resultQueue.put(Some(t))
    } finally {
      requestQueue.put(this)
    }
  }
}

private[parallelizer] class WithIndexWorker[T, R](
  requestQueue: BlockingQueue[WithIndexWorker[T, R]],
  resultQueue: BlockingQueue[Option[(Try[R], Int)]],
  f: T => R) extends Runnable {

  val message: AtomicReference[(T, Int)] = new AtomicReference[(T, Int)]()

  override def run: Unit = {
    try {
      val (m, i) = message.get()
      val t = Try {
        f(m)
      }
      resultQueue.put(Some((t, i)))
    } finally {
      requestQueue.put(this)
    }
  }
}
