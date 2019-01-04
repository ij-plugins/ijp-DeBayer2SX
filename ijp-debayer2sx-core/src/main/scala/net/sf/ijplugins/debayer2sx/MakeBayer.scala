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
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */

package net.sf.ijplugins.debayer2sx

import ij.process.{ByteProcessor, ColorProcessor, FloatProcessor}
import net.sf.ijplugins.debayer2sx.DeBayer2Config.MosaicOrder
import net.sf.ijplugins.debayer2sx.process.copyRanges

object MakeBayer {

  def process(cp: ColorProcessor, mosaicOrder: MosaicOrder): ByteProcessor = {

    mosaicOrder match {
      case MosaicOrder.G_R => bayerGR(cp)
      case x => throw new UnsupportedOperationException("Unsupported bayer order: " + x)
    }

  }


  /**
    * Encode color image in a Bayer pattern with RG ordering.
    *
    * @param cp input color image
    * @return Bayer pattern coded image.
    */
  def bayerGR(cp: ColorProcessor): ByteProcessor = {
    val fpR = cp.getChannel(1, null).convertToFloatProcessor()
    val fpG = cp.getChannel(2, null).convertToFloatProcessor()
    val fpB = cp.getChannel(3, null).convertToFloatProcessor()

    val w = cp.getWidth
    val h = cp.getHeight
    val fpBayer = new FloatProcessor(w, h)

    // Green
    copyRanges(fpBayer, Range(0, w, 2), Range(0, h, 2), fpG, Range(0, w, 2), Range(0, h, 2))
    // Red
    copyRanges(fpBayer, Range(1, w, 2), Range(0, h, 2), fpR, Range(1, w, 2), Range(0, h, 2))
    // Blue
    copyRanges(fpBayer, Range(0, w, 2), Range(1, h, 2), fpB, Range(0, w, 2), Range(1, h, 2))
    // Green
    copyRanges(fpBayer, Range(1, w, 2), Range(1, h, 2), fpG, Range(1, w, 2), Range(1, h, 2))

    fpBayer.convertToByteProcessor(false)
  }
}
