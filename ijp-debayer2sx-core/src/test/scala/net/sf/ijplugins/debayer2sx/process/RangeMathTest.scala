package net.sf.ijplugins.debayer2sx.process

import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class RangeMathTest extends FlatSpec with BeforeAndAfter with Matchers {

  behavior of "RangeMath"

  it should "add to a range" in {
    val r = Range(5, 10, 3)
    val delta = 3

    val r2 = r + delta

    r2.start should be(r.start + delta)
    r2.end should be(r.end + delta)
    r2.step should be(r.step)
  }

  it should "subtract from a range" in {
    val r = Range(5, 10, 3)
    val delta = 3

    val r2 = r - delta

    r2.start should be(r.start - delta)
    r2.end should be(r.end - delta)
    r2.step should be(r.step)
  }


}
