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
import ij.process._
import net.sf.ijplugins.debayer2sx.DDFAPD.debayerGR
import net.sf.ijplugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}
import net.sf.ijplugins.debayer2sx.process.{FR, copyRanges}

object DeBayer2 {

  def process(ip: ImageProcessor, config: DeBayer2Config): ColorProcessor = {

    val bbp: Int = ip match {
      case _: ByteProcessor => 8
      case _: ShortProcessor => 16
      case _ => throw new IllegalArgumentException("Unsupported image processor type: " + ip)
    }

    config.demosaicing match {
      case Demosaicing.DDFAPD =>
        val bay = ip.convertToFloatProcessor()
        val stack = debayerDDFAPD(bay, bbp, doRefine = false, config.mosaicOrder)
        stackToColorProcessor(stack, bbp)
      case Demosaicing.DDFAPDRefined =>
        val bay = ip.convertToFloatProcessor()
        val stack = debayerDDFAPD(bay, bbp, doRefine = true, config.mosaicOrder)
        stackToColorProcessor(stack, bbp)
      case x =>
        throw new UnsupportedOperationException("Unsupported demosaicing type: " + x)
    }
  }

  private def debayerDDFAPD(src: FloatProcessor, bbp: Int, doRefine: Boolean, order: MosaicOrder): ImageStack = {

    val w = src.getWidth
    val h = src.getHeight

    val bay: FloatProcessor = order match {
      case MosaicOrder.G_R =>
        // GR
        // BG
        src
      case MosaicOrder.B_G =>
        //       gr
        // BG -> BG
        // GR    GR
        // Add extra row so the pattern starts with G_R
        val ip = new FloatProcessor(w, h + 2)
        // Copy original pixels shifted by 1
        copyRanges(ip, FR, Range(1, h + 1, 1), src, FR, FR)
        copyRanges(ip, FR, Range(0, 1, 1), src, FR, Range(1, 2, 1))
        copyRanges(ip, FR, Range(h + 1, h + 2, 1), src, FR, Range(h - 2, h - 1, 1))
        ip
      case MosaicOrder.G_B =>
        //
        // GB -> GR
        // RG    BG
        // transpose
        val ip = src.rotateLeft().asInstanceOf[FloatProcessor]
        ip.flipVertical()
        ip
      case MosaicOrder.R_G =>
        // RG -> gRG
        // GB    bGB
        // Add extra column so the pattern starts with G_R
        val ip = new FloatProcessor(w + 2, h)
        // Copy original pixels shifted by 1
        copyRanges(ip, Range(1, w + 1, 1), FR, src, FR, FR)
        copyRanges(ip, Range(0, 1, 1), FR, src, Range(1, 2, 1), FR)
        copyRanges(ip, Range(w + 1, w + 2, 1), FR, src, Range(w - 2, w - 1, 1), FR)
        ip
    }

    val stack = debayerGR(bay, bbp, doRefine = doRefine)

    // Crop as needed to remove padding used in processing
    order match {
      case MosaicOrder.G_R =>
        stack
      case MosaicOrder.B_G =>
        stack.crop(0, 1, 0, w, h, stack.size())
      case MosaicOrder.G_B =>
        // transpose
        val sp1 = new StackProcessor(stack, stack.getProcessor(1).duplicate())
        val stack2 = sp1.rotateLeft()
        val sp2 = new StackProcessor(stack2, stack2.getProcessor(1).duplicate())
        sp2.flipVertical()
        stack2
      case MosaicOrder.R_G =>
        stack.crop(1, 0, 0, w, h, stack.size())
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
