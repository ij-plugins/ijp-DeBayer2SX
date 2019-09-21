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
 * Latest release available at https://github.com/ij-plugins/ijp-DeBayer2SX
 */

package net.sf.ijplugins.debayer2sx

import ij.gui.GenericDialog
import ij.plugin.PlugIn
import ij.process.ColorProcessor
import ij.{CompositeImage, IJ, ImagePlus, ImageStack}
import net.sf.ijplugins.debayer2sx.DeBayer2Config.MosaicOrder
import net.sf.ijplugins.util.IJPUtils

object MakeBayerPlugin {
  private var mosaicOrder: MosaicOrder = MosaicOrder.R_G
}

class MakeBayerPlugin extends PlugIn {

  import MakeBayerPlugin._

  private val Title = "Make Bayer"
  private val Description = "Convert color image to Bayer pattern image."
  private val HelpURL = "https://github.com/ij-plugins/ijp-DeBayer2SX"

  override def run(arg: String): Unit = {

    def isColorComposite(imp: ImagePlus): Boolean = {
      imp.isComposite && imp.getNChannels == 3 && imp.getStackSize == 3 && imp.isInstanceOf[CompositeImage]
    }

    // We need an input image
    val imp = IJ.getImage
    if (imp == null)
      return

    // Check for supported types
    if (!(imp.getType == ImagePlus.COLOR_RGB || isColorComposite(imp))) {
      IJ.error(Title, "Unsupported image type. Expecting RGB color image or 3 channel composite image.")
      return
    }

    // Ask for options
    val gd = new GenericDialog(Title)
    gd.addPanel(IJPUtils.createInfoPanel(Title, Description))
    gd.addChoice("Order of first row:", MosaicOrder.names, mosaicOrder.entryName)
    gd.addHelp(HelpURL)


    gd.showDialog()
    if (gd.wasCanceled()) return

    mosaicOrder = MosaicOrder.withName(gd.getNextChoice)

    val srcStack: ImageStack =
      if (imp.getType == ImagePlus.COLOR_RGB) {
        val cp = imp.getProcessor.asInstanceOf[ColorProcessor]
        MakeBayer.toStack(cp)
      } else if (isColorComposite(imp)) {
        imp.getStack
      } else {
        IJ.error(Title, "Unsupported image type. Expecting RGB color image or 3 channel composite image.")
        return
      }

    val dstStack = MakeBayer.process(srcStack, mosaicOrder)
    new ImagePlus(imp.getShortTitle + "_bayer_" + mosaicOrder, dstStack).show()
  }
}
