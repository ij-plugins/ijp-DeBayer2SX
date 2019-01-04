package net.sf.ijplugins.util

import java.awt._
import java.io.IOException
import java.net.URISyntaxException

import ij.IJ
import javax.swing._
import javax.swing.border.EmptyBorder
import javax.swing.event.HyperlinkEvent.EventType._
import javax.swing.event.{HyperlinkEvent, HyperlinkListener}
import javax.swing.text.html.HTMLDocument

/**
  */
object IJPUtils {
  /**
    * Load icon as a resource for given class without throwing exceptions.
    *
    * @param aClass Class requesting resource.
    * @param path   Icon file path.
    * @return Icon or null if loading failed.
    */
  def loadIcon(aClass: Class[_], path: String): ImageIcon = {
    try {
      val url = aClass.getResource(path)
      if (url == null) {
        IJ.log("Unable to find resource '" + path + "' for class '" + aClass.getName + "'.")
        return null
      }
      return new ImageIcon(url)
    } catch {
      case t: Throwable =>
        IJ.log("Error loading icon from resource '" + path + "' for class '" + aClass.getName + "'. \n" + t.getMessage)
    }
    null
  }

  /**
    * Create pane for displaying a message that may contain HTLM formatting, including links.
    *
    * @param message the message.
    * @param  title  used in error dialogs.
    * @return component containg the message.
    */
  def createHTMLMessageComponent(message: String, title: String): JComponent = {
    val pane = new JEditorPane()
    pane.setContentType("text/html")
    pane.setEditable(false)
    pane.setOpaque(false)
    pane.setBorder(null)
    val htmlDocument = pane.getDocument.asInstanceOf[HTMLDocument]
    val font = UIManager.getFont("Label.font")
    val bodyRule = "body { font-family: " + font.getFamily + "; " + "font-size: " + font.getSize + "pt; }"
    htmlDocument.getStyleSheet.addRule(bodyRule)
    pane.addHyperlinkListener(new HyperlinkListener() {
      def hyperlinkUpdate(e: HyperlinkEvent) {
        if (e.getEventType == ACTIVATED) {
          try {
            Desktop.getDesktop.browse(e.getURL.toURI)
          } catch {
            case ex@(_: IOException | _: URISyntaxException) =>
              IJ.error(title, "Error following a link.\n" + ex.getMessage)
          }
        }
      }
    })
    pane.setText(message)
    pane
  }

  /**
    * Creeate simple info panel for a plugin dialog. Intended to be displayed at the top.
    *
    * @param title   title displayed in bold font larger than default.
    * @param message message that can contain HTML formatting.
    * @return a panel containing the message with a title and a default icon.
    */
  def createInfoPanel(title: String, message: String): Panel = {
    // TODO: use icon with rounded corners
    val rootPanel = new Panel(new BorderLayout(7, 7))
    val titlePanel = new Panel(new BorderLayout(7, 7))
    val logo = IJPUtils.loadIcon(this.getClass, "/net/sf/ijplugins/IJP-48.png")
    if (logo != null) {
      val logoLabel = new JLabel(logo, SwingConstants.CENTER)
      logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT)
      titlePanel.add(logoLabel, BorderLayout.WEST)
    }
    val titleLabel = new JLabel(title)
    val font = titleLabel.getFont
    titleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize * 2))
    titlePanel.add(titleLabel, BorderLayout.CENTER)

    rootPanel.add(titlePanel, BorderLayout.NORTH)

    val messageComponent = IJPUtils.createHTMLMessageComponent(message, title)
    rootPanel.add(messageComponent, BorderLayout.CENTER)

    // Add some spacing at the bottom
    val separatorPanel = new JPanel(new BorderLayout())
    separatorPanel.setBorder(new EmptyBorder(7, 0, 7, 0))
    separatorPanel.add(new JSeparator(), BorderLayout.SOUTH)
    rootPanel.add(separatorPanel, BorderLayout.SOUTH)

    rootPanel
  }

}
