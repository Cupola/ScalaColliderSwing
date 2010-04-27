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

import javax.swing._
import java.awt.GraphicsEnvironment
import de.sciss.scalainterpreter.{ LogPane, ScalaInterpreterPane }
import de.sciss.synth.Server
import tools.nsc.Interpreter

/**
 *    @version 0.11, 11-Apr-10
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
val x = play {
    val f = LFSaw.kr(0.4).madd(24, LFSaw.kr(List(8, 7.23)).madd(3, 80)).midicps // glissando function
    CombN.ar(SinOsc.ar(f)*0.04, 0.2, 0.2, 4) // echoing sine wave
}

x.free

val df = SynthDef("AnalogBubbles") {
    val f1 = "freq1".kr(0.4)
    val f2 = "freq2".kr(8)
    val d  = "detune".kr(0.90375)
    val f = LFSaw.ar(f1).madd(24, LFSaw.ar(List(f2, f2*d)).madd(3, 80)).midicps // glissando function
    val x = CombN.ar(SinOsc.ar(f)*0.04, 0.2, 0.2, 4) // echoing sine wave
    Out.ar( 0, x )
}
val x = df.play()
x.set( "freq1" -> 0.1f )  // todo: should support doubles
x.set( "freq2" -> 222.2f )
x.set( "detune" -> 0.44f )

s.freeAll

x.moveAfter( s )  // note: s expands to s.defaultGroup
x.moveToHead( s )

x.run( false )
x.run( true )
"""

      ip.initialCode = Some(
"""
import math._
import de.sciss.synth._
import de.sciss.synth.SC._
import de.sciss.synth.ugen._
import de.sciss.synth.swing._
"""
      )

      ip.bindingsCreator = Some( (in: Interpreter ) => {
         in.bind( "s", classOf[ Server ].getName, s )
         in.bind( "ntp", classOf[ NodeTreePanel ].getName, ntp )
      })

      val lp = new LogPane
      lp.init
      ip.out = Some( lp.writer )
      Console.setOut( lp.outputStream )
      Console.setErr( lp.outputStream )

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