package com.github.takezoe.parallelizer

import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

import scala.util.Try

private[parallelizer] class Worker[T, R](
  requestQueue: BlockingQueue[Worker[T, R]],
  resultQueue: BlockingQueue[Option[R]],
  f: T => R) extends Runnable {

  val message: AtomicReference[T] = new AtomicReference[T]()

  override def run: Unit = {
    try {
      resultQueue.put(Some(f(message.get())))
    } finally {
      requestQueue.put(this)
    }
  }
}

private[parallelizer] class WithIndexWorker[T, R](
  requestQueue: BlockingQueue[WithIndexWorker[T, R]],
  resultArray: Array[R],
  f: T => R) extends Runnable {

  val message: AtomicReference[(T, Int)] = new AtomicReference[(T, Int)]()

  override def run: Unit = {
    try {
      val (m, i) = message.get()
      resultArray(i) = f(m)
    } finally {
      requestQueue.put(this)
    }
  }
}
