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

import java.awt.{ BorderLayout, Color, EventQueue }
import java.awt.geom.Point2D
import collection.immutable.IntMap
import prefuse.{ Constants, Display }
import prefuse.action.{ ActionList, RepaintAction }
import prefuse.action.animate.{ ColorAnimator, LocationAnimator, VisibilityAnimator }
import prefuse.action.assignment.{ ColorAction }
import prefuse.action.layout.graph.{  NodeLinkTreeLayout }
import prefuse.controls.{ PanControl, WheelZoomControl, ZoomControl, ZoomToFitControl }
import prefuse.data.{ Node => PNode, Tree }
import prefuse.data.tuple.DefaultTupleSet
import prefuse.render.{ AbstractShapeRenderer, DefaultRendererFactory, EdgeRenderer, LabelRenderer }
import prefuse.util.{ ColorLib }
import prefuse.visual.{ VisualItem }
import prefuse.visual.expression.InGroupPredicate
import prefuse.visual.sort.TreeDepthItemSorter
import de.sciss.synth.{ Group, Node, NodeManager, Server, Synth }
import de.sciss.synth.osc.OSCNodeInfo
import VisualInsertionTree._
import javax.swing.{ JFrame, JPanel, WindowConstants }
import java.net.URL

/**
 *    @version	0.11, 27-Apr-10
 */
