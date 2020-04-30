/*
 * Image/J Plugins
 * Copyright (C) 2002-2020 Jarek Sacha
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

import ij.IJ
import ij.process.{ByteProcessor, ColorProcessor}
import net.sf.ijplugins.debayer2sx.DeBayer2Config.MosaicOrder
import net.sf.ijplugins.debayer2sx.Utils.compare
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class debayer2sxTest extends AnyFlatSpec with Matchers {

  behavior of "package debayer2sx"

  it should "compute `bayerRG`" in {
    // Read image to process
    val imp = IJ.openImage("../data/Lighthouse.png")
    assert(imp != null)
    val cp = imp.getProcessor.asInstanceOf[ColorProcessor]
    assert(cp != null)

    // Process
    val bay = MakeBayer.process(cp, MosaicOrder.G_R)
    assert(bay != null)

    // Read expected result
    val impRef = IJ.openImage("../data/Lighthouse_bayerGR8.png")
    assert(impRef != null)
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
    assert(impRef != null)
    val cpRef = impRef.getProcessor.asInstanceOf[ColorProcessor]
    assert(cpRef != null)

    compare(cp, cpRef)
  }
}
