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
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = -1)
@Measurement(iterations = 10, time = -1)
@Fork(value = 1)
class JMHBenchmarkDDFAPD {
  @Param(Array("2048"))
  var width: Int = _

  @Param(Array("1536"))
  var height: Int = _


  var fp: FloatProcessor = new FloatProcessor(width, height)

  @Setup
  def setup(): Unit = {
    fp = new FloatProcessor(width, height)
  }

  @Benchmark
  def DDFAPD_cfor(): ImageStack = {
    val r = ij_plugins.debayer2sx.DDFAPD.debayerGR(fp, 16, doRefine = true)
    r
  }

  @Benchmark
  def DDFAPD_v_1_2_0(): ImageStack = {
    val r = ij_plugins.debayer2sx.experimental.v_1_2_0.core.DDFAPD.debayerGR(fp, 16, doRefine = true)
    r
  }
}
