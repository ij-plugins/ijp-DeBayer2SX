/*
 * Image/J Plugins
 * Copyright (C) 2002-2019 Jarek Sacha
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

import ij.gui.GenericDialog
import ij.plugin.PlugIn
import ij.process.ColorProcessor
import ij.{IJ, ImagePlus}
import net.sf.ijplugins.debayer2sx.DeBayer2Config.MosaicOrder
import net.sf.ijplugins.util.IJPUtils

object MakeBayerPlugin {
  private var mosaicOrder: MosaicOrder = MosaicOrder.G_R
}

class MakeBayerPlugin extends PlugIn {

  import MakeBayerPlugin._

  private val Title = "Make_Bayer"
  private val Description = "Convert color image to Bayer pattern image."

  override def run(arg: String): Unit = {

    // We need an input image
    val imp = IJ.getImage
    if (imp == null)
      return

    // Check for supported types
    imp.getType match {
      case ImagePlus.COLOR_RGB =>
      case _ =>
        IJ.error(Title, "Unsupported image type. Expecting RGB color image.")
        return
    }

    // Ask for options
    val gd = new GenericDialog(Title)
    gd.addPanel(IJPUtils.createInfoPanel(Title, Description))
    gd.addChoice("Order of first row:", MosaicOrder.names, mosaicOrder.entryName)

    gd.showDialog()
    if (gd.wasCanceled()) return

    mosaicOrder = MosaicOrder.withName(gd.getNextChoice)

    val cp = imp.getProcessor.asInstanceOf[ColorProcessor]
    val dst = MakeBayer.process(cp, mosaicOrder)

    new ImagePlus(imp.getShortTitle + "_bayer_" + mosaicOrder, dst).show()

  }
}
