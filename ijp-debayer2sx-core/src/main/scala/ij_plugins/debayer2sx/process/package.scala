/*
 * Image/J Plugins
 * Copyright (C) 2002-2021 Jarek Sacha
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

package ij_plugins.debayer2sx

import ij.process.FloatProcessor

import scala.language.implicitConversions

package object process {

  /** Marker object that represent full range for a given processor. Similar to ":" in MATLAB. */
  val FR = Range(Int.MinValue, Int.MaxValue)

  implicit def wrapRange(r: Range): RangeMath = new RangeMath(r)

  implicit def wrapFloatProcessor(fp: FloatProcessor): FloatProcessorMath = new FloatProcessorMath(fp)

  def add(a: Array[Float], b: Array[Float]): Array[Float] = {
    require(a.length == b.length)
    a.zip(b).map(e => e._1 + e._2)
  }

  @inline
  final def duplicate(src: FloatProcessor): FloatProcessor = {
    val dst = new FloatProcessor(src.getWidth, src.getHeight)
    val srcPixels = src.getPixels.asInstanceOf[Array[Float]]
    val dstPixels = dst.getPixels.asInstanceOf[Array[Float]]
    System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length)
    dst
  }

  @inline
  final def sortedRangeParams(range: Range): (Int, Int, Int) = {
    if (range.step >= 0)
      (range.start, range.end, range.step)
    else
      (range.end + 1, range.start + 1, -range.step)
  }

}
