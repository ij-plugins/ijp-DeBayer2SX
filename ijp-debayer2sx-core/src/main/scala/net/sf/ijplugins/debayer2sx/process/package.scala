package net.sf.ijplugins.debayer2sx

import scala.language.implicitConversions

package object process {
  implicit def wraRange(r: Range): RangeMath = new RangeMath(r)

}
