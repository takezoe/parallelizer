package com.github.takezoe.parallelizer

import java.util.concurrent.TimeoutException

import org.scalatest.FunSuite

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.{Success, Failure}


class ParallelizerSpec extends FunSuite {

  test("run()"){
    val source = Seq(1, 2, 3)
    val start = System.currentTimeMillis()
    val result = Parallelizer.run(source, parallelism = 2){ i =>
      Thread.sleep(500)
      i * 2
    }
    val duration = System.currentTimeMillis() - start

    assert(duration > 500 && duration < 1100)
    assert(result == List(Success(2), Success(4), Success(6)))
  }

  test("large source with run()"){
    val source = Range(0, 999)
    val start = System.currentTimeMillis()

    Parallelizer.run(source, parallelism = 100){ i =>
      Thread.sleep(500)
      i * 2
    }
    val duration = System.currentTimeMillis() - start

    assert(duration > 500 && duration < 6000)
  }

  test("large source with iterate()"){
    val source = Range(0, 999)
    val start = System.currentTimeMillis()

    val result = Parallelizer.iterate(source.toIterator, parallelism = 100){ i =>
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

  test("iterate()"){
    val source = Seq(1, 2, 3)
    val start = System.currentTimeMillis()
    val result = Parallelizer.iterate(source.toIterator, parallelism = 2){ i =>
      Thread.sleep(500 * i)
      i * 2
    }

    val duration1 = System.currentTimeMillis() - start
    assert(duration1 < 500)

    // wait for completion here
    val list = result.toList

    val duration2 = System.currentTimeMillis() - start
    assert(duration2 > 1500 && duration2 < 2100)
    assert(list == List(Success(2), Success(4), Success(6)))
  }

  test("failure in run()"){
    val source = Seq(1, 2, 3)
    val exception = new RuntimeException("failure")

    val result = Parallelizer.run(source, parallelism = 2){ i =>
      if(i == 2){
        throw exception
      }
      i * 2
    }

    assert(result == List(Success(2), Failure(exception), Success(6)))
  }

  test("failure in iterate()"){
    val source = Seq(1, 2, 3)
    val exception = new RuntimeException("failure")

    val result = Parallelizer.iterate(source.toIterator, parallelism = 2){ i =>
      Thread.sleep(500 * i)
      if(i == 2){
        throw exception
      }
      i * 2
    }

    // wait for completion here
    val list = result.toList

    assert(list == List(Success(2), Failure(exception), Success(6)))
  }

  test("timeout in run()"){
    val source = Seq(1, 2, 3)
    var count = 0

    assertThrows[TimeoutException]{
      Parallelizer.run(source, parallelism = 1, timeout = 1 second){ i =>
        Thread.sleep(600)
        count = count + 1
        i
      }
    }

    assert(count == 1)
  }

  test("timeout in iterate()"){
    val source = Seq(1, 2, 3).toIterator

    val result = Parallelizer.iterate(source, parallelism = 1, timeout = 1 second){ i =>
      Thread.sleep(600)
      i
    }

    assert(result.toList == List(Success(1)))
  }
}
