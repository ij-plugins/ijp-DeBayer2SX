/*
 * Image/J Plugins
 * Copyright (C) 2002-2018 Jarek Sacha
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
 * Latest release available at http://sourceforge.net/projects/ij-plugins/
 */

package net.sf.ijplugins.debayer2sx

import ij.process.{ByteProcessor, ColorProcessor}

object Utils {

  def compare(im1: ByteProcessor, im2: ByteProcessor): Unit = {
    val w = im1.getWidth
    val h = im1.getHeight

    assert(w == im2.getWidth, "Width should match")
    assert(h == im2.getHeight, "Height should match")

    for (y <- Range(0, h); x <- Range(0, w)) {
      assert(im1.get(x, y) == im2.get(x, y), s"Location ($x, $y)")
    }
  }

  def compare(im1: ColorProcessor, im2: ColorProcessor): Unit = {
    val w = im1.getWidth
    val h = im1.getHeight

    assert(w == im2.getWidth, "Width should match")
    assert(h == im2.getHeight, "Height should match")

    for (y <- Range(0, h); x <- Range(0, w)) {
      assert(im1.getColor(x, y).getRed == im2.getColor(x, y).getRed, s"Red Location ($x, $y)")
      assert(im1.getColor(x, y).getGreen == im2.getColor(x, y).getGreen, s"Green Location ($x, $y)")
      assert(im1.getColor(x, y).getBlue == im2.getColor(x, y).getBlue, s"Blue Location ($x, $y)")
    }
  }
}
