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

  /**
    * Encode color image in a Bayer pattern.
    *
    * @param cp          input color image
    * @param mosaicOrder filter order
    * @return Bayer pattern coded image.
    */
  def process(cp: ColorProcessor, mosaicOrder: MosaicOrder): ByteProcessor = {
    val fpR = cp.getChannel(1, null).convertToFloatProcessor()
    val fpG = cp.getChannel(2, null).convertToFloatProcessor()
    val fpB = cp.getChannel(3, null).convertToFloatProcessor()

    val w = cp.getWidth
    val h = cp.getHeight
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

    fpBayer.convertToByteProcessor(false)
  }

}
