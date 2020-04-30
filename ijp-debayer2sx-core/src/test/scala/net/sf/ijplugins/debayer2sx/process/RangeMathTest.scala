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

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RangeMathTest extends AnyFlatSpec with BeforeAndAfter with Matchers {

  behavior of "RangeMath"

  it should "add to a range" in {
    val r = Range(5, 10, 3)
    val delta = 3

    val r2 = r + delta

    r2.start should be(r.start + delta)
    r2.end should be(r.end + delta)
    r2.step should be(r.step)
  }

  it should "subtract from a range" in {
    val r = Range(5, 10, 3)
    val delta = 3

    val r2 = r - delta

    r2.start should be(r.start - delta)
    r2.end should be(r.end - delta)
    r2.step should be(r.step)
  }


}
