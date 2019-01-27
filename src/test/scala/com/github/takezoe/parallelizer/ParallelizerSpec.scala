package com.github.takezoe.parallelizer

import java.util.concurrent.TimeoutException

import org.scalatest.FunSuite

import scala.concurrent.duration._
import scala.util.Success


class ParallelizerSpec extends FunSuite {

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
