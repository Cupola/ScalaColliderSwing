/*
 *  NodeTreePanel.scala
 *  (ScalaCollider-Swing)
 *
 *  Copyright (c) 2008-2010 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.synth.swing

import java.awt.{ BorderLayout, Color }
import javax.swing.{ JFrame, JPanel, WindowConstants }
import prefuse.{ Display, Visualization }
import prefuse.action.{ ActionList, RepaintAction }
import prefuse.action.assignment.ColorAction
import prefuse.action.layout.graph.ForceDirectedLayout
import prefuse.activity.Activity
import prefuse.controls.{ DragControl, PanControl, WheelZoomControl, ZoomControl, ZoomToFitControl }
import prefuse.data.{ Graph, Tree }
import prefuse.render.{ DefaultRendererFactory, LabelRenderer }
import prefuse.util.{ ColorLib, PrefuseLib }
import prefuse.visual.{ NodeItem, VisualItem }
import de.sciss.synth.Server

/**
 *    @version	0.11, 23-Apr-10
 */
class NodeTreePanel( server: Server )
extends JPanel {

   // ---- constructor ----
   {
      val label   = "name"
      val graph   = "nodes"
      val nodes   = "graph.nodes"
      val edges   = "graph.edges"

//      val tree    = new Tree()
//      val root    = tree.addRoot()
//      val testChild   = tree.addChild( root )

      val g = new Graph()
      for( i <- (0 until 3) ) {
          val n1 = g.addNode()
          val n2 = g.addNode()
          val n3 = g.addNode()
          g.addEdge( n1, n2 )
          g.addEdge( n1, n3 )
          g.addEdge( n2, n3 )
      }
      g.addEdge( 0, 3 )
      g.addEdge( 3, 6 )
      g.addEdge( 6, 0 )

//      root.setString( label, "Gugu" )
//      testChild.setString( label, "Gugu" )
      val vis     = new Visualization()
//      val vg      = vis.addGraph( graph, tree )
//      vis.setValue( edges, null, VisualItem.INTERACTIVE, Boolean.FALSE )

      // set up the renderers
      val tr = new LabelRenderer( label )
      tr.setRoundedCorner( 8, 8 )
      val drf = new DefaultRendererFactory( tr ) 
      vis.setRendererFactory( drf )

      // update labeling
      tr.setTextField( label )

      // update graph
//      vis.removeGroup( graph )
      val vg = vis.addGraph( graph, g )
//      vis.setValue( edges, null, VisualItem.INTERACTIVE, Boolean.FALSE )
      val f = vg.getNode( 0 ).asInstanceOf[ VisualItem ]
      vis.getGroup( Visualization.FOCUS_ITEMS ).setTuple( f )
      f.setFixed( false )

      val draw = new ActionList()
//      draw.add( filter )
      draw.add( new ColorAction( nodes, VisualItem.FILLCOLOR, ColorLib.rgb( 200, 200, 255 )))
      draw.add( new ColorAction( nodes, VisualItem.STROKECOLOR, 0 ))
      draw.add( new ColorAction( nodes, VisualItem.TEXTCOLOR, ColorLib.rgb( 0, 0, 0 )))
      draw.add( new ColorAction( edges, VisualItem.FILLCOLOR, ColorLib.gray( 200 )))
      draw.add( new ColorAction( edges, VisualItem.STROKECOLOR, ColorLib.gray( 200 )))

      val fdl = new ForceDirectedLayout( graph )
      val fsim = fdl.getForceSimulator()
      fsim.getForces()( 0 ).setParameter( 0, -1.2f )

      val animate = new ActionList( Activity.INFINITY )
      animate.add( fdl )
//      animate.add( fill )
      animate.add( new RepaintAction() )

      vis.putAction( "draw", draw )
      vis.putAction( "layout", animate )
      vis.runAfter( "draw", "layout" )

      // STEP 4: set up a display to show the visualization
      val display = new Display( vis )
      display.setSize( 400, 400 )
      display.setForeground( Color.WHITE )
      display.setBackground( Color.BLACK )

      // main display controls
//      display.addControlListener( new FocusControl( 1 ))
      display.addControlListener( new DragControl() )
      display.addControlListener( new PanControl() )
      display.addControlListener( new ZoomControl() )
      display.addControlListener( new WheelZoomControl() )
      display.addControlListener( new ZoomToFitControl() )
//      display.addControlListener( new NeighborHighlightControl() )

      // position and fix the default focus node
      val focus = vg.getNode( 0 ).asInstanceOf[ NodeItem ]
      PrefuseLib.setX( focus, null, 200 )
      PrefuseLib.setY( focus, null, 200 )
//      focusGroup.setTuple( focus )

      // set things running
      vis.run( "layout" )

      setLayout( new BorderLayout() )
      add( display, BorderLayout.CENTER )
   }

	def makeWindow: JFrame = {
		val frame = new JFrame( "Nodes" )
//		frame.setResizable( false )
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )
		frame.getContentPane.add( this )
		frame.pack()
//		frame.setVisible( true )
		frame
	}
}