class NodeTreePanel( server: Server )
extends JPanel {
   import NodeManager._
   import MyLabelRenderer._

   private val COL_LABEL            = "name"
   private val COL_PAUSED           = "paused"
   val t                    = {
      val t       = new Tree
      val nodes   = t.getNodeTable()
      PrefuseHelper.addColumn( nodes, COL_LABEL,  classOf[ String ])
      PrefuseHelper.addColumn( nodes, COL_ICON,   classOf[ String ])
      PrefuseHelper.addColumn( nodes, COL_PAUSED, classOf[ Boolean ])
      FIELDS.foreach( PrefuseHelper.addColumn( nodes, _, classOf[ PNode ]))
      t
   }
   var map                          = IntMap( 0 -> t.addRoot() )
   val vis                          = new InsertionTreeVis()
   // create the tree layout action
   private val orientation = Constants.ORIENT_LEFT_RIGHT
   private val GROUP_TREE           = "tree"
   private val GROUP_NODES          = "tree.nodes"
   private val GROUP_EDGES          = "tree.edges"
   private val GROUP_PAUSED         = "paused"
   private val ACTION_LAYOUT        = "layout"
   private val ACTION_LAYOUT_ANIM   = "layout-anim"
   private val ACTION_COLOR         = "color"
   private val ACTION_COLOR_ANIM    = "layout-anim"
   private val FADE_TIME            = 333

   private val setPaused      = new DefaultTupleSet()

   private val nodeListener: (AnyRef) => Unit = m => {
//      assert( java.awt.EventQueue.isDispatchThread )
      m match {
      case NodeGo( synth: Synth, info ) => defer( nlAddSynth( synth, info ))
      case NodeGo( group: Group, info ) => defer( nlAddGroup( group, info ))
      case NodeEnd( node, info )        => defer( nlRemoveNode( node, info ))
      case NodeMove( node, info )       => defer( nlMoveChild( node, info ))
      case NodeOn( node, info )         => defer( nlPauseChild( node, false ))
      case NodeOff( node, info )        => defer( nlPauseChild( node, true ))
      case Cleared                      => defer( nlClear )
   }}

   // ---- constructor ----
   {
      val display = new Display( vis )

      vis.add( GROUP_TREE, t )
      vis.addFocusGroup( GROUP_PAUSED, setPaused )

      val nodeRenderer = new MyLabelRenderer( COL_LABEL )
      nodeRenderer.setRenderType( AbstractShapeRenderer.RENDER_TYPE_FILL )
      nodeRenderer.setHorizontalAlignment( Constants.LEFT )
      nodeRenderer.setRoundedCorner( 8, 8 )
      nodeRenderer.setVerticalPadding( 2 )
      val edgeRenderer = new EdgeRenderer( Constants.EDGE_TYPE_CURVE )

      val rf = new DefaultRendererFactory( nodeRenderer )
      rf.add( new InGroupPredicate( GROUP_EDGES), edgeRenderer )
      vis.setRendererFactory( rf )

      // colors
//      val nodeColor = new NodeColorAction( treeNodes )
      val actionNodeColor = new ColorAction( GROUP_NODES, VisualItem.FILLCOLOR, ColorLib.rgb( 200, 200, 200 ))
      actionNodeColor.add( new InGroupPredicate( GROUP_PAUSED ), ColorLib.rgb( 200, 0, 0 ))
      val actionTextColor = new ColorAction( GROUP_NODES, VisualItem.TEXTCOLOR, ColorLib.rgb( 0, 0, 0 ))
//      vis.putAction( "nodeColor", actionNodeColor )
//      vis.putAction( "textColor", actionTextColor )

      val actionEdgeColor = new ColorAction( GROUP_EDGES, VisualItem.STROKECOLOR, ColorLib.rgb( 200, 200, 200 ))

      // quick repaint
      val actionColor = new ActionList()
      actionColor.add( actionNodeColor )
//      actionColorRepaint.add( new RepaintAction() )
//actionColor.add( new RepaintAction() )
      vis.putAction( ACTION_COLOR, actionColor )

//      // full paint
//      val fullPaint = new ActionList()
//      fullPaint.add( nodeColor )
//      vis.putAction( "fullPaint", fullPaint )

      // animate paint change
      val animateColor = new ActionList( FADE_TIME )
      animateColor.add( new ColorAnimator( GROUP_NODES ))
      animateColor.add( new RepaintAction() )
      vis.putAction( ACTION_COLOR_ANIM, animateColor )
      vis.alwaysRunAfter( ACTION_COLOR, ACTION_COLOR_ANIM )

      val actionTreeLayout = new NodeLinkTreeLayout( GROUP_TREE, orientation, 32, 2, 8 )
//      treeLayout.setLayoutAnchor( new Point2D.Double( 25, 300 ))
//      treeLayout.setOrientation( orientation )
//      vis.putAction( "treeLayout", actionTreeLayout )

//      val subLayout = new CollapsedSubtreeLayout( tree, orientation )
//      subLayout.setOrientation( orientation )
//      vis.putAction( "subLayout", subLayout )

//      val autoPan = new AutoPanAction()

      // create the filtering and layout
      val actionLayout = new ActionList()
//      actionLayout.add( new FisheyeTreeFilter( tree, 2 ))
//      actionLayout.add( new FontAction( treeNodes, FontLib.getFont( "Helvetica", 16 )))
      actionLayout.add( actionTreeLayout )
//      actionLayout.add( subLayout )
      actionLayout.add( actionTextColor )
      actionLayout.add( actionNodeColor )
      actionLayout.add( actionEdgeColor )
//actionLayout.add( new RepaintAction() )
      vis.putAction( ACTION_LAYOUT, actionLayout )

      // animated transition
      val animateLayout = new ActionList( FADE_TIME )
//      animateLayout.setPacingFunction( new SlowInSlowOutPacer() )
//      animateLayout.add( autoPan )
//      animateLayout.add( new QualityControlAnimator() )
      animateLayout.add( new VisibilityAnimator( GROUP_TREE ))
      animateLayout.add( new LocationAnimator( GROUP_NODES ))
      animateLayout.add( new ColorAnimator( GROUP_NODES ))
      animateLayout.add( new RepaintAction() )
      vis.putAction( ACTION_LAYOUT_ANIM, animateLayout )
      vis.alwaysRunAfter( ACTION_LAYOUT, ACTION_LAYOUT_ANIM )

//      // create animator for orientation changes
//      val orient = new ActionList( 2000 )
//      orient.setPacingFunction( new SlowInSlowOutPacer() )
////      orient.add( autoPan )
//      orient.add( new QualityControlAnimator() )
//      orient.add( new LocationAnimator( treeNodes ))
//      orient.add( new RepaintAction() )
//      vis.putAction( "orient", orient )

      // ------------------------------------------------

      // initialize the display
      display.setSize( 400, 400 )
      display.setItemSorter( new TreeDepthItemSorter() )
      display.addControlListener( new ZoomToFitControl() )
      display.addControlListener( new ZoomControl() )
      display.addControlListener( new WheelZoomControl() )
      display.addControlListener( new PanControl() )
      display.setHighQuality( true )
//      display.addControlListener( new FocusControl( 1, "filter" ))

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

      vis.run( ACTION_LAYOUT )

//      val search = new PrefixSearchTupleSet();
////      vis.addFocusGroup( Visualization.SEARCH_ITEMS, search )
//      search.addTupleSetListener( new TupleSetListener() {
//          def tupleSetChanged( t: TupleSet, add: Array[ Tuple ], rem: Array[ Tuple ]) {
//              vis.cancel( "animatePaint" )
//              vis.run( "fullPaint" )
//              vis.run( "animatePaint" )
//          }
//      })

//      vis.setValue( edges, null, VisualItem.INTERACTIVE, Boolean.FALSE )

      display.setForeground( Color.WHITE )
      display.setBackground( Color.BLACK )

      setLayout( new BorderLayout() )
      add( display, BorderLayout.CENTER )

      server.nodeMgr.addListener( nodeListener )

//      val test = treeLayout.getLayoutRoot
//      val ch = test.getFirstChild
//      val st = t.getSpanningTree()
//      println( test )
   }

   private def defer( code: => Unit ) {
      EventQueue.invokeLater( new Runnable { def run = code })
   }

   private def insertChild( pNode: PNode, pParent: PNode, info: OSCNodeInfo ) {
      val pPred = if( info.predID == -1 ) {
         pParent.set( HEAD, pNode )
         null
      } else {
         map.get( info.predID ) orNull
      }
      if( pPred != null ) {
         pPred.set( SUCC, pNode )
         pNode.set( PRED, pPred )
      }
      val pSucc = if( info.succID == -1 ) {
         pParent.set( TAIL, pNode )
         null
      } else {
         map.get( info.succID ) orNull
      }
      if( pSucc != null ) {
         pNode.set( SUCC, pSucc )
         pSucc.set( PRED, pNode )
      }
      pNode.set( PARENT, pParent )
   }

   private def deleteChild( node: Node, pNode: PNode ) {
      removeChild( pNode )
      // note: we need to update the graph structure first,
      // because after calling Tree->removeChild, it is
      // not allowed to call get on the PNode any more.
      t.removeChild( pNode )
      map -= node.id
   }

   private def removeChild( pNode: PNode ) {
      val pPred   = pNode.get( PRED ).asInstanceOf[ PNode ]
      val pSucc   = pNode.get( SUCC ).asInstanceOf[ PNode ]
      val pParent = pNode.get( PARENT ).asInstanceOf[ PNode ]
      if( pPred == null ) {
         if( pParent != null ) pParent.set( HEAD, pSucc )
      } else {
         pPred.set( SUCC, pSucc )
      }
      if( pSucc == null ) {
         if( pParent != null ) pParent.set( TAIL, pPred )
      } else {
         pSucc.set( PRED, pPred )
      }
   }

   private def createChild( node: Node, pParent: PNode, info: OSCNodeInfo ) : PNode = {
      val pNode = t.addChild( pParent )
      insertChild( pNode, pParent, info )
      map += node.id -> pNode
      pNode
   }

   private def nlAddSynth( synth: Synth, info: OSCNodeInfo ) {
      map.get( info.parentID ).map( pParent => {
         vis.synchronized {
            stopAnimation
            val pNode = createChild( synth, pParent, info )
            pNode.set( COL_LABEL, synth.id.toString )
            pNode.set( COL_ICON, "synth" )
            initPosAndAnimate( pNode )
         }
      })
   }

   private def nlAddGroup( group: Group, info: OSCNodeInfo ) {
      map.get( info.parentID ).map( pParent => {
         vis.synchronized {
            stopAnimation
            val pNode = createChild( group, pParent, info )
            pNode.set( COL_LABEL, group.id.toString )
            pNode.set( COL_ICON, "group" )
            initPosAndAnimate( pNode )
         }
      })
   }

   private def nlRemoveNode( node: Node, info: OSCNodeInfo ) {
      map.get( node.id ).foreach( pNode => {
         vis.synchronized {
            stopAnimation
            deleteChild( node, pNode )
            vis.run( ACTION_LAYOUT )
         }
      })
   }

   private def nlMoveChild( node: Node, info: OSCNodeInfo ) {
      map.get( node.id ).foreach( pOld => {
         vis.synchronized {
            stopAnimation
            val viOld      = vis.getVisualItem( GROUP_TREE, pOld )
            val startPos   = if( viOld != null ) Some( new Point2D.Double( viOld.getX, viOld.getY )) else None
            val labelOld   = pOld.get( COL_LABEL )
            val iconOld    = pOld.get( COL_ICON )
            deleteChild( node, pOld )
            map.get( info.parentID ).map( pParent => {
               val pNew = createChild( node, pParent, info )
               pNew.set( COL_LABEL, labelOld )
               pNew.set( COL_ICON, iconOld )
               val viNew = vis.getVisualItem( GROUP_TREE, pNew )
               if( viNew != null ) {
                  startPos.foreach( p => { viNew.setX( p.getX ); viNew.setY( p.getY )})
               }
            })
            vis.run( ACTION_LAYOUT )
         }
      })
   }


   private def nlPauseChild( node: Node, onOff: Boolean ) {
      map.get( node.id ).foreach( pNode => {
         vis.synchronized {
            stopAnimation
            val vi = vis.getVisualItem( GROUP_NODES, pNode )
            if( vi != null ) {
               if( onOff) {
                  setPaused.addTuple( vi )
               } else {
                  setPaused.removeTuple( vi )
               }
               vis.run( ACTION_COLOR )
            }
         }
      })
   }

   private def nlClear {
      vis.synchronized {
         stopAnimation
         setPaused.clear
         val r = t.getRoot

         val c = r.children
         while( c.hasNext ) {
            t.removeChild( c.next.asInstanceOf[ PNode ])
         }
         map   = IntMap( 0 -> r )
         vis.run( ACTION_LAYOUT )
      }
   }
      
   private def initPosAndAnimate( pNode: PNode ) {
      val pParent = pNode.get( PARENT ).asInstanceOf[ PNode ]
      if( pParent != null ) {
         val vi   = vis.getVisualItem( GROUP_TREE, pNode )
         val vip  = vis.getVisualItem( GROUP_TREE, pParent )
         if( vi != null && vip != null ) {
            vi.setX( vip.getX )
            vi.setY( vip.getY )
         }
      }
      vis.run( ACTION_LAYOUT )
   }

   private def stopAnimation {
//      vis.cancel( ACTION_COLOR )
      vis.cancel( ACTION_COLOR_ANIM )
//      vis.cancel( ACTION_LAYOUT )
      vis.cancel( ACTION_LAYOUT_ANIM )
   }

	def makeWindow: JFrame = {
		val frame = new JFrame( "Nodes (" + server.name + ")" )
//		frame.setResizable( false )
		frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )
		frame.getContentPane.add( this )
		frame.pack()
//		frame.setVisible( true )
		frame
	}
}
