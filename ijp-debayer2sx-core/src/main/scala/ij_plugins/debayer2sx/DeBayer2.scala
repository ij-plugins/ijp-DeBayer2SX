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
import ij.process.*
import ij_plugins.debayer2sx.DDFAPD.debayerGR
import ij_plugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}
import ij_plugins.debayer2sx.LoopUtils.copyRanges
import ij_plugins.debayer2sx.process.FR

object DeBayer2 {

  /**
    * Decode Bayer pattern in the input image `ip`.
    * This is a convenient helper method that give assess to all implemented algorithms.
    *
    * @param ip     image in a Bayer pattern, either 8-bit (ByteProcessor) or 16-bit (ShortProcessor).
    * @param config what algorithm and which filter order to use.
    * @return stack representing R, G, and B channels and assumed bits-per-pixel used in decoding (8 or 16).
    */
  def process(ip: ImageProcessor, config: DeBayer2Config): (ImageStack, Int) = {

    val bbp: Int = ip match {
      case _: ByteProcessor => 8
      case _: ShortProcessor => 16
      case _ => throw new IllegalArgumentException("Unsupported image processor type: " + ip)
    }

    val stack = config.demosaicing match {
      case Demosaicing.Replication =>
        Debayer1.replicate_decode(config.mosaicOrder.bayer1ID, ip)
      case Demosaicing.Bilinear =>
        Debayer1.average_decode(config.mosaicOrder.bayer1ID, ip)
      case Demosaicing.SmoothHue =>
        Debayer1.smooth_decode(config.mosaicOrder.bayer1ID, ip)
      case Demosaicing.AdaptiveSmoothHue =>
        Debayer1.adaptive_decode(config.mosaicOrder.bayer1ID, ip)
      case Demosaicing.DDFAPD =>
        debayerDDFAPD(ip.convertToFloatProcessor(), bbp, doRefine = false, config.mosaicOrder)
      case Demosaicing.DDFAPDRefined =>
        debayerDDFAPD(ip.convertToFloatProcessor(), bbp, doRefine = true, config.mosaicOrder)
      case null =>
        throw new UnsupportedOperationException("config.demosaicing cannot be null")
    }

    stack.setSliceLabel("Red", 1)
    stack.setSliceLabel("Green", 2)
    stack.setSliceLabel("Blue", 3)

    (stack, bbp)
  }

  /**
    * Convert stack created during demosaicing (Bayer pattern decoding) to a 8-bit-per-band color image.
    * The stack contains three bands R, G, and B.
    *
    * @param stack  input stack
    * @param srcBBP bits-per-pixel in the Bayer image before decoding.
    * @param dstBPP desired bits-per-pixel in the Bayer image after decoding.
    * @return color image
    */
  def stackToColorProcessor(stack: ImageStack, srcBBP: Int, dstBPP: Int = 8): ColorProcessor = {
    require(dstBPP > 0, "valueRange must be larger than 0")
    require(dstBPP <= 8, "valueRange must be less or equal 8")

    val scale = math.pow(2, dstBPP) / math.pow(2, srcBBP)
    val cp = new ColorProcessor(stack.getWidth, stack.getHeight)
    for (i <- 1 to 3) {
      val fp = stack.getProcessor(i).duplicate()
      if (srcBBP != dstBPP) fp.multiply(scale)
      cp.setChannel(i, fp.convertToByteProcessor(false))
    }

    cp
  }

  /**
    * Convert stack created during demosaicing (Bayer pattern decoding) to a 8-bit-per-band color image.
    * The stack contains three bands R, G, and B.
    *
    * @param stack  input stack.
    * @param srcBBP bits-per-pixel in the Bayer image before decoding.
    * @param dstBPP desired bits-per-pixel in the Bayer image after decoding.
    * @return color image
    */
  def stackToShortStack(stack: ImageStack, srcBBP: Int, dstBPP: Int = 16): ImageStack = {
    require(dstBPP > 0, "valueRange must be larger than 0")
    require(dstBPP <= 16, "valueRange must be less or equal 16")

    val dst = new ImageStack(stack.getWidth, stack.getHeight)
    val dstRange = math.pow(2, dstBPP)
    val scale = math.pow(2, dstBPP) / math.pow(2, srcBBP)
    for ((label, i) <- Array("Red", "Green", "Blue").zipWithIndex) {
      val fp = stack.getProcessor(i + 1).duplicate()
      if (srcBBP != dstBPP) fp.multiply(scale)
      val ip = fp.convertToShortProcessor(false)
      ip.setMinAndMax(0, dstRange)
      dst.addSlice(label, ip)
    }

    dst
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

}
