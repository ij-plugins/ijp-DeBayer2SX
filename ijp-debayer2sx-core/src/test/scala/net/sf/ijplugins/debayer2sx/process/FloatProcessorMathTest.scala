package net.sf.ijplugins.debayer2sx.process

import ij.process.FloatProcessor
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class FloatProcessorMathTest extends FlatSpec with BeforeAndAfter with Matchers {

  behavior of "FloatProcessorMath"

  it should "slice to correct size" in {
    val fp = new FloatProcessor(12, 30)
    val fp1 = fp(Range(0, 12, 2), Range(0, 30, 3))

    fp1.getWidth should be(6)
    fp1.getHeight should be(10)
  }

}
