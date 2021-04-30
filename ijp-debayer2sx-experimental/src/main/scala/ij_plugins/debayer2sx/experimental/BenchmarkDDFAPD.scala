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

import ij.ImageStack
import ij.process.FloatProcessor
import ij_plugins.debayer2sx.DDFAPD

object BenchmarkDDFAPD extends App {

  run()

  private def run(): Unit = {

    val helper = new BenchmarkHelper(testIter = 5)

    val fp = new FloatProcessor(2048, 1536)

    val nbIter = 5

    for (i <- 0 until nbIter) {
      println()
      println(s"Run ${i + 1}")

      helper.measure[FloatProcessor, ImageStack](
        tag = "DDFAPD.debayerGR          ", fp,
        DDFAPD.debayerGR(_, 16, doRefine = true))
    }

    println()
    println("Min time:")
    helper.printResults()
  }
}
