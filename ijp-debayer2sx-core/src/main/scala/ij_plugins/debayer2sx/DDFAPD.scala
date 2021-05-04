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
import ij.plugin.filter.Convolver
import ij.process._
import ij_plugins.debayer2sx.process.{FR, add, copyRanges, _}

import java.awt.Rectangle

/**
  * ImageJ implementation of
  * "Demosaicing with Directional Filtering and a Posteriori Decision", D. Menon, S. Andriani, and G. Calvagno,
  * IEEE Trans. Image Processing, vol. 16 no. 1, Jan. 2007.
  *
  * @author Jarek Sacha
  */
object DDFAPD {

  // Parts of the implementation is based on the reference MATLAB code by Daniele Menon.


  /**
    * Convert Bayer encoded image to to stack of color bands (red, green, blue).
    * The demosaicing is done assuming GB bayer pattern variant (top left corner pixel is G, next in row is R).
    *
    * @param bay      Bayer-pattern image input image
    * @param doRefine if `true` the final refinement will be applied.
    * @return reconstructed color image
    */
  def debayerGR(bay: ByteProcessor, doRefine: Boolean): ColorProcessor = {

    val stack = debayerGR(bay.convertToFloatProcessor(), 8, doRefine)

    val cp = new ColorProcessor(bay.getWidth, bay.getHeight)
    for (i <- 1 to 3) cp.setChannel(i, stack.getProcessor(i).convertToByteProcessor(false))

    cp
  }


  /**
    * Convert Bayer encoded image to to stack of color bands (red, green, blue).
    * The demosaicing is done assuming GB bayer pattern variant (top left corner pixel is G, next in row is R).
    *
    * @param bay      Bayer-pattern image input image
    * @param bpp      bits-per-pixel, this would be typically 8 or 16.
    * @param doRefine if `true` the final refinement will be applied.
    * @return reconstructed image as stack of bands
    */
  def debayerGR(bay: FloatProcessor, bpp: Int, doRefine: Boolean): ImageStack = {
    require(bay.getWidth > 0, s"Image width must be greater than 0, got ${bay.getWidth}.")
    require(bay.getWidth % 2 == 0, s"Image width must be even (multiple of 2), got ${bay.getWidth}.")
    require(bay.getHeight > 0, s"Image height must be greater than 0, got ${bay.getHeight}.")
    require(bay.getHeight % 2 == 0, s"Image height must be even (multiple of 2), got ${bay.getHeight}.")

    //
    // Horizontal and vertical interpolation of the green channel
    //
    // [Gh,Gv]=ddfapd_intDirezG(bay);
    // Gh=check_img(Gh,bpp);
    // Gv=check_img(Gv,bpp);
    val r1 = intDirezG(bay)
    val Gh = r1._1
    val Gv = r1._2
    checkImg(Gh, bpp)
    checkImg(Gv, bpp)

    //
    // Decision between the two estimated green values
    //
    // [outG, interpDir]=ddfapd_decision(Gh,Gv,bay);
    val r2 = decision(Gh, Gv, bay)
    val outG = r2._1
    val interpDir = r2._2

    //
    // Red and blue reconstruction

    // [outR, outB]=ddfapd_interpRB(bay,outG,interpDir);
    val r3 = interpRB(bay, outG, interpDir)
    val outR = r3._1
    val outB = r3._2
    //    out=check_img(out,bpp);
    checkImg(outR, bpp)
    checkImg(outG, bpp)
    checkImg(outB, bpp)

    //
    // Refining of the reconstructed image')
    //
    // out=ddfapd_refining(out,interpDir);
    // out=check_img(out,bpp);
    val (r, g, b) = if (doRefine) {
      val r4 = refining(outR, outG, outB, interpDir)
      val outRRefined = r4._1
      val outGRefined = r4._2
      val outBRefined = r4._3
      checkImg(outRRefined, bpp)
      checkImg(outGRefined, bpp)
      checkImg(outBRefined, bpp)

      (outRRefined, outGRefined, outBRefined)
    } else {
      (outR, outG, outB)
    }

    val stack = new ImageStack(bay.getWidth, bay.getHeight)
    stack.addSlice(r)
    stack.addSlice(g)
    stack.addSlice(b)

    stack
  }


