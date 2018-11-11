package net.sf.ijplugins.debayer2sx

import ij.process.{FloatProcessor, ImageProcessor}

import scala.language.implicitConversions

package object process {
  /** Marker object that represent full range for a given processor. Similar to ":" in MATLAB. */
  val FR = Range(Int.MinValue, Int.MaxValue)

  implicit def wraRange(r: Range): RangeMath = new RangeMath(r)

  implicit def wraFloatProcessor(fp: FloatProcessor): FloatProcessorMath = new FloatProcessorMath(fp)


  def add(a: Array[Float], b: Array[Float]): Array[Float] = {
    require(a.length == b.length)
    a.zip(b).map(e => e._1 + e._2)
  }

  /**
    * Copies elements between arrays within specified ranges
    *
    * MATLAB equivalent
    * {{{
    *    G0(:, 1:2:n) = bay(1:2:m, 1:2:n);
    * }}}
    *
    * Scala code
    * {{{
    *    copyRanges(
    *       G0, Range(0, w, 2), FR,
    *       bay, Range(0, w, 2), Range(0, h, 2)
    *    )
    * }}}
    *
    * @param dstIP     destination processor
    * @param dstRangeX destination X range
    * @param dstRangeY destination Y range
    * @param srcIP     source processor
    * @param srcRangeX source X range
    * @param srcRangeY source Y range
    */
  def copyRanges(dstIP: ImageProcessor, dstRangeX: Range, dstRangeY: Range,
                 srcIP: ImageProcessor, srcRangeX: Range, srcRangeY: Range): Unit = {

    val _dstRangeX = if (dstRangeX == FR) Range(0, dstIP.getWidth) else dstRangeX
    val _dstRangeY = if (dstRangeY == FR) Range(0, dstIP.getHeight) else dstRangeY
    val _srcRangeX = if (srcRangeX == FR) Range(0, srcIP.getWidth) else srcRangeX
    val _srcRangeY = if (srcRangeY == FR) Range(0, srcIP.getHeight) else srcRangeY

    for (x <- _dstRangeX; y <- _dstRangeY) {
      val indexX = (x - _dstRangeX.start) / _dstRangeX.step
      val srcX = _srcRangeX.start + indexX * _srcRangeX.step

      val indexY = (y - _dstRangeY.start) / _dstRangeY.step
      val srcY = _srcRangeY.start + indexY * _srcRangeY.step

      val v = srcIP.getf(srcX, srcY)
      dstIP.setf(x, y, v)
    }
  }


}
