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

import ij.process.{FloatProcessor, ImageProcessor}
import ij_plugins.debayer2sx.process.{FR, sortedRangeParams}

import java.awt.Rectangle

object LoopUtils {

  final def crop(src: FloatProcessor, roi: Rectangle): FloatProcessor = {
    val width = src.getWidth
    val pixels = src.getPixels.asInstanceOf[Array[Float]]
    val roiX = roi.x
    val roiY = roi.y
    val roiWidth = roi.width
    val roiHeight = roi.height
    val ip2 = new FloatProcessor(roiWidth, roiHeight)
    val pixels2 = ip2.getPixels.asInstanceOf[Array[Float]]
    //    for (ys <- roiY until roiY + roiHeight) {

    var ys = roiY
    while (ys < roiY + roiHeight) {
      var offset1 = (ys - roiY) * roiWidth
      var offset2 = ys * width + roiX
      //      for (xs <- 0 until roiWidth) {
      var xs = 0
      while (xs < roiWidth) {
        pixels2(offset1) = pixels(offset2)
        offset1 += 1
        offset2 += 1
        xs += 1
      }
      ys += 1
    }
    ip2
  }

  /**
    * Clip values in the image to the range specified by `bpp`.
    *
    * {{{
    *   min = 0
    *   max = 2^bpp - 1
    * }}}
    *
    * @param ip  image to check
    * @param bpp bits per pixel
    */
  final def checkImg(ip: FloatProcessor, bpp: Int): Unit = {
    val maxVal = (math.pow(2, bpp) - 1).toFloat
    val pixels = ip.getPixels.asInstanceOf[Array[Float]]

    //    for (i <- pixels.indices) {
    var i = 0
    while (i < pixels.length) {
      val v = pixels(i)
      if (v > maxVal) {
        pixels(i) = maxVal
      } else if (v < 0) {
        pixels(i) = 0
      }

      i += 1
    }
  }

  @inline
  final def slice(src: FloatProcessor, srcRangeX: Range, srcRangeY: Range): FloatProcessor = {
    // bay(1:2:m,2:2:n)
    val _srcRangeX = if (srcRangeX == FR) Range(0, src.getWidth) else srcRangeX
    val _srcRangeY = if (srcRangeY == FR) Range(0, src.getHeight) else srcRangeY

    val (xStart, xEnd, xStep) = sortedRangeParams(_srcRangeX)
    val (yStart, yEnd, yStep) = sortedRangeParams(_srcRangeY)

    val srcWidth = src.getWidth
    val srcPixels = src.getPixels.asInstanceOf[Array[Float]]

    val dstWidth = _srcRangeX.length
    val dstHeight = _srcRangeY.length
    val dst = new FloatProcessor(dstWidth, dstHeight)
    val dstPixels = dst.getPixels.asInstanceOf[Array[Float]]

    var y = yStart
    while (y < yEnd) {
      val srcOffsetY = y * srcWidth
      val dstY = (y - _srcRangeY.start) / _srcRangeY.step
      val dstOffsetY = dstY * dstWidth

      var x = xStart
      while (x < xEnd) {
        val dstX = (x - _srcRangeX.start) / _srcRangeX.step
        dstPixels(dstX + dstOffsetY) = srcPixels(x + srcOffsetY)

        x += xStep
      }

      y += yStep
    }
    dst
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
  final def copyRanges(
                        dstIP: ImageProcessor,
                        dstRangeX: Range,
                        dstRangeY: Range,
                        srcIP: ImageProcessor,
                        srcRangeX: Range,
                        srcRangeY: Range
                      ): Unit = {

    val _dstRangeX = if (dstRangeX == FR) Range(0, dstIP.getWidth) else dstRangeX
    val _dstRangeY = if (dstRangeY == FR) Range(0, dstIP.getHeight) else dstRangeY
    val _srcRangeX = if (srcRangeX == FR) Range(0, srcIP.getWidth) else srcRangeX
    val _srcRangeY = if (srcRangeY == FR) Range(0, srcIP.getHeight) else srcRangeY

    val (dstXStart, dstXEnd, dstXStep) = sortedRangeParams(_dstRangeX)
    val (dstYStart, dstYEnd, dstYStep) = sortedRangeParams(_dstRangeY)

    var y = dstYStart
    while (y < dstYEnd) {
      val indexY = (y - _dstRangeY.start) / _dstRangeY.step
      val srcY = _srcRangeY.start + indexY * _srcRangeY.step
      val srcYOffset = srcY * srcIP.getWidth
      val dstYOffset = y * dstIP.getWidth

      var x = dstXStart
      while (x < dstXEnd) {
        val indexX = (x - _dstRangeX.start) / _dstRangeX.step
        val srcX = _srcRangeX.start + indexX * _srcRangeX.step

        val v = srcIP.getf(srcX + srcYOffset)
        dstIP.setf(x + dstYOffset, v)

        x += dstXStep
      }

      y += dstYStep
    }
  }

}
