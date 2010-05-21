/*
 *  ScalaInterpreterFrame.scala
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

import java.awt.GraphicsEnvironment
import de.sciss.scalainterpreter.{ LogPane, ScalaInterpreterPane }
import de.sciss.synth.Server
import tools.nsc.Interpreter
import java.io.PrintStream
import javax.swing.{ JFrame, JSplitPane, SwingConstants, WindowConstants }

/**
 *    @version 0.12, 21-May-10
 */
class ScalaInterpreterFrame( s: Server, ntp: NodeTreePanel )
extends JFrame( "Scala Interpreter" ) {

   // ---- constructor ----
   {
      val cp = getContentPane
      val ip = new ScalaInterpreterPane

      ip.initialText = ip.initialText +
"""
s.options.programPath.value = ".../scsynth"
s.boot

// analog bubbles
val x = {
    val f = LFSaw.kr(0.4).madd(24, LFSaw.kr(List(8, 7.23)).madd(3, 80)).midicps // glissando function
    CombN.ar(SinOsc.ar(f)*0.04, 0.2, 0.2, 4) // echoing sine wave
}.play

x.release( 10 )

val df = SynthDef("AnalogBubbles") {
    val f1 = "freq1".kr(0.4)
    val f2 = "freq2".kr(8)
    val d  = "detune".kr(0.90375)
    val f = LFSaw.ar(f1).madd(24, LFSaw.ar(List(f2, f2*d)).madd(3, 80)).midicps // glissando function
    val x = CombN.ar(SinOsc.ar(f)*0.04, 0.2, 0.2, 4) // echoing sine wave
    Out.ar( 0, x )
}
val x = df.play( args = List( "freq2" -> 222.2 ))
x.set( "freq1" -> 0.1 )
x.set( "detune" -> 0.44 )

x.moveAfter( s )  // note: s expands to s.defaultGroup
x.moveToHead( s )

x.run( false )
x.run( true )

s.freeAll

viewDef( df )
"""

      ip.initialCode = Some(
"""
import math._
import de.sciss.scalaosc.{ OSCBundle, OSCMessage, OSCPacket }
import de.sciss.synth._
import de.sciss.synth.SC._
import de.sciss.synth.swing.SynthGraphPanel._
import de.sciss.synth.io._
import de.sciss.synth.osc._
import de.sciss.synth.ugen._
"""
      )

      ip.bindingsCreator = Some( (in: Interpreter ) => {
         in.bind( "s", classOf[ Server ].getName, s )
//         in.bind( "ntp", classOf[ NodeTreePanel ].getName, ntp )
//         in.bind( "in", classOf[ Interpreter ].getName, in )
      })

      val lp = new LogPane
      lp.init
      ip.out = Some( lp.writer )
      Console.setOut( lp.outputStream )
      Console.setErr( lp.outputStream )
      System.setErr( new PrintStream( lp.outputStream ))

      ip.init
      val sp = new JSplitPane( SwingConstants.HORIZONTAL )
      sp.setTopComponent( ip )
      sp.setBottomComponent( lp )
      cp.add( sp )
      val b = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds
      setSize( b.width / 2, b.height * 7 / 8 )
      sp.setDividerLocation( b.height * 2 / 3 )
      setLocationRelativeTo( null )
//      setLocation( x, getY )
      setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
//      setVisible( true )
   }
}