package de.sciss.synth.swing;

import prefuse.data.Table;

/*
 *  Helps with a java interop problem of scala 2.8 rc1 -
 *  scalac refuses to call methods with non-generic Class argument
 */
public class PrefuseHelper {
    public static void addColumn( Table tab, String name, Class<?> clz ) {
        tab.addColumn( name, clz );
    }
}
