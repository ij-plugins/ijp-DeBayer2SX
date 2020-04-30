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
 * Latest release available at https://github.com/ij-plugins/ijp-DeBayer2SX
 */

package net.sf.ijplugins.debayer2sx

import java.io.File

import ij.process.{Blitter, ByteProcessor, ColorBlitter, ColorProcessor}
import ij.{IJ, ImagePlus}
import org.scalactic.Requirements._

object Utils {

  def compare(im1: ByteProcessor, im2: ByteProcessor): Unit = {
    val w = im1.getWidth
    val h = im1.getHeight

    assert(w == im2.getWidth, "Width should match")
    assert(h == im2.getHeight, "Height should match")

    for (y <- Range(0, h); x <- Range(0, w)) {
      assert(im1.get(x, y) == im2.get(x, y), s"Location ($x, $y)")
    }
  }

  def compare(im1: ColorProcessor, im2: ColorProcessor): Unit = {
    val w = im1.getWidth
    val h = im1.getHeight

    assert(w == im2.getWidth, "Width should match")
    assert(h == im2.getHeight, "Height should match")

    for (y <- Range(0, h); x <- Range(0, w)) {
      assert(im1.getColor(x, y).getRed == im2.getColor(x, y).getRed, s"Red Location ($x, $y)")
      assert(im1.getColor(x, y).getGreen == im2.getColor(x, y).getGreen, s"Green Location ($x, $y)")
      assert(im1.getColor(x, y).getBlue == im2.getColor(x, y).getBlue, s"Blue Location ($x, $y)")
    }
  }

  def openColorProcessor(path: String): ColorProcessor = {
    require(new File(path).exists())
    val imp = IJ.openImage(path)
    require(imp != null)
    require(imp.getStackSize == 1)
    require(imp.getType == ImagePlus.COLOR_RGB)

    val ip = imp.getProcessor
    require(ip.isInstanceOf[ColorProcessor])

    ip.asInstanceOf[ColorProcessor]
  }

  def openByteProcessor(path: String): ByteProcessor = {
    require(new File(path).exists())
    val imp = IJ.openImage(path)
    require(imp != null)
    require(imp.getStackSize == 1)
    require(imp.getType == ImagePlus.GRAY8)

    val ip = imp.getProcessor
    require(ip.isInstanceOf[ByteProcessor])

    ip.asInstanceOf[ByteProcessor]
  }

  def meanDistance(cp1: ColorProcessor, cp2: ColorProcessor): Double = {
    require(cp1 != null)
    require(cp2 != null)
    require(cp1.getWidth == cp2.getWidth)
    require(cp1.getHeight == cp2.getHeight)

    val cp = cp1.duplicate().asInstanceOf[ColorProcessor]
    new ColorBlitter(cp).copyBits(cp2, 0, 0, Blitter.DIFFERENCE)
    cp.getStatistics.mean
  }

}
