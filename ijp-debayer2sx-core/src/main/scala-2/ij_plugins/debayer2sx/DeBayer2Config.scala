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

import enumeratum.{Enum, EnumEntry}
import ij_plugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}

import scala.collection.immutable

object DeBayer2Config {

  sealed abstract class Demosaicing(override val entryName: String) extends EnumEntry

  /**
    * Debayer algorithm type.
    */
  object Demosaicing extends Enum[Demosaicing] {
    val values: immutable.IndexedSeq[Demosaicing] = findValues
    val names: Array[String] = values.map(_.entryName).toArray

    case object Replication extends Demosaicing("Replication")

    case object Bilinear extends Demosaicing("Bilinear")

    case object SmoothHue extends Demosaicing("Smooth Hue")

    case object AdaptiveSmoothHue extends Demosaicing("Adaptive Smooth Hue")

    case object DDFAPD extends Demosaicing("DDFAPD without Refining")

    case object DDFAPDRefined extends Demosaicing("DDFAPD with Refining")

  }

  sealed abstract class MosaicOrder(override val entryName: String, val bayer1ID: Int) extends EnumEntry

  /**
    * Order of filters in Bayer image.
    * For instance B-G means that the first pixel in the first row is `B` the next one is `G`.
    */
  object MosaicOrder extends Enum[MosaicOrder] {
    val values: immutable.IndexedSeq[MosaicOrder] = findValues
    val names: Array[String] = values.map(_.entryName).toArray

    case object B_G extends MosaicOrder("B-G", 1)

    case object G_B extends MosaicOrder("G-B", 3)

    case object G_R extends MosaicOrder("G-R", 2)

    case object R_G extends MosaicOrder("R-G", 0)

  }

}

case class DeBayer2Config(mosaicOrder: MosaicOrder = MosaicOrder.R_G, demosaicing: Demosaicing = Demosaicing.DDFAPD)
