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

import ij.process.ByteProcessor
import ij_plugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}
import ij_plugins.debayer2sx.{DDFAPD, DeBayer2, DeBayer2Config}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class DDFAPD_Bug_13_Test extends AnyFlatSpec with BeforeAndAfter with Matchers {

  val prefix = "Issue #13:"

  s"$prefix DDFAPD.debayerGR" should "should throw IllegalArgumentException for images with odd sizes" in {
    val bp = new ByteProcessor(347, 440)
    assertThrows[IllegalArgumentException] {
      DDFAPD.debayerGR(bp, doRefine = true)
    }

    assertThrows[IllegalArgumentException] {
      DDFAPD.debayerGR(bp.convertToFloatProcessor(), 8, doRefine = true)
    }
  }

  s"$prefix DeBayer2.process(...,DDFAPD)" should "should work with images with odd sizes" in {
    val bp = new ByteProcessor(347, 441)
    for (o <- MosaicOrder.values)
      DeBayer2.process(bp, DeBayer2Config(mosaicOrder = o, demosaicing = Demosaicing.DDFAPD))
  }

  s"$prefix DeBayer2.process(...,DDFAPDRefined)" should "should work with images with odd sizes" in {
    val bp = new ByteProcessor(347, 441)
    for (o <- MosaicOrder.values)
      DeBayer2.process(bp, DeBayer2Config(o, Demosaicing.DDFAPDRefined))
  }
}
