package com.github.takezoe.parallelizer

import java.util.concurrent.TimeoutException

import org.scalatest.FunSuite

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import scala.util.Try


class ParallelSpec extends FunSuite {

  test("run()"){
    val source = Seq(1, 2, 3)
    val start = System.currentTimeMillis()
    val result = Parallel.run(source, parallelism = 2){ i =>
      Thread.sleep(500)
      i * 2
    }
    val duration = System.currentTimeMillis() - start

    assert(duration > 500 && duration < 1100)
    assert(result == List(2, 4, 6))
  }

  test("iterate()"){
    val source = Seq(1, 2, 3)
    val start = System.currentTimeMillis()
    val result = Parallel.iterate(source.toIterator, parallelism = 2){ i =>
      Thread.sleep(500 * i)
      i * 2
    }

    val duration1 = System.currentTimeMillis() - start
    assert(duration1 < 500)

    // wait for completion here
    val list = result.toList

    val duration2 = System.currentTimeMillis() - start
    assert(duration2 > 1500 && duration2 < 2100)
    assert(list == List(2, 4, 6))
  }

  test("large source with run()"){
    val source = Range(0, 999)
    val start = System.currentTimeMillis()

    Parallel.run(source, parallelism = 100){ i =>
      Thread.sleep(500)
      i * 2
    }
    val duration = System.currentTimeMillis() - start

    assert(duration > 500 && duration < 6000)
  }

  test("large source with iterate()"){
    val source = Range(0, 999)
    val start = System.currentTimeMillis()

    val result = Parallel.iterate(source.toIterator, parallelism = 100){ i =>
      Thread.sleep(500)
      i * 2
    }

    val duration1 = System.currentTimeMillis() - start
    assert(duration1 < 500)

    // wait for completion here
    val list = result.toList

    val duration2 = System.currentTimeMillis() - start
    assert(duration2 > 5000 && duration2 < 6000)
  }

  test("failure in run()"){
    val source = Seq(1, 2, 3)
    val exception = new RuntimeException("failure")

    val result = Parallel.run(source, parallelism = 2){ i =>
      Try {
        if(i == 2){
          throw exception
        }
        i * 2
      }
    }

    assert(result == List(Success(2), Failure(exception), Success(6)))
  }

  test("failure in iterate()"){
    val source = Seq(1, 2, 3)
    val exception = new RuntimeException("failure")

    val result = Parallel.iterate(source.toIterator, parallelism = 2){ i =>
      Try {
        Thread.sleep(500 * i)
        if(i == 2){
          throw exception
        }
        i * 2
      }
    }

    // wait for completion here
    val list = result.toList

    assert(list == List(Success(2), Failure(exception), Success(6)))
  }

  test("timeout in run()"){
    val source = Seq(1, 2, 3)
    var count = 0

    assertThrows[TimeoutException]{
      Parallel.run(source, parallelism = 1, timeout = 1 second){ i =>
        Thread.sleep(600)
        count = count + 1
        i
      }
    }

    assert(count == 1)
  }

  test("timeout in iterate()"){
    val source = Seq(1, 2, 3).toIterator

    val result = Parallel.iterate(source, parallelism = 1, timeout = 1 second){ i =>
      Thread.sleep(600)
      i
    }

    assert(result.toList == List(1))
  }

  test("repeat()"){
    val source = Seq(0, 2, 5)
    val counter = scala.collection.mutable.HashMap[Int, Int]()
    val cancelable = Parallel.repeat(source, interval = 1 second){ e =>
      counter.update(e, counter.get(e).getOrElse(0) + 1)
      Thread.sleep(e * 1000)
    }

    Thread.sleep(4900)

    println("cancelled")
    cancelable.cancel()

    Thread.sleep(1000)

    assert(counter(0) == 5)
    assert(counter(2) == 3)
    assert(counter(5) == 1)
  }
}
