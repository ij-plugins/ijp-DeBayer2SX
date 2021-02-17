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

import ij.ImageStack
import ij.process.{ByteProcessor, ColorProcessor, FloatProcessor, ImageProcessor}
import ij_plugins.debayer2sx.DeBayer2Config.MosaicOrder
import ij_plugins.debayer2sx.process.copyRanges

/**
  * Create Bayer images from color images. This is mostly useful for testing and demos.
  */
object MakeBayer {

  /**
    * Convert RGB color image to stack of gray level images.
    *
    * @param cp color image
    * @return stack of 3 8-bit images
    */
  def toStack(cp: ColorProcessor): ImageStack = {
    val w: Int = cp.getWidth
    val h: Int = cp.getHeight
    val size: Int = w * h
    val r = new Array[Byte](size)
    val g = new Array[Byte](size)
    val b = new Array[Byte](size)
    cp.getRGB(r, g, b)
    val stack = new ImageStack(w, h)
    stack.addSlice("Red", r)
    stack.addSlice("Green", g)
    stack.addSlice("Blue", b)
    stack.setColorModel(cp.getDefaultColorModel)
    stack
  }


  /**
    * Encode color image in a Bayer pattern.
    *
    * @param cp          input color image
    * @param mosaicOrder filter order
    * @return Bayer pattern coded image.
    */
  def process(cp: ColorProcessor, mosaicOrder: MosaicOrder): ByteProcessor = {
    process(toStack(cp), mosaicOrder).asInstanceOf[ByteProcessor]
  }

  /**
    * Encode color image in a Bayer pattern.
    *
    * @param stack       input color image represented as a stack of 3 gray level images.
    * @param mosaicOrder filter order
    * @return Bayer pattern coded image of the same bit depth as slices in the input stack.
    */
  def process(stack: ImageStack, mosaicOrder: MosaicOrder): ImageProcessor = {

    require(stack.size() == 3)
    require(stack.getBitDepth == 8 || stack.getBitDepth == 16 || stack.getBitDepth == 32)

    val fpR = stack.getProcessor(1).convertToFloatProcessor()
    val fpG = stack.getProcessor(2).convertToFloatProcessor()
    val fpB = stack.getProcessor(3).convertToFloatProcessor()

    val w = stack.getWidth
    val h = stack.getHeight
    val fpBayer = new FloatProcessor(w, h)

    // ip00 ip10
    // ip01 ip11
    val (ip00, ip10, ip01, ip11) = mosaicOrder match {
      case MosaicOrder.B_G =>
        // B G
        // G R
        (fpB, fpG, fpG, fpR)

      case MosaicOrder.G_B =>
        // G B
        // R G
        (fpG, fpB, fpR, fpG)

      case MosaicOrder.G_R =>
        // G R
        // B G
        (fpG, fpR, fpB, fpG)

      case MosaicOrder.R_G =>
        // R G
        // G B
        (fpR, fpG, fpG, fpB)
    }

    // ip00
    copyRanges(fpBayer, Range(0, w, 2), Range(0, h, 2), ip00, Range(0, w, 2), Range(0, h, 2))
    // ip10
    copyRanges(fpBayer, Range(1, w, 2), Range(0, h, 2), ip10, Range(1, w, 2), Range(0, h, 2))
    // ip01
    copyRanges(fpBayer, Range(0, w, 2), Range(1, h, 2), ip01, Range(0, w, 2), Range(1, h, 2))
    // ip11
    copyRanges(fpBayer, Range(1, w, 2), Range(1, h, 2), ip11, Range(1, w, 2), Range(1, h, 2))

    stack.getBitDepth match {
      case 8 => fpBayer.convertToByte(false)
      case 16 => fpBayer.convertToShort(false)
      case 32 => fpBayer.convertToFloat()
      case bitDepth => throw new IllegalArgumentException("Unsupported bit depth: " + bitDepth)
    }
  }
}
