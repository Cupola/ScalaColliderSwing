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
import java.awt.geom.Point2D
import javax.swing.{ JFrame, JPanel, WindowConstants }
import collection.immutable.IntMap
import prefuse.{ Constants, Display, Visualization }
import prefuse.action.{ ActionList, RepaintAction }
import prefuse.action.animate.{ ColorAnimator, LocationAnimator, QualityControlAnimator, VisibilityAnimator }
import prefuse.action.assignment.{ ColorAction, FontAction }
import prefuse.action.filter.FisheyeTreeFilter
import prefuse.action.layout.CollapsedSubtreeLayout
import prefuse.action.layout.graph.{ ForceDirectedLayout, NodeLinkTreeLayout }
import prefuse.activity.{ Activity, SlowInSlowOutPacer }
import prefuse.controls.{ DragControl, FocusControl, PanControl, WheelZoomControl, ZoomControl, ZoomToFitControl }
import prefuse.data.{ Graph, Node => PNode, Tree, Tuple }
import prefuse.data.event.TupleSetListener
import prefuse.data.search.PrefixSearchTupleSet
import prefuse.data.tuple.TupleSet
import prefuse.render.{ AbstractShapeRenderer, DefaultRendererFactory, EdgeRenderer, LabelRenderer }
import prefuse.util.{ ColorLib, FontLib, PrefuseLib }
import prefuse.visual.{ NodeItem, VisualItem }
import prefuse.visual.expression.InGroupPredicate
import prefuse.visual.sort.TreeDepthItemSorter
import de.sciss.synth.{ Group, NodeManager, OSCNodeChange, OSCNodeGoMessage, OSCNodeEndMessage, OSCNodeMoveMessage,
   OSCGroupInfo, OSCSynthInfo, Server, Synth }

/**
 *    @version	0.11, 24-Apr-10
 */
