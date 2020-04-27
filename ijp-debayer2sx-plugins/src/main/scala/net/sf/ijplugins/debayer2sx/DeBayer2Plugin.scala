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

package net.sf.ijplugins.debayer2sx

import ij.gui.GenericDialog
import ij.plugin.PlugIn
import ij.plugin.filter.PlugInFilter
import ij.{CompositeImage, IJ, ImagePlus}
import net.sf.ijplugins.debayer2sx.DeBayer2Config.{Demosaicing, MosaicOrder}

object DeBayer2Plugin {
  private var config: DeBayer2Config = DeBayer2Config()
}

class DeBayer2Plugin extends PlugIn {

  import DeBayer2Plugin._

  private val Title = "DeBayer2"
  private val Description = "Convert a bayer pattern image to a color image."
  private val HelpURL = "https://github.com/ij-plugins/ijp-DeBayer2SX"

  protected def Flags: Int = PlugInFilter.DOES_8G + PlugInFilter.DOES_16

  override def run(arg: String): Unit = {
    // We need an input image
    val imp = IJ.getImage
    if (imp == null)
      return

    // Check for supported types
    imp.getType match {
      case ImagePlus.GRAY8 =>
      case ImagePlus.GRAY16 =>
      case _ =>
        IJ.error(Title, "Unsupported image type. Expecting 8-bit or 16-bit gray level.")
        return
    }

    // Check for stacks
    if (imp.getStackSize != 1) {
      IJ.error(Title, "Processing of stacks not supported.")
      return
    }

    // Ask for options
    val ok = showDialog()
    if (!ok) return

    // Run DeBayer2
    IJ.showStatus("Debayering...")

    val shortTitle = imp.getShortTitle
    val (stack, bpp) = DeBayer2.process(imp.getProcessor, config)

    val dstTitle = shortTitle + "-" + Title
    val dstImp: ImagePlus = imp.getType match {
      case ImagePlus.GRAY8 =>
        val cp = DeBayer2.stackToColorProcessor(stack, bpp)
        new ImagePlus(dstTitle, cp)
      case ImagePlus.GRAY16 =>
        val ss = if (stack.getBitDepth == 16) {
          stack
        } else {
          DeBayer2.stackToShortStack(stack, bpp, bpp)
        }
        val imp1 = new ImagePlus(dstTitle, ss)
        new CompositeImage(imp1, CompositeImage.COMPOSITE)
      case _ =>
        IJ.error(Title, "Unsupported image type. Expecting 8-bit or 16-bit gray level.")
        return
    }

    dstImp.show()
  }

  private def showDialog(): Boolean = {
    // Create dialog
    val gd = new GenericDialog(Title)
    gd.addPanel(IJPUtils.createInfoPanel(Title, Description))
    gd.addChoice("Order of first row:", MosaicOrder.names, config.mosaicOrder.entryName)
    gd.addChoice("Demosaicing type", Demosaicing.names, config.demosaicing.entryName)
    gd.addHelp(HelpURL)

    // Show to the user
    gd.showDialog()
    if (gd.wasCanceled()) {
      return false
    }

    // Decode values
    config = DeBayer2Config(
      mosaicOrder = MosaicOrder.withName(gd.getNextChoice),
      demosaicing = Demosaicing.withName(gd.getNextChoice)
    )

    true
  }
}
