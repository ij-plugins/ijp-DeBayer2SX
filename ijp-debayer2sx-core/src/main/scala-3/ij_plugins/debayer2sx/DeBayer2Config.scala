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

object DeBayer2Config {

  /**
    * Debayer algorithm type.
    */
  enum Demosaicing(val entryName: String)
  {
    case Replication extends Demosaicing("Replication")
    case Bilinear
    extends Demosaicing("Bilinear")
    case SmoothHue
    extends Demosaicing("Smooth Hue")
    case AdaptiveSmoothHue
    extends Demosaicing("Adaptive Smooth Hue")
    case DDFAPD
    extends Demosaicing("DDFAPD without Refining")
    case DDFAPDRefined
    extends Demosaicing("DDFAPD with Refining")

    override def toString: String = entryName

    def name: String = entryName
  }

  object Demosaicing {
    val names: Array[String] = values.map(_.entryName).toArray

    def withName(name: String): Demosaicing =
      withNameOption(name).getOrElse(throw new NoSuchElementException(s"No MappingMethod with name: $name"))

    def withNameOption(name: String): Option[Demosaicing] = Demosaicing.values.find(_.name == name)
  }

  /**
    * Order of filters in Bayer image.
    * For instance B-G means that the first pixel in the first row is `B` the next one is `G`.
    */
  enum MosaicOrder(val entryName: String, val bayer1ID: Int) {
    case B_G extends MosaicOrder("B-G", 1)
    case G_B extends MosaicOrder("G-B", 3)
    case G_R extends MosaicOrder("G-R", 2)
    case R_G extends MosaicOrder("R-G", 0)

    override def toString: String = entryName

    def name: String = entryName
  }

  object MosaicOrder {
    val names: Array[String] = values.map(_.entryName).toArray

    def withName(name: String): MosaicOrder =
      withNameOption(name).getOrElse(throw new NoSuchElementException(s"No MappingMethod with name: $name"))

    def withNameOption(name: String): Option[MosaicOrder] = MosaicOrder.values.find(_.name == name)
  }

}

case class DeBayer2Config(mosaicOrder: MosaicOrder = MosaicOrder.R_G, demosaicing: Demosaicing = Demosaicing.DDFAPD)
