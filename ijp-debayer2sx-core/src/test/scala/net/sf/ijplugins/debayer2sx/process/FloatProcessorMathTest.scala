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

package net.sf.ijplugins.debayer2sx.process

import ij.process.FloatProcessor
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FloatProcessorMathTest extends AnyFlatSpec with BeforeAndAfter with Matchers {

  behavior of "FloatProcessorMath"

  it should "slice to correct size" in {
    val fp = new FloatProcessor(12, 30)
    val fp1 = fp(Range(0, 12, 2), Range(0, 30, 3))

    fp1.getWidth should be(6)
    fp1.getHeight should be(10)
  }

}
