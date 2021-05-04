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

package ij_plugins.debayer2sx.experimental.v_1_2_0.core.process

import ij.process.{Blitter, FloatProcessor}

import scala.language.implicitConversions


object FloatProcessorMath {

  implicit def toFP(fpm: FloatProcessorMath): FloatProcessor = fpm.fp
}

final class FloatProcessorMath(val fp: FloatProcessor) {

  /**
    * Create new FloatProcessor by slicing current processor.
    *
    * {{{
    *   val fp = new FloatProcessor(w, h)
    *   // Select even values of x, select values y starting at 3, with increment 5, all that are less than (h-4).
    *   val a = fp(Range(0, w, 2), Range(3, h-4, 5))
    *   // Skip first and last value of x, use all y values.
    *   val b = fp(Range(1, w-1), FR)
    * }}}
    *
    * @param rangeX range to x values.
    *               For instance, Range(3, 10, 2), means select x values from 3 to 10 with step 2: [3, 5, 7, 9].
    *               You can use `FR` when you want all valid x values to be included.
    * @param rangeY range to y values.
    * @return
    */
  def apply(rangeX: Range, rangeY: Range): FloatProcessor = {
    slice(fp, rangeX, rangeY)
  }

  /**
    * Create new FloatProcessor by slicing current processor.
    *
    * @param x      single value x to be selected
    * @param rangeY range for y values.
    * @return
    */
  def apply(x: Int, rangeY: Range): FloatProcessor = {
    slice(fp, Range(x, x + 1), rangeY)
  }

  /**
    * Create new FloatProcessor by slicing current processor.
    *
    * @param rangeX range for x values.
    * @param y      single value x to be selected
    * @return
    */
  def apply(rangeX: Range, y: Int): FloatProcessor = {
    slice(fp, rangeX, Range(y, y + 1))
  }

  /**
    * Create sum of this and `other` processor element-wise.
    *
    * @return result of addition
    */
  def +(other: FloatProcessor): FloatProcessor = {
    require(fp.getWidth == other.getWidth)
    require(fp.getHeight == other.getHeight)

    val r = fp.duplicate().asInstanceOf[FloatProcessor]
    r.copyBits(other, 0, 0, Blitter.ADD)
    r
  }

  /**
    * Create difference of this and `other` processor element-wise.
    *
    * @return result of subtraction
    */
  def -(other: FloatProcessor): FloatProcessor = {
    require(fp.getWidth == other.getWidth)
    require(fp.getHeight == other.getHeight)

    val r = fp.duplicate().asInstanceOf[FloatProcessor]
    r.copyBits(other, 0, 0, Blitter.SUBTRACT)
    r
  }

  /**
    * Divide all values of this processor by `v`
    *
    * @return result of division
    */
  def /(v: Double): FloatProcessor = {

    val r = fp.duplicate().asInstanceOf[FloatProcessor]
    r.multiply(1 / v)
    r
  }

  /**
    * Multiply all values of this processor by `v`
    *
    * @return result of multiplication
    */
  def *(v: Double): FloatProcessor = {

    val r = fp.duplicate().asInstanceOf[FloatProcessor]
    r.multiply(v)
    r
  }

  @inline
  private def slice(src: FloatProcessor, srcRangeX: Range, srcRangeY: Range): FloatProcessor = {
    // bay(1:2:m,2:2:n)
    val _srcRangeX = if (srcRangeX == FR) Range(0, src.getWidth) else srcRangeX
    val _srcRangeY = if (srcRangeY == FR) Range(0, src.getHeight) else srcRangeY

    val dst = new FloatProcessor(_srcRangeX.length, _srcRangeY.length)
    for (x <- _srcRangeX; y <- _srcRangeY) {
      val v = src.getf(x, y)
      val dstX = (x - _srcRangeX.start) / _srcRangeX.step
      val dstY = (y - _srcRangeY.start) / _srcRangeY.step
      dst.setf(dstX, dstY, v)
    }
    dst
  }
}
