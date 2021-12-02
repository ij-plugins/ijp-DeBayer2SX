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

import ij_plugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class DeBayer2ConfigTest extends AnyFlatSpec {

  "Demosaicing" should "have unique names" in {
    val names = Demosaicing.values.map(_.entryName)
    names.toSet.size should be(Demosaicing.values.size)
  }

  "Demosaicing" should "lookup by name" in {
    for (v <- Demosaicing.values) {
      println(s"v: '$v', entryName: '${v.entryName}'")

      Demosaicing.withName(v.entryName) should be(v)
    }
  }

  "MosaicOrder" should "have unique names" in {
    val names = MosaicOrder.values.map(_.entryName)
    names.toSet.size should be(MosaicOrder.values.size)
  }

  "MosaicOrder" should "lookup by name" in {
    for (v <- MosaicOrder.values) {
      println(s"v: '$v', entryName: '${v.entryName}'")

      MosaicOrder.withName(v.entryName) should be(v)
    }
  }
}
