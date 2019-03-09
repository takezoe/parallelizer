package com.github.takezoe.parallelizer

import org.scalatest.FunSuite

import scala.language.postfixOps
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

  test("Seq.parallelMap()"){
    val source = Seq(1, 2, 3)
    val start = System.currentTimeMillis()
    val result = source.parallelMap(parallelism = 2){ i =>
      Thread.sleep(500)
      i * 2
    }
    val duration = System.currentTimeMillis() - start

    assert(duration > 500 && duration < 1100)
    assert(result == List(2, 4, 6))
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

}
