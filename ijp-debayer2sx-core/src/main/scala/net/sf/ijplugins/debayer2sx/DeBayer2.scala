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

import ij.ImageStack
import ij.process.{ByteProcessor, ColorProcessor, ImageProcessor, ShortProcessor}
import net.sf.ijplugins.debayer2sx.DDFAPD.debayerGR
import net.sf.ijplugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}

object DeBayer2 {

  def process(ip: ImageProcessor, config: DeBayer2Config): ColorProcessor = {

    val bbp: Int = ip match {
      case _: ByteProcessor => 8
      case _: ShortProcessor => 16
      case _ => throw new IllegalArgumentException("Unsupported image processor type: " + ip)
    }

    if (config.mosaicOrder != MosaicOrder.G_R) {
      throw new UnsupportedOperationException("Unsupported mosaic order: " + config.mosaicOrder)
    }

    config.demosaicing match {
      case Demosaicing.DDFAPD =>
        val bay = ip.convertToFloatProcessor()
        val stack = debayerGR(bay, bbp, doRefine = false)
        stackToColorProcessor(stack, bbp)
      case Demosaicing.DDFAPDRefined =>
        val bay = ip.convertToFloatProcessor()
        val stack = debayerGR(bay, bbp, doRefine = true)
        stackToColorProcessor(stack, bbp)
      case x =>
        throw new UnsupportedOperationException("Unsupported demosaicing type: " + x)
    }
  }


  private def stackToColorProcessor(stack: ImageStack, bbp: Int): ColorProcessor = {
    val scale = 256 / math.pow(2, bbp)
    val cp = new ColorProcessor(stack.getWidth, stack.getHeight)
    for (i <- 1 to 3) {
      val fp = stack.getProcessor(i)
      fp.multiply(scale)
      cp.setChannel(i, fp.convertToByteProcessor(false))
    }

    cp
  }


}
