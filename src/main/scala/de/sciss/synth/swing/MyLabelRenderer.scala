package de.sciss.synth.swing

import prefuse.render.LabelRenderer
import java.awt.{ Image, Toolkit }
import prefuse.visual.VisualItem

object MyLabelRenderer {
   val COL_ICON = "icon"
   private val imgGroup = Toolkit.getDefaultToolkit.createImage( ScalaColliderSwing.getClass.getResource( "path_group_16.png" ))
   private val imgSynth = Toolkit.getDefaultToolkit.createImage( ScalaColliderSwing.getClass.getResource( "path_synth_16.png" ))
}

class MyLabelRenderer( label: String ) extends LabelRenderer( label ) {
   import MyLabelRenderer._

   override protected def getImage( item: VisualItem ) : Image = {
      item.get( COL_ICON ) match {
         case "synth" => imgSynth
         case "group" => imgGroup
         case _ => null
      }
   }
}