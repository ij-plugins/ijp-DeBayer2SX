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

package ij_plugins.debayer2sx.experimental

import ij.process.{FloatProcessor, ShortProcessor}
import ij.{ImagePlus, ImageStack}
import ij_plugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}
import ij_plugins.debayer2sx.debayer1.Debayer_Image
import ij_plugins.debayer2sx.{DDFAPD, DeBayer2, DeBayer2Config}

object BenchmarkDebayer extends App {

  run()

  private def run(): Unit = {

    val helper = new BenchmarkHelper(testIter = 10)

    val sp = new ShortProcessor(2048, 1536)
    val fp: FloatProcessor = sp.convertToFloatProcessor()
    val imp = new ImagePlus("", sp)

    val nbIter = 10

    for (i <- 0 until nbIter) {
      println()
      println(s"Run ${i + 1}")

      helper.measure("replicate_decode", imp, Debayer_Image.replicate_decode(0, _))
      helper.measure("average_decode  ", imp, Debayer_Image.average_decode(0, _))
      helper.measure("smooth_decode   ", imp, Debayer_Image.smooth_decode(0, _))
      helper.measure("adaptive_decode ", imp, Debayer_Image.adaptive_decode(0, _))

      helper.measure(
        tag = "DeBayer2/AdaptiveSmoothHue", sp,
        DeBayer2.process(_, DeBayer2Config(MosaicOrder.B_G, Demosaicing.AdaptiveSmoothHue)))

      helper.measure(
        tag = "DeBayer2/DDFAPDRefined    ", sp,
        DeBayer2.process(_, DeBayer2Config(MosaicOrder.G_R, Demosaicing.DDFAPDRefined)))

      helper.measure[FloatProcessor, ImageStack](
        tag = "DDFAPD.debayerGR          ", fp,
        DDFAPD.debayerGR(_, 16, doRefine = true))
    }

    println()
    println("Min time:")
    helper.printResults()
  }
}