  /**
    * Directional interpolation of the green channel.
    * Reconstructs two estimates of the green channel from the bayer data `bay`,
    * using horizontal and vertical interpolation, to produce `gh` and `gv`, respectively.
    *
    * @param bay Bayer-pattern image
    * @return (gh, gv) horizontally (gh) and vertically (gv) interpolated green image
    */
  private[debayer2sx] def intDirezG(bay: FloatProcessor): (FloatProcessor, FloatProcessor) = {

    // [m,n]=size(bay);
    val w = bay.getWidth
    val h = bay.getHeight

    // h0=[-0.25, 0, 0.5, 0, -0.25];
    // h1=[0, 0.5, 0, 0.5, 0];
    val h0 = Array[Float](-0.25f, 0f, 0.5f, 0f, -0.25f)
    val h1 = Array[Float](0f, 0.5f, 0f, 0.5f, 0f)

    //
    // Horizontal interpolation
    //
    val gh: FloatProcessor = {
      // Odd rows
      //
      // G0=zeros(m/2,n);
      // G0(:,1:2:n)=bay(1:2:m,1:2:n);
      val G0 = new FloatProcessor(w, h / 2)
      copyRanges(
        G0, Range(0, w, 2), Range(0, h / 2),
        bay, Range(0, w, 2), Range(0, h, 2)
      )

      // R1=zeros(m/2,n);
      // R1(:,2:2:n)=bay(1:2:m,2:2:n);
      val R1 = new FloatProcessor(w, h / 2)
      copyRanges(
        R1, Range(1, w, 2), Range(0, h / 2),
        bay, Range(1, w, 2), Range(0, h, 2)
      )

      // f1=filtImg(h1+[0 0 1 0 0],G0,1);
      val f11 = filtImg(add(h1, Array(0f, 0f, 1f, 0f, 0f)), G0, 1)
      // f2 = filtImg(h0, R1, 1)
      val f21 = filtImg(h0, R1, 1)

      // Gh=zeros(m,n);
      val Gh = new FloatProcessor(w, h)
      // Gh(1:2:m,:)=f1+f2;
      copyRanges(Gh, FR, Range(0, h, 2), /* = */ f11 + f21, FR, FR)

      // Even rows
      //
      // B0=zeros(m/2,n);
      val B0 = new FloatProcessor(w, h / 2)
      // B0(:,1:2:n)=bay(2:2:m,1:2:n);
      copyRanges(B0, Range(0, w, 2), FR, /* = */ bay, Range(0, w, 2), Range(1, h, 2))

      // G1=zeros(m/2,n);
      val G1 = new FloatProcessor(w, h / 2)
      // G1(:,2:2:n)=bay(2:2:m,2:2:n);
      copyRanges(G1, Range(1, w, 2), FR, /* = */ bay, Range(1, w, 2), Range(1, h, 2))

      // f1=filtImg(h1+[0,0,1,0,0],G1,1);
      val f12 = filtImg(add(h1, Array(0f, 0f, 1f, 0f, 0f)), G1, 1)
      // f2=filtImg(h0,B0,1);
      val f22 = filtImg(h0, B0, 1)
      // Gh(2:2:m,:)=f1+f2;
      copyRanges(Gh, FR, Range(1, h, 2), /* = */ f12 + f22, FR, FR)
      Gh
    }

    //
    // Vertical interpolation
    //
    val gv: FloatProcessor = {
      // Gv=zeros(m,n);
      val Gv = new FloatProcessor(w, h)

      // Odd columns
      //
      // G0=zeros(m,n/2);
      val G0 = new FloatProcessor(w / 2, h)
      // G0(1:2:m,:)=bay(1:2:m,1:2:n);
      copyRanges(G0, FR, Range(0, h, 2), /* = */ bay, Range(0, w, 2), Range(0, h, 2))

      // B1=zeros(m,n/2);
      val B1 = new FloatProcessor(w / 2, h)
      // B1(2:2:m,:)=bay(2:2:m,1:2:n);
      copyRanges(B1, FR, Range(1, h, 2), /* = */ bay, Range(0, w, 2), Range(1, h, 2))

      // f1=filtImg([0 0 1 0 0]+h1,G0,2);
      val f11 = filtImg(add(Array(0f, 0f, 1f, 0f, 0f), h1), G0, 2)
      // f2=filtImg(h0,B1,2);
      val f21 = filtImg(h0, B1, 2)

      // Gv(:,1:2:n)=f1+f2;
      copyRanges(Gv, Range(0, w, 2), FR, /* = */ f11 + f21, FR, FR)

      // Even columns
      //
      // R0=zeros(m,n/2);
      val R0 = new FloatProcessor(w / 2, h)
      // R0(1:2:m,:)=bay(1:2:m,2:2:n);
      copyRanges(R0, FR, Range(0, h, 2), /* = */ bay, Range(1, w, 2), Range(0, h, 2))

      // G1=zeros(m,n/2);
      val G1 = new FloatProcessor(w / 2, h)
      // G1(2:2:m,:)=bay(2:2:m,2:2:n);
      copyRanges(G1, FR, Range(1, h, 2), /* = */ bay, Range(1, w, 2), Range(1, h, 2))

      // f1=filtImg(h1+[0 0 1 0 0],G1,2);
      val f12 = filtImg(add(h1, Array(0f, 0f, 1f, 0f, 0f)), G1, 2)
      // f2=filtImg(h0,R0,2);
      val f22 = filtImg(h0, R0, 2)

      // Gv(:,2:2:n)=f1+f2;
      copyRanges(Gv, Range(1, w, 2), FR, /* = */ f12 + f22, FR, FR)

      Gv
    }

    (gh, gv)
  }

  /**
    * Directional filtering of the image `x` along the direction `dir`.
    * Near the border of the image the neighboring pixels are replicated.
    */
  private[debayer2sx] def filtImg(hh: Array[Float], x: FloatProcessor, dir: Int): FloatProcessor = {
    // [m,n]=size(x);
    val w = x.getWidth
    val h = x.getHeight

    //    B = (length(h)-1)/2;
    val B = (hh.length - 1) / 2

    val y = if (dir == 1) {
      // Add mirroring of the borders
      // xx = [x(:,1+B:-1:2), x, x(:,n-1:-1:n-B)];
      val xx = mirrorBorderWidth(x, B)

      //    y=conv2(1,h,xx,'valid');
      new Convolver().convolveFloat1D(xx, hh, hh.length, 1, 1)
      val cropROI = new Rectangle(B, 0, w, h)
      //      xx.setRoi(cropROI)
      //      xx.crop().asInstanceOf[FloatProcessor]
      crop(xx, cropROI)
    } else if (dir == 2) {
      // Add mirroring of the borders
      // xx = [x(1+B:-1:2,:); x; x(m-1:-1:m-B,:)];
      val xx = mirrorBorderHeight(x: FloatProcessor, B)

      // y=conv2(h,1,xx,'valid');
      new Convolver().convolveFloat1D(xx, hh, 1, hh.length, 1)
      val cropROI = new Rectangle(0, B, w, h)
      //      xx.setRoi(cropROI)
      //      xx.crop().asInstanceOf[FloatProcessor]
      crop(xx, cropROI)
    } else {
      throw new IllegalArgumentException("Invalid `dir` value:" + dir)
    }

    y
  }

