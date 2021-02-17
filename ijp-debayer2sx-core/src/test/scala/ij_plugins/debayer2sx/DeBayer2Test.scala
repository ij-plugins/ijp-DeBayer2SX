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

import ij.process.ColorProcessor
import ij_plugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}
import ij_plugins.debayer2sx.Utils._
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DeBayer2Test extends AnyFlatSpec with Matchers with BeforeAndAfter {

  private val meanDistanceTolerance = 1.5
  private var refCP: ColorProcessor = _

  behavior of "DeBayer2"

  before {
    refCP = openColorProcessor("../data/Lighthouse.png")
  }


  it should "DDFAPDRefined BG" in {
    val bayBP = openByteProcessor("../data/Lighthouse_bayerBG8.png")
    val (dstStack, bpp) = DeBayer2.process(bayBP, DeBayer2Config(MosaicOrder.B_G, Demosaicing.DDFAPDRefined))
    val cp = DeBayer2.stackToColorProcessor(dstStack, bpp)
    assert(meanDistance(cp, refCP) <= meanDistanceTolerance)
  }

  it should "DDFAPDRefined GB" in {
    val bayBP = openByteProcessor("../data/Lighthouse_bayerGB8.png")
    val (dstStack, bpp) = DeBayer2.process(bayBP, DeBayer2Config(MosaicOrder.G_B, Demosaicing.DDFAPDRefined))
    val cp = DeBayer2.stackToColorProcessor(dstStack, bpp)
    assert(meanDistance(cp, refCP) <= meanDistanceTolerance)
  }

  it should "DDFAPDRefined GR" in {
    val bayBP = openByteProcessor("../data/Lighthouse_bayerGR8.png")
    val (dstStack, bpp) = DeBayer2.process(bayBP, DeBayer2Config(MosaicOrder.G_R, Demosaicing.DDFAPDRefined))
    val cp = DeBayer2.stackToColorProcessor(dstStack, bpp)
    assert(meanDistance(cp, refCP) <= meanDistanceTolerance)
  }


  it should "DDFAPDRefined RG" in {
    val bayBP = openByteProcessor("../data/Lighthouse_bayerRG8.png")
    val (dstStack, bpp) = DeBayer2.process(bayBP, DeBayer2Config(MosaicOrder.R_G, Demosaicing.DDFAPDRefined))
    val cp = DeBayer2.stackToColorProcessor(dstStack, bpp)
    assert(meanDistance(cp, refCP) <= meanDistanceTolerance)
  }

  it should "AdaptiveSmoothHue RG" in {
    val bayBP = openByteProcessor("../data/Lighthouse_bayerRG8.png")
    val (dstStack, bpp) = DeBayer2.process(bayBP, DeBayer2Config(MosaicOrder.R_G, Demosaicing.AdaptiveSmoothHue))
    val cp = DeBayer2.stackToColorProcessor(dstStack, bpp)
    assert(meanDistance(cp, refCP) <= meanDistanceTolerance * 3)
  }
}
