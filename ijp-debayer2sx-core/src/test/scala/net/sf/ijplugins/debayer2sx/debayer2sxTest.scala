/*
 * Image/J Plugins
 * Copyright (C) 2002-2018 Jarek Sacha
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

import ij.IJ
import ij.process.{ByteProcessor, ColorProcessor}
import org.scalatest.{FlatSpec, Matchers}

class debayer2sxTest extends FlatSpec with Matchers {

  behavior of "package debayer2sx"

  it should "compute `bayerRG`" in {
    // Read image to process
    val imp = IJ.openImage("../data/Lighthouse.png")
    assert(imp != null)
    val cp = imp.getProcessor.asInstanceOf[ColorProcessor]
    assert(cp != null)

    // Process
    val bay = bayerGR(cp)
    assert(bay != null)

    // Read expected result
    val impRef = IJ.openImage("../data/Lighthouse_bayerGR8.png")
    assert(imp != null)
    val bpRef = impRef.getProcessor.asInstanceOf[ByteProcessor]
    assert(bpRef != null)

    compare(bay, bpRef)
  }

  it should "compute `debayerCopyGR`" in {
    // Read image to process
    val imp = IJ.openImage("../data/Lighthouse_bayerGR8.png")
    assert(imp != null)
    val bay = imp.getProcessor.asInstanceOf[ByteProcessor]
    assert(bay != null)

    // Process
    val cp = debayerCopyGR(bay)
    assert(cp != null)

    // Read expected result
    val impRef = IJ.openImage("../data/Lighthouse_bayerGR8_expand_bands.png")
    assert(imp != null)
    val cpRef = impRef.getProcessor.asInstanceOf[ColorProcessor]
    assert(cpRef != null)

    compare(cp, cpRef)
  }


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
}