class NodeTreePanel( server: Server )
extends JPanel {
   import NodeManager._

   private val label                = "name"
   private val t                    = {
      val t       = new Tree
      val nodes   = t.getNodeTable()
      PrefuseHelper.addColumn( nodes, label, label.getClass )
      t
   }
   private var map                  = IntMap( 0 -> t.addRoot() )
   private val vis                  = new Visualization()

   private val nodeListener: (AnyRef) => Unit = _ match {
      case NodeGo( synth: Synth, info ) => {
         map.get( info.parentID ).foreach( pNode => {
            // XXX should place after pred
            val n = t.addChild( pNode )
            n.set( label, synth.toString )
            map += synth.id -> n
            vis.run( "filter" )
         })
      }
      case NodeGo( group: Group, info ) => {
         map.get( info.parentID ).foreach( pNode => {
            // XXX should place after pred
            val n = t.addChild( pNode )
            n.set( label, group.toString )
            map += group.id -> n
            vis.run( "filter" )
         })
      }
      case NodeEnd( node, info ) => {
         map.get( node.id ).foreach( n => {
            t.removeChild( n )
            map -= node.id
            vis.run( "filter" )
         })
      }
      case NodeMove( node, info ) => {
         map.get( node.id ).foreach( nOld => {
            t.removeChild( nOld )
            map.get( info.parentID ).foreach( pNode => {
               // XXX should place after pred
               val nNew = t.addChild( pNode )
               nNew.set( label, nOld.get( label ))
               vis.run( "filter" )
            })
         })
      }
      case Cleared => {
         val r = t.getRoot
         val c = r.children
         while( c.hasNext ) {
            t.removeChild( c.next.asInstanceOf[ PNode ])
         }
         map   = IntMap( 0 -> r )
         vis.run( "filter" )
      }
   }

   // ---- constructor ----
   {
      val tree        = "tree"
      val treeNodes   = "tree.nodes"
      val treeEdges   = "tree.edges"

//      val g = new Graph()
//      for( i <- (0 until 3) ) {
//          val n1 = g.addNode()
//          val n2 = g.addNode()
//          val n3 = g.addNode()
//          g.addEdge( n1, n2 )
//          g.addEdge( n1, n3 )
//          g.addEdge( n2, n3 )
//      }
//      g.addEdge( 0, 3 )
//      g.addEdge( 3, 6 )
//      g.addEdge( 6, 0 )
//
////      root.setString( label, "Gugu" )
////      testChild.setString( label, "Gugu" )
      val display = new Display( vis )

      vis.add( tree, t )

      val nodeRenderer = new LabelRenderer( label )
      nodeRenderer.setRenderType( AbstractShapeRenderer.RENDER_TYPE_FILL )
      nodeRenderer.setHorizontalAlignment( Constants.LEFT )
      nodeRenderer.setRoundedCorner( 8, 8 )
      val edgeRenderer = new EdgeRenderer( Constants.EDGE_TYPE_CURVE )

      val rf = new DefaultRendererFactory( nodeRenderer )
      rf.add( new InGroupPredicate( treeEdges ), edgeRenderer )
      vis.setRendererFactory( rf )

      // colors
//      val nodeColor = new NodeColorAction( treeNodes )
      val nodeColor = new ColorAction( treeNodes, VisualItem.TEXTCOLOR, ColorLib.rgb( 255, 0, 0 ))
      val textColor = new ColorAction( treeNodes, VisualItem.TEXTCOLOR, ColorLib.rgb( 0, 0, 0 ))
      vis.putAction( "textColor", textColor )

      val edgeColor = new ColorAction( treeEdges, VisualItem.STROKECOLOR, ColorLib.rgb( 200, 200, 200 ))

      // quick repaint
      val repaint = new ActionList()
      repaint.add( nodeColor )
      repaint.add( new RepaintAction() )
      vis.putAction( "repaint", repaint )

      // full paint
      val fullPaint = new ActionList()
      fullPaint.add( nodeColor )
      vis.putAction( "fullPaint", fullPaint )

      // animate paint change
      val animatePaint = new ActionList( 400 )
      animatePaint.add( new ColorAnimator( treeNodes ))
      animatePaint.add( new RepaintAction() )
      vis.putAction( "animatePaint", animatePaint )

      // create the tree layout action
      val orientation = Constants.ORIENT_LEFT_RIGHT      
      val treeLayout = new NodeLinkTreeLayout( tree, orientation, 50, 0, 8 )
      treeLayout.setLayoutAnchor( new Point2D.Double( 25, 300 ))
      treeLayout.setOrientation( orientation )
      vis.putAction( "treeLayout", treeLayout )

      val subLayout = new CollapsedSubtreeLayout( tree, orientation )
      subLayout.setOrientation( orientation )
      vis.putAction( "subLayout", subLayout )

//      val autoPan = new AutoPanAction()

      // create the filtering and layout
      val filter = new ActionList()
      filter.add( new FisheyeTreeFilter( tree, 2 ))
      filter.add( new FontAction( treeNodes, FontLib.getFont( "Helvetica", 16 )))
      filter.add( treeLayout )
      filter.add( subLayout )
      filter.add( textColor )
      filter.add( nodeColor )
      filter.add( edgeColor )
      vis.putAction( "filter", filter )

      // animated transition
      val animate = new ActionList( 1000 )
      animate.setPacingFunction( new SlowInSlowOutPacer() )
//      animate.add( autoPan )
      animate.add( new QualityControlAnimator() )
      animate.add( new VisibilityAnimator( tree ))
      animate.add( new LocationAnimator( treeNodes ))
      animate.add( new ColorAnimator( treeNodes ))
      animate.add( new RepaintAction() )
      vis.putAction( "animate", animate )
      vis.alwaysRunAfter( "filter", "animate" )

      // create animator for orientation changes
      val orient = new ActionList( 2000 )
      orient.setPacingFunction( new SlowInSlowOutPacer() )
//      orient.add( autoPan )
      orient.add( new QualityControlAnimator() )
      orient.add( new LocationAnimator( treeNodes ))
      orient.add( new RepaintAction() )
      vis.putAction( "orient", orient )

      // ------------------------------------------------

      // initialize the display
      display.setSize( 400, 400 )
      display.setItemSorter( new TreeDepthItemSorter() )
      display.addControlListener( new ZoomToFitControl() )
      display.addControlListener( new ZoomControl() )
      display.addControlListener( new WheelZoomControl() )
      display.addControlListener( new PanControl() )
      display.addControlListener( new FocusControl( 1, "filter" ))

//      registerKeyboardAction(
//          new OrientAction(Constants.ORIENT_LEFT_RIGHT),
//          "left-to-right", KeyStroke.getKeyStroke("ctrl 1"), WHEN_FOCUSED);
//      registerKeyboardAction(
//          new OrientAction(Constants.ORIENT_TOP_BOTTOM),
//          "top-to-bottom", KeyStroke.getKeyStroke("ctrl 2"), WHEN_FOCUSED);
//      registerKeyboardAction(
//          new OrientAction(Constants.ORIENT_RIGHT_LEFT),
//          "right-to-left", KeyStroke.getKeyStroke("ctrl 3"), WHEN_FOCUSED);
//      registerKeyboardAction(
//          new OrientAction(Constants.ORIENT_BOTTOM_TOP),
//          "bottom-to-top", KeyStroke.getKeyStroke("ctrl 4"), WHEN_FOCUSED);

      // ------------------------------------------------

       nodeRenderer.setHorizontalAlignment( Constants.LEFT )
       edgeRenderer.setHorizontalAlignment1( Constants.RIGHT )
       edgeRenderer.setHorizontalAlignment2( Constants.LEFT )
       edgeRenderer.setVerticalAlignment1( Constants.CENTER )
       edgeRenderer.setVerticalAlignment2( Constants.CENTER )

      vis.run( "filter" )

      val search = new PrefixSearchTupleSet();
      vis.addFocusGroup( Visualization.SEARCH_ITEMS, search )
      search.addTupleSetListener( new TupleSetListener() {
          def tupleSetChanged( t: TupleSet, add: Array[ Tuple ], rem: Array[ Tuple ]) {
              vis.cancel( "animatePaint" )
              vis.run( "fullPaint" )
              vis.run( "animatePaint" )
          }
      })

//      vis.setValue( edges, null, VisualItem.INTERACTIVE, Boolean.FALSE )

      display.setForeground( Color.WHITE )
      display.setBackground( Color.BLACK )

      setLayout( new BorderLayout() )
      add( display, BorderLayout.CENTER )

      server.nodeMgr.addListener( nodeListener )
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