  private[this] def crop(src: FloatProcessor, roi: Rectangle): FloatProcessor = {
    import io.github.metarank.cfor._

    val width = src.getWidth
    val pixels = src.getPixels.asInstanceOf[Array[Float]]
    val roiX = roi.x
    val roiY = roi.y
    val roiWidth = roi.width
    val roiHeight = roi.height
    val ip2 = new FloatProcessor(roiWidth, roiHeight)
    val pixels2 = ip2.getPixels.asInstanceOf[Array[Float]]
    //    for (ys <- roiY until roiY + roiHeight) {
    cfor(roiY)(_ < roiY + roiHeight, _ + 1) { ys =>
      var offset1 = (ys - roiY) * roiWidth
      var offset2 = ys * width + roiX
      //      for (xs <- 0 until roiWidth) {
      cfor(0)(_ < roiWidth, _ + 1) { _ =>
        pixels2(offset1) = pixels(offset2)
        offset1 += 1
        offset2 += 1
      }
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
  private[this] def checkImg(ip: FloatProcessor, bpp: Int): Unit = {
    import io.github.metarank.cfor._

    val maxVal = (math.pow(2, bpp) - 1).toFloat
    val pixels = ip.getPixels.asInstanceOf[Array[Float]]

    //    for (i <- pixels.indices) {
    cfor(0)(_ < pixels.length, _ + 1) { i =>
      val v = pixels(i)
      if (v > maxVal) {
        pixels(i) = maxVal
      } else if (v < 0) {
        pixels(i) = 0
      }
    }
  }


  /**
    * The absolute norm between two values.
    */
  private def nor(g1: FloatProcessor, g2: FloatProcessor): FloatProcessor = {
    val fp = g1 - g2
    for (i <- Range(0, fp.getPixelCount)) {
      //    y=abs(g1(:,:)-g2(:,:));
      fp.setf(i, math.abs(fp.getf(i)))
    }
    fp
  }

  /**
    * Decision for the best directional reconstruction, uses the horizontally interpolated image `Gh`,
    * the vertically interpolated image Gv and the Bayer-sampled `bay` to estimate for each pixel
    * the best reconstruction for the green component.
    *
    * @param Gh  horizontally interpolated green image
    * @param Gv  vertically interpolated green image
    * @param bay Bayer pattern image
    * @return (dstG, interpDir) `dstG` is the resulting green image.
    *         `interpDir` is an image where if interpDir(i,j)=1 the pixel (i,j) is the best reconstruction
    *         is the horizontal interpolation. if interpDir(i,j)=2 the best reconstruction is the vertical one,
    *         and if interpDir(i,j)==0 no estimation was performed (for the G positions)
    *         *
    */
  private[debayer2sx] def decision(Gh: FloatProcessor, Gv: FloatProcessor, bay: FloatProcessor): (FloatProcessor, ByteProcessor) = {
    val h = Gh.getHeight
    val w = Gh.getWidth
    val hh = h - 2
    val ww = w - 2

    // Image interpDir takes into account the selected direction for the interpolation
    //   1 --> horizontal reconstruction
    //   2 --> vertical reconstruction
    val dstG = new FloatProcessor(w, h)

    // Copy the original green values available from the Bayer pattern
    // outG(1:2:m,1:2:n)=bay(1:2:m,1:2:n);
    // outG(2:2:m,2:2:n)=bay(2:2:m,2:2:n);
    copyRanges(dstG, Range(1 - 1, w, 2), Range(1 - 1, h, 2), /*=*/ bay, Range(1 - 1, w, 2), Range(1 - 1, h, 2))
    copyRanges(dstG, Range(2 - 1, w, 2), Range(2 - 1, h, 2), /*=*/ bay, Range(2 - 1, w, 2), Range(2 - 1, h, 2))

    // Chrominances of the horizontally reconstructed image
    // chrH=zeros(m,n);
    val chrH = new FloatProcessor(w, h)
    // chrH(1:2:m,2:2:n)=bay(1:2:m,2:2:n)-Gh(1:2:m,2:2:n);
    val b1 = bay(Range(2 - 1, w, 2), Range(1 - 1, h, 2))
    val Gh1 = Gh(Range(2 - 1, w, 2), Range(1 - 1, h, 2))
    copyRanges(chrH, Range(2 - 1, w, 2), Range(1 - 1, h, 2), /*=*/ b1 - Gh1, FR, FR)
    // chrH(2:2:m,1:2:n)=bay(2:2:m,1:2:n)-Gh(2:2:m,1:2:n);
    val b2 = bay(Range(1 - 1, w, 2), Range(2 - 1, h, 2))
    val Gh2 = Gh(Range(1 - 1, w, 2), Range(2 - 1, h, 2))
    copyRanges(chrH, Range(1 - 1, w, 2), Range(2 - 1, h, 2), /*=*/ b2 - Gh2, FR, FR)

    // Chrominances of the vertically reconstructed image
    // chrV=zeros(m,n);
    val chrV = new FloatProcessor(w, h)
    // chrV(1:2:m,2:2:n)=bay(1:2:m,2:2:n)-Gv(1:2:m,2:2:n);
    val Gv1 = Gv(Range(2 - 1, w, 2), Range(1 - 1, h, 2))
    copyRanges(chrV, Range(2 - 1, w, 2), Range(1 - 1, h, 2), /*=*/ b1 - Gv1, FR, FR)
    // chrV(2:2:m,1:2:n)=bay(2:2:m,1:2:n)-Gv(2:2:m,1:2:n);
    val Gv2 = Gv(Range(1 - 1, w, 2), Range(2 - 1, h, 2))
    copyRanges(chrV, Range(1 - 1, w, 2), Range(2 - 1, h, 2), /*=*/ b2 - Gv2, FR, FR)

    // Gradients of chrH and chrV
    val DH = new FloatProcessor(w, h)

    // DH(1:2:m,2:2:nn)=nor(chrH(1:2:m,2:2:nn),chrH(1:2:m,(2:2:nn)+2));
    {
      val v = nor(chrH(Range(2 - 1, ww, 2), Range(1 - 1, h, 2)), chrH(Range(2 - 1, ww, 2) + 2, Range(1 - 1, h, 2)))
      copyRanges(DH, Range(2 - 1, ww, 2), Range(1 - 1, h, 2), /*=*/ v, FR, FR)
    }

    // DH(2:2:m,1:2:nn)=nor(chrH(2:2:m,1:2:nn),chrH(2:2:m,(1:2:nn)+2));
    {
      val v = nor(chrH(Range(1 - 1, ww, 2), Range(2 - 1, h, 2)), chrH(Range(1 - 1, ww, 2) + 2, Range(2 - 1, h, 2)))
      copyRanges(DH, Range(1 - 1, ww, 2), Range(2 - 1, h, 2), /*=*/ v, FR, FR)
    }

    // DH(1:2:m,n)=nor(chrH(1:2:m,n),chrH(1:2:m,n-2));
    {
      val v = nor(chrH(w - 1, Range(1 - 1, h, 2)), chrH(w - 2 - 1, Range(1 - 1, h, 2)))
      copyRanges(DH, Range(w - 1, w), Range(1 - 1, h, 2), v, FR, FR)
    }

    val DV = new FloatProcessor(w, h)

    // DV(1:2:mm,2:2:n)=nor(chrV(1:2:mm,2:2:n),chrV((1:2:mm)+2,2:2:n));
    {
      val v = nor(chrV(Range(2 - 1, w, 2), Range(1 - 1, hh, 2)), chrV(Range(2 - 1, w, 2), Range(1 - 1, hh, 2) + 2))
      copyRanges(DV, Range(2 - 1, w, 2), Range(1 - 1, hh, 2), v, FR, FR)
    }

    // DV(2:2:mm,1:2:n)=nor(chrV(2:2:mm,1:2:n),chrV((2:2:mm)+2,1:2:n));
    {
      val v = nor(chrV(Range(1 - 1, w, 2), Range(2 - 1, hh, 2)), chrV(Range(1 - 1, w, 2), Range(2 - 1, hh, 2) + 2))
      copyRanges(DV, Range(1 - 1, w, 2), Range(2 - 1, hh, 2), v, FR, FR)
    }

    // DV(m,1:2:n)=nor(chrV(m,1:2:n,:),chrV(m-2,1:2:n));
    {
      val v = nor(chrV(Range(1 - 1, w, 2), h - 1), chrV(Range(1 - 1, w, 2), h - 2 - 1))
      copyRanges(DV, Range(1 - 1, w, 2), Range(h - 1, h), v, FR, FR)
    }

    // Weight
    val c = 3

    // Compute DeltaH and DeltaV
    // DeltaH=zeros(m,n);
    // DeltaV=zeros(m,n);
    val DeltaH = new FloatProcessor(w, h)
    val DeltaV = new FloatProcessor(w, h)

    // i=3:2:mm;
    // j=4:2:nn;
    // DeltaH(i,j)=DH(i-2,j-2)+DH(i-2,j)+c*DH(i,j-2)+c*DH(i,j)+DH(i+2,j-2)+DH(i+2,j)+DH(i-1,j-1)+DH(i+1,j-1);
    // DeltaV(i,j)=DV(i-2,j-2)+c*DV(i-2,j)+DV(i-2,j+2)+DV(i,j-2)+c*DV(i,j)+DV(i,j+2)+DV(i-1,j-1)+DV(i-1,j+1);
    {
      val y = Range(3 - 1, hh, 2)
      val x = Range(4 - 1, ww, 2)
      val vh = DH(x - 2, y - 2) + DH(x, y - 2) + DH(x - 2, y) * c + DH(x, y) * c + DH(x - 2, y + 2) + DH(x, y + 2) + DH(x - 1, y - 1) + DH(x - 1, y + 1)
      copyRanges(DeltaH, x, y, vh, FR, FR)
      val vv = DV(x - 2, y - 2) + DV(x, y - 2) * c + DV(x + 2, y - 2) + DV(x - 2, y) + DV(x, y) * c + DV(x + 2, y) + DV(x - 1, y - 1) + DV(x + 1, y - 1)
      copyRanges(DeltaV, x, y, vv, FR, FR)
    }

    // i=4:2:mm;
    // j=3:2:nn;
    // DeltaH(i,j)=DH(i-2,j-2)+DH(i-2,j)+c*DH(i,j-2)+c*DH(i,j)+DH(i+2,j-2)+DH(i+2,j)+DH(i-1,j-1)+DH(i+1,j-1);
    // DeltaV(i,j)=DV(i-2,j-2)+c*DV(i-2,j)+DV(i-2,j+2)+DV(i,j-2)+c*DV(i,j)+DV(i,j+2)+DV(i-1,j-1)+DV(i-1,j+1);
    {
      val y = Range(4 - 1, hh, 2)
      val x = Range(3 - 1, ww, 2)
      val vh = DH(x - 2, y - 2) + DH(x, y - 2) + DH(x - 2, y) * c + DH(x, y) * c + DH(x - 2, y + 2) + DH(x, y + 2) + DH(x - 1, y - 1) + DH(x - 1, y + 1)
      copyRanges(DeltaH, x, y, vh, FR, FR)
      val vv = DV(x - 2, y - 2) + DV(x, y - 2) * c + DV(x + 2, y - 2) + DV(x - 2, y) + DV(x, y) * c + DV(x + 2, y) + DV(x - 1, y - 1) + DV(x + 1, y - 1)
      copyRanges(DeltaV, x, y, vv, FR, FR)
    }

    // Compute DeltaH and Delta V near the border of the image
    // i=2;
    // j=3:2:nn;
    // DeltaH(i,j)=DH(i+2,j-2)+DH(i+2,j)+c*DH(i,j-2)+c*DH(i,j)+DH(i+2,j-2)+DH(i+2,j)+DH(i-1,j-1)+DH(i+1,j-1);
    // DeltaV(i,j)=DV(i+2,j-2)+c*DV(i+2,j)+DV(i+2,j+2)+DV(i,j-2)+c*DV(i,j)+DV(i,j+2)+DV(i-1,j-1)+DV(i-1,j+1);
    {
      val y = Range(2 - 1, 2)
      val x = Range(3 - 1, ww, 2)
      val vh = DH(x - 2, y + 2) + DH(x, y + 2) + DH(x - 2, y) * c + DH(x, y) * c + DH(x - 2, y + 2) + DH(x, y + 2) + DH(x - 1, y - 1) + DH(x - 1, y + 1)
      copyRanges(DeltaH, x, y, vh, FR, FR)
      val vv = DV(x - 2, y + 2) + DV(x, y + 2) * c + DV(x + 2, y + 2) + DV(x - 2, y) + DV(x, y) * c + DV(x + 2, y) + DV(x - 1, y - 1) + DV(x + 1, y - 1)
      copyRanges(DeltaV, x, y, vv, FR, FR)
    }

    // i=m-1;
    // j=4:2:nn;
    // DeltaH(i,j)=DH(i-2,j-2)+DH(i-2,j)+c*DH(i,j-2)+c*DH(i,j)+DH(i-2,j-2)+DH(i-2,j)+DH(i-1,j-1)+DH(i+1,j-1);
    // DeltaV(i,j)=DV(i-2,j-2)+c*DV(i-2,j)+DV(i-2,j+2)+DV(i,j-2)+c*DV(i,j)+DV(i,j+2)+DV(i-1,j-1)+DV(i-1,j+1);
    {
      val y = Range(h - 1 - 1, h - 1)
      val x = Range(4 - 1, ww, 2)
      val vh = DH(x - 2, y - 2) + DH(x, y - 2) + DH(x - 2, y) * c + DH(x, y) * c + DH(x - 2, y - 2) + DH(x, y - 2) + DH(x - 1, y - 1) + DH(x - 1, y + 1)
      copyRanges(DeltaH, x, y, vh, FR, FR)
      val vv = DV(x - 2, y - 2) + DV(x, y - 2) * c + DV(x + 2, y - 2) + DV(x - 2, y) + DV(x, y) * c + DV(x + 2, y) + DV(x - 1, y - 1) + DV(x + 1, y - 1)
      copyRanges(DeltaV, x, y, vv, FR, FR)
    }

    // j=2;
    // i=3:2:mm;
    // DeltaH(i,j)=DH(i-2,j+2)+DH(i-2,j)+c*DH(i,j+2)+c*DH(i,j)+DH(i+2,j+2)+DH(i+2,j)+DH(i-1,j-1)+DH(i+1,j-1);
    // DeltaV(i,j)=DV(i-2,j+2)+c*DV(i-2,j)+DV(i-2,j+2)+DV(i,j+2)+c*DV(i,j)+DV(i,j+2)+DV(i-1,j-1)+DV(i-1,j+1);
    {
      val y = Range(2 - 1, 2)
      val x = Range(3 - 1, hh, 2)
      val vh = DH(y + 2, x - 2) + DH(y, x - 2) + DH(y + 2, x) * c + DH(y, x) * c + DH(y + 2, x + 2) + DH(y, x + 2) + DH(y - 1, x - 1) + DH(y - 1, x + 1)
      copyRanges(DeltaH, y, x, vh, FR, FR)
      val vv = DV(y + 2, x - 2) + DV(y, x - 2) * c + DV(y + 2, x - 2) + DV(y + 2, x) + DV(y, x) * c + DV(y + 2, x) + DV(y - 1, x - 1) + DV(y + 1, x - 1)
      copyRanges(DeltaV, y, x, vv, FR, FR)
    }

    // j=n-1;
    // i=4:2:mm;
    // DeltaH(i,j)=DH(i-2,j-2)+DH(i-2,j)+c*DH(i,j-2)+c*DH(i,j)+DH(i+2,j-2)+DH(i+2,j)+DH(i-1,j-1)+DH(i+1,j-1);
    // DeltaV(i,j)=DV(i-2,j-2)+c*DV(i-2,j)+DV(i-2,j-2)+DV(i,j-2)+c*DV(i,j)+DV(i,j-2)+DV(i-1,j-1)+DV(i-1,j+1);
    {
      val y = Range(w - 1 - 1, w - 1)
      val x = Range(4 - 1, hh, 2)
      val vh = DH(y - 2, x - 2) + DH(y, x - 2) + DH(y - 2, x) * c + DH(y, x) * c + DH(y - 2, x + 2) + DH(y, x + 2) + DH(y - 1, x - 1) + DH(y - 1, x + 1)
      copyRanges(DeltaH, y, x, vh, FR, FR)
      val vv = DV(y - 2, x - 2) + DV(y, x - 2) * c + DV(y - 2, x - 2) + DV(y - 2, x) + DV(y, x) * c + DV(y - 2, x) + DV(y - 1, x - 1) + DV(y + 1, x - 1)
      copyRanges(DeltaV, y, x, vv, FR, FR)
    }

    // Decision between the horizontal and vertical interpolation and reconstruction of the green component
    val interpDir = new ByteProcessor(w, h)

    // x=find(DeltaV<DeltaH);
    // outG(x)=Gv(x);
    // interpDir(x)=2;
    for (i <- 0 until dstG.getPixelCount) {
      if (DeltaV.getf(i) < DeltaH.getf(i)) {
        dstG.setf(i, Gv.getf(i))
        interpDir.set(i, 2)
      }
    }

    // x=find(DeltaV>=DeltaH);
    // outG(x)=Gh(x);
    // interpDir(x)=1;
    for (i <- 0 until dstG.getPixelCount) {
      if (DeltaV.getf(i) >= DeltaH.getf(i)) {
        dstG.setf(i, Gh.getf(i))
        interpDir.set(i, 1)
      }
    }


    // Reconstruction near the border of the image
    // outG(1,:)=Gh(1,:);
    copyRanges(dstG, FR, Range(0, 1), Gh, FR, Range(0, 1))
    // outG(m,:)=Gh(m,:);
    copyRanges(dstG, FR, Range(h - 1, h), Gh, FR, Range(h - 1, h))
    // outG(:,1)=Gv(:,1);
    copyRanges(dstG, Range(0, 1), FR, Gv, Range(0, 1), FR)
    // outG(:,n)=Gv(:,n);
    copyRanges(dstG, Range(w - 1, w), FR, Gv, Range(w - 1, w), FR)

    // outG(2,2)=Gh(2,2);
    copyRanges(dstG, Range(1, 2), Range(1, 2), Gh, Range(1, 2), Range(1, 2))
    // outG(m-1,2)=Gh(m-1,2);
    copyRanges(dstG, Range(1, 2), Range(h - 1 - 1, h - 1), Gh, Range(1, 2), Range(h - 1 - 1, h - 1))
    // outG(2,n-1)=Gh(2,n-1);
    copyRanges(dstG, Range(w - 1 - 1, w - 1), Range(1, 2), Gh, Range(w - 1 - 1, w - 1), Range(1, 2))
    // outG(m-1,n-1)=Gh(m-1,n-1);
    copyRanges(dstG, Range(w - 1 - 1, w - 1), Range(h - 1 - 1, h - 1), Gh, Range(w - 1 - 1, w - 1), Range(h - 1 - 1, h - 1))

    (dstG, interpDir)
  }

  /**
    * Reconstruction of the red and blue components.
    *
    * Reconstructs the red and blue components, `R` and `B` respectively, using the bayer data `bay`,
    * the reconstructed green image `G`, and the knowledge of the green decision contained in the matrix `interpDir`.
    *
    * @param bay       Bayer sampled image
    * @param G         reconstructed green channel
    * @param interpDir best interpolation direction
    * @return Reconstructed channels red and blue.
    */
  private[debayer2sx] def interpRB(bay: FloatProcessor, G: FloatProcessor, interpDir: ByteProcessor): (FloatProcessor, FloatProcessor) = {
    val h = G.getHeight
    val w = G.getWidth

    // R=zeros(m,n);
    val R = new FloatProcessor(w, h)
    // B=zeros(m,n);
    val B = new FloatProcessor(w, h)
    // R(1:2:m,2:2:n)=bay(1:2:m,2:2:n);
    // B(2:2:m,1:2:n)=bay(2:2:m,1:2:n);
    copyRanges(R, Range(2 - 1, w, 2), Range(1 - 1, h, 2), /*=*/ bay, Range(2 - 1, w, 2), Range(1 - 1, h, 2))
    copyRanges(B, Range(1 - 1, w, 2), Range(2 - 1, h, 2), /*=*/ bay, Range(1 - 1, w, 2), Range(2 - 1, h, 2))

    // Green pixels: reconstruction of the red through bilinear interpolation of R-G
    // R(1:2:m,3:2:n)=G(1:2:m,3:2:n)+(R(1:2:m,(3:2:n)-1)-G(1:2:m,(3:2:n)-1)+R(1:2:m,(3:2:n)+1)-G(1:2:m,(3:2:n)+1))/2;
    {
      val v1 = G(Range(3 - 1, w, 2), Range(1 - 1, h, 2))
      val v2 = R(Range(3 - 1, w, 2) - 1, Range(1 - 1, h, 2))
      val v3 = G(Range(3 - 1, w, 2) - 1, Range(1 - 1, h, 2))
      val v4 = R(Range(3 - 1, w, 2) + 1, Range(1 - 1, h, 2))
      val v5 = G(Range(3 - 1, w, 2) + 1, Range(1 - 1, h, 2))
      val v6 = v1 + (v2 - v3 + v4 - v5) / 2
      copyRanges(R, Range(3 - 1, w, 2), Range(1 - 1, h, 2), /*=*/ v6, FR, FR)
    }

    // R(2:2:m-1,2:2:n)=G(2:2:m-1,2:2:n)+(R((2:2:m-1)-1,2:2:n)-G((2:2:m-1)-1,2:2:n)+R((2:2:m-1)+1,2:2:n)-G((2:2:m-1)+1,2:2:n))/2;
    {
      val v1 = G(Range(2 - 1, w, 2), Range(2 - 1, h - 1, 2))
      val v2 = R(Range(2 - 1, w, 2), Range(1 - 1, h - 2, 2))
      val v3 = G(Range(2 - 1, w, 2), Range(1 - 1, h - 2, 2))
      val v4 = R(Range(2 - 1, w, 2), Range(3 - 1, h, 2))
      val v5 = G(Range(2 - 1, w, 2), Range(3 - 1, h, 2))
      val v6 = v1 + (v2 - v3 + v4 - v5) / 2
      copyRanges(R, Range(2 - 1, w, 2), Range(2 - 1, h - 1, 2), /*=*/ v6, FR, FR)
    }

    // Green pixels: reconstruction of the blue through bilinear interpolation of B-G
    // B(2:2:m,2:2:n-1)=G(2:2:m,2:2:n-1)+(B(2:2:m,(2:2:n-1)-1)-G(2:2:m,(2:2:n-1)-1)+B(2:2:m,(2:2:n-1)+1)-G(2:2:m,(2:2:n-1)+1))/2;
    {
      val v1 = G(Range(2 - 1, w - 1, 2), Range(2 - 1, h, 2))
      val v2 = B(Range(2 - 1, w - 1, 2) - 1, Range(2 - 1, h, 2))
      val v3 = G(Range(2 - 1, w - 1, 2) - 1, Range(2 - 1, h, 2))
      val v4 = B(Range(2 - 1, w - 1, 2) + 1, Range(2 - 1, h, 2))
      val v5 = G(Range(2 - 1, w - 1, 2) + 1, Range(2 - 1, h, 2))
      val v6 = v1 + (v2 - v3 + v4 - v5) / 2
      copyRanges(B, Range(2 - 1, w - 1, 2), Range(2 - 1, h, 2), /*=*/ v6, FR, FR)
    }

    // B(3:2:m,1:2:n)=G(3:2:m,1:2:n)+(B((3:2:m)-1,1:2:n)-G((3:2:m)-1,1:2:n)+B((3:2:m)+1,1:2:n)-G((3:2:m)+1,1:2:n))/2;
    {
      val v1 = G(Range(1 - 1, w, 2), Range(3 - 1, h, 2))
      val v2 = B(Range(1 - 1, w, 2), Range(3 - 1, h, 2) - 1)
      val v3 = G(Range(1 - 1, w, 2), Range(3 - 1, h, 2) - 1)
      val v4 = B(Range(1 - 1, w, 2), Range(3 - 1, h, 2) + 1)
      val v5 = G(Range(1 - 1, w, 2), Range(3 - 1, h, 2) + 1)
      val v6 = v1 + (v2 - v3 + v4 - v5) / 2
      copyRanges(B, Range(1 - 1, w, 2), Range(3 - 1, h, 2), /*=*/ v6, FR, FR)
    }

    // Reconstruction near the borders of the image
    // R(:,1)=G(:,1)+R(:,2)-G(:,2);
    {
      val v1 = G(0, FR)
      val v2 = R(1, FR)
      val v3 = G(1, FR)
      val v4 = v1 + v2 - v3
      copyRanges(R, Range(0, 1), FR, /*=*/ v4, FR, FR)
    }
    // R(m,:)=G(m,:)+R(m-1,:)-G(m-1,:);
    {
      val v1 = G(FR, h - 1)
      val v2 = R(FR, h - 1 - 1)
      val v3 = G(FR, h - 1 - 1)
      val v4 = v1 + v2 - v3
      copyRanges(R, FR, Range(h - 1, h), /*=*/ v4, FR, FR)
    }
    // B(1,:)=G(1,:)+B(2,:)-G(2,:);
    {
      val v1 = G(FR, 0)
      val v2 = B(FR, 1)
      val v3 = G(FR, 1)
      val v4 = v1 + v2 - v3
      copyRanges(B, FR, Range(0, 1), /*=*/ v4, FR, FR)
    }
    // B(:,n)=G(:,n)+B(:,n-1)-G(:,n-1);
    {
      val v1 = G(w - 1, FR)
      val v2 = B(w - 2, FR)
      val v3 = G(w - 2, FR)
      val v4 = v1 + v2 - v3
      copyRanges(B, Range(w - 1, w), FR, /*=*/ v4, FR, FR)
    }

    // Reconstruction of the red and blue values in the blue and red pixels, respectively.
    // for i=2:2:m-1,
    //   for j=3:2:n-1,
    //     if interpDir(i,j)==1,
    //       R(i,j) = B(i,j)+1/2*(R(i,j-1)-B(i,j-1)+R(i,j+1)-B(i,j+1));
    //     else
    //       R(i,j) = B(i,j)+1/2*(R(i-1,j)-B(i-1,j)+R(i+1,j)-B(i+1,j));
    //     end
    //   end
    // end
    for (y <- Range(1, h - 1, 2)) {
      for (x <- Range(2, w - 1, 2)) {
        if (interpDir.get(x, y) == 1) {
          R.setf(x, y, B.getf(x, y) + 1 / 2f * (R.getf(x - 1, y) - B.getf(x - 1, y) + R.getf(x + 1, y) - B.getf(x + 1, y)))
        } else {
          val v = B.getf(x, y) + 1 / 2f * (R.getf(x, y - 1) - B.getf(x, y - 1) + R.getf(x, y + 1) - B.getf(x, y + 1))
          R.setf(x, y, v)
        }
      }
    }


    // for i=3:2:m-1,
    //   for j=2:2:n-2,
    //     if interpDir(i,j)==1,
    //       B(i,j) = R(i,j)+1/2*(B(i,j-1)-R(i,j-1)+B(i,j+1)-R(i,j+1));
    //     else
    //       B(i,j) = R(i,j)+1/2*(B(i-1,j)-R(i-1,j)+B(i+1,j)-R(i+1,j));
    //     end
    //   end
    // end
    for (y <- Range(2, h - 1, 2)) {
      for (x <- Range(1, w - 2, 2)) {
        if (interpDir.get(x, y) == 1) {
          B.setf(x, y, R.getf(x, y) + 1 / 2f * (B.getf(x - 1, y) - R.getf(x - 1, y) + B.getf(x + 1, y) - R.getf(x + 1, y)))
        } else {
          B.setf(x, y, R.getf(x, y) + 1 / 2f * (B.getf(x, y - 1) - R.getf(x, y - 1) + B.getf(x, y + 1) - R.getf(x, y + 1)))
        }
      }
    }

    (R, B)
  }

  /**
    * Refining of the reconstructed image.
    *
    * Refines the reconstruction of the interpolated image represented by three bands (R, G, B).
    * The knowledge of the edge-direction estimation contained in interpDir is also used.
    *
    * @param R         red band of the reconstructed image
    * @param G         green band of the reconstructed image
    * @param B         blue band of the reconstructed image
    * @param interpDir optimal interpolation direction
    * @return bands of the reconstructed image
    */
  def refining(R: FloatProcessor, G: FloatProcessor, B: FloatProcessor, interpDir: ByteProcessor): (FloatProcessor, FloatProcessor, FloatProcessor) = {
    val h = R.getHeight
    val w = R.getWidth

    // medRlessG=zeros(m,n);
    // medBlessG=zeros(m,n);
    // medRlessB=zeros(m,n);
    val medRlessG = new FloatProcessor(w, h)
    val medBlessG = new FloatProcessor(w, h)
    val medRlessB = new FloatProcessor(w, h)


    // ff=[1 1 1]/3;
    val ff = 1 / 3f

    {
      // RlessG=R-G;
      // BlessG=B-G;
      val RlessG = R - G
      val BlessG = B - G


      // Refining of the green
      // for i=2:2:m-1
      //     for j=3:2:n-1
      //         if interpDir(i,j)==1
      //             medBlessG(i,j)=ff*BlessG(i,j-1:j+1).';
      //         else
      //             medBlessG(i,j)=ff*BlessG(i-1:i+1,j);
      //         end
      //     end
      // end
      for (y <- 2 - 1 until h - 1 by 2) {
        for (x <- 3 - 1 until w - 1 by 2) {
          val v = if (interpDir.get(x, y) == 1) {
            // medBlessG(i,j)=ff*BlessG(i,j-1:j+1).';
            ff * (BlessG.getf(x - 1, y) + BlessG.getf(x, y) + BlessG.getf(x + 1, y))
          } else {
            // medBlessG(i,j)=ff*BlessG(i-1:i+1,j);
            ff * (BlessG.getf(x, y - 1) + BlessG.getf(x, y) + BlessG.getf(x, y + 1))
          }
          medBlessG.setf(x, y, v)
        }
      }
      // for i=3:2:m-1
      //     for j=2:2:n-1
      //         if interpDir(i,j)==1
      //             medRlessG(i,j)=ff*RlessG(i,j-1:j+1).';
      //         else
      //             medRlessG(i,j)=ff*RlessG(i-1:i+1,j);
      //         end
      //     end
      // end
      for (y <- 3 - 1 until h - 1 by 2) {
        for (x <- 2 - 1 until w - 1 by 2) {
          val v = if (interpDir.get(x, y) == 1) {
            // medRlessG(i,j)=ff*RlessG(i,j-1:j+1).';
            ff * (RlessG.getf(x - 1, y) + RlessG.getf(x, y) + RlessG.getf(x + 1, y))
          } else {
            // medRlessG(i,j)=ff*RlessG(i-1:i+1,j);
            ff * (RlessG.getf(x, y - 1) + RlessG.getf(x, y) + RlessG.getf(x, y + 1))
          }
          medRlessG.setf(x, y, v)
        }
      }

      // G(3:2:m-1,2:2:n-1)=R(3:2:m-1,2:2:n-1)-medRlessG(3:2:m-1,2:2:n-1);
      {
        val v = R(Range(2 - 1, w - 1, 2), Range(3 - 1, h - 1, 2)) - medRlessG(Range(2 - 1, w - 1, 2), Range(3 - 1, h - 1, 2))
        copyRanges(G, Range(2 - 1, w - 1, 2), Range(3 - 1, h - 1, 2), v, FR, FR)
      }

      // G(2:2:m-1,3:2:n-1)=B(2:2:m-1,3:2:n-1)-medBlessG(2:2:m-1,3:2:n-1);
      {
        val v = B(Range(3 - 1, w - 1, 2), Range(2 - 1, h - 1, 2)) - medBlessG(Range(3 - 1, w - 1, 2), Range(2 - 1, h - 1, 2))
        copyRanges(G, Range(3 - 1, w - 1, 2), Range(2 - 1, h - 1, 2), v, FR, FR)
      }
    }

    // RlessG=R-G;
    // BlessG=B-G;
    val RlessG = R - G
    val BlessG = B - G

    // Refining of the red in the green pixels
    {
      // i=2:2:m-1;
      // j=2:2:n-1;
      val y = Range(2 - 1, h - 1, 2)
      val x = Range(2 - 1, w - 1, 2)
      // medRlessG(i,j)=(RlessG(i-1,j)+RlessG(i+1,j))/2;
      copyRanges(medRlessG, x, y, (RlessG(x, y - 1) + RlessG(x, y + 1)) / 2, FR, FR)

      // R(i,j)=G(i,j)+medRlessG(i,j);
      copyRanges(R, x, y, G(x, y) + medRlessG(x, y), FR, FR)
    }

    {
      // i=3:2:m-1;
      // j=3:2:n-1;
      val y = Range(3 - 1, h - 1, 2)
      val x = Range(3 - 1, w - 1, 2)

      // medRlessG(i,j)=(RlessG(i,j-1)+RlessG(i,j+1))/2;
      copyRanges(medRlessG, x, y, (RlessG(x - 1, y) + RlessG(x + 1, y)) / 2, FR, FR)
      // R(i,j)=G(i,j)+medRlessG(i,j);
      copyRanges(R, x, y, G(x, y) + medRlessG(x, y), FR, FR)
    }

    // Refining of the blue in the green pixels
    {
      // i=2:2:m-1;
      // j=2:2:n-1;
      val y = Range(2 - 1, h - 1, 2)
      val x = Range(2 - 1, w - 1, 2)
      //    * medBlessG(i,j)=(BlessG(i,j-1)+BlessG(i,j+1))/2;
      copyRanges(medBlessG, x, y, (BlessG(x - 1, y) + BlessG(x + 1, y)) / 2, FR, FR)
      //    * B(i,j)=G(i,j)+medBlessG(i,j);
      copyRanges(B, x, y, G(x, y) + medBlessG(x, y), FR, FR)
    }

    {
      // i=3:2:m-1;
      // j=3:2:n-1;
      val y = Range(3 - 1, h - 1, 2)
      val x = Range(3 - 1, w - 1, 2)

      // medBlessG(i,j)=(BlessG(i-1,j)+BlessG(i+1,j))/2;
      copyRanges(medBlessG, x, y, (BlessG(x, y - 1) + BlessG(x, y + 1)) / 2, FR, FR)
      // B(i,j)=G(i,j)+medBlessG(i,j);
      copyRanges(B, x, y, G(x, y) + medBlessG(x, y), FR, FR)
    }

    // RlessB=R-B;
    val RlessB = R - B

    // Refining of the red in the blue pixels
    // for i=2:2:m-1,
    //     for j=3:2:n-1,
    //         if interpDir(i,j)==1
    //             medRlessB(i,j)=ff*RlessB(i,j-1:j+1).';
    //         else
    //             medRlessB(i,j)=ff*RlessB(i-1:i+1,j);
    //         end
    //         R(i,j)=B(i,j)+medRlessB(i,j);
    //      end
    // end
    for (y <- 2 - 1 until h - 1 by 2) {
      for (x <- 3 - 1 until w - 1 by 2) {
        val v = if (interpDir.get(x, y) == 1) {
          // medRlessB(i,j)=ff*RlessB(i,j-1:j+1).';
          ff * (RlessB.getf(x - 1, y) + RlessB.getf(x, y) + RlessB.getf(x + 1, y))
        } else {
          // medRlessB(i,j)=ff*RlessB(i-1:i+1,j);
          ff * (RlessB.getf(x, y - 1) + RlessB.getf(x, y) + RlessB.getf(x, y + 1))
        }
        medRlessB.setf(x, y, v)

        // R(i,j)=B(i,j)+medRlessB(i,j);
        R.setf(x, y, B.getf(x, y) + medRlessB.getf(x, y))
      }
    }


    //  Refining of the blue in the red pixels
    //  for i=3:2:m-1,
    //      for j=2:2:n-2,
    //          if interpDir(i,j)==1
    //             medRlessB(i,j)=ff*RlessB(i,j-1:j+1).';
    //          else
    //             medRlessB(i,j)=ff*RlessB(i-1:i+1,j);
    //          end
    //          B(i,j)=R(i,j)-medRlessB(i,j);
    //       end
    //  end
    for (y <- 3 - 1 until h - 1 by 2) {
      for (x <- 2 - 1 until w - 2 by 2) {
        val v = if (interpDir.get(x, y) == 1) {
          // medRlessB(i,j)=ff*RlessB(i,j-1:j+1).';
          ff * (RlessB.getf(x - 1, y) + RlessB.getf(x, y) + RlessB.getf(x + 1, y))
        } else {
          // medRlessB(i,j)=ff*RlessB(i-1:i+1,j);
          ff * (RlessB.getf(x, y - 1) + RlessB.getf(x, y) + RlessB.getf(x, y + 1))
        }
        medRlessB.setf(x, y, v)

        // B(i,j)=R(i,j)-medRlessB(i,j);
        B.setf(x, y, R.getf(x, y) - medRlessB.getf(x, y))
      }
    }

    (R, G, B)
  }

  private def mirrorBorderWidth(x: FloatProcessor, b: Int): FloatProcessor = {
    val w = x.getWidth
    val h = x.getHeight

    // xx = [x(:,1+B:-1:2), x, x(:,n-1:-1:n-B)];
    val xx1 = x(Range(1 + b - 1, 2 - 2, -1), FR)
    val xx2 = x(Range(w - 1 - 1, w - b - 2, -1), FR)
    val xx = new FloatProcessor(b + w + b, h)
    val blitter = new FloatBlitter(xx)
    blitter.copyBits(xx1, 0, 0, Blitter.COPY)
    blitter.copyBits(x, b, 0, Blitter.COPY)
    blitter.copyBits(xx2, b + w, 0, Blitter.COPY)

    xx
  }

  private def mirrorBorderHeight(x: FloatProcessor, b: Int): FloatProcessor = {
    val w = x.getWidth
    val h = x.getHeight

    // xx = [x(1+B:-1:2,:); x; x(m-1:-1:m-B,:)];
    val xx1 = x(FR, Range(1 + b - 1, 2 - 2, -1))
    val xx2 = x(FR, Range(h - 1 - 1, h - b - 2, -1))
    val xx = new FloatProcessor(w, b + h + b)
    val blitter = new FloatBlitter(xx)
    blitter.copyBits(xx1, 0, 0, Blitter.COPY)
    blitter.copyBits(x, 0, b, Blitter.COPY)
    blitter.copyBits(xx2, 0, b + h, Blitter.COPY)

    xx
  }

}
