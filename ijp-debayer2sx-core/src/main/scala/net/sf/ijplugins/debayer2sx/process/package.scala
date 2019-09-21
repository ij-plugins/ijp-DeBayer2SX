/*
 * Image/J Plugins
 * Copyright (C) 2002-2019 Jarek Sacha
 * Author's email: jpsacha at gmail [dot] com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Latest release available at https://github.com/ij-plugins/ijp-DeBayer2SX
 */

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

    for (y <- _dstRangeY) {
      val indexY = (y - _dstRangeY.start) / _dstRangeY.step
      val srcY = _srcRangeY.start + indexY * _srcRangeY.step

      for (x <- _dstRangeX) {
        val indexX = (x - _dstRangeX.start) / _dstRangeX.step
        val srcX = _srcRangeX.start + indexX * _srcRangeX.step

        val v = srcIP.getf(srcX, srcY)
        dstIP.setf(x, y, v)
      }
    }
  }


}
