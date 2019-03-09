package com.github.takezoe.parallelizer

import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

private[parallelizer] class Worker[T, R](
  requestQueue: BlockingQueue[Worker[T, R]],
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
