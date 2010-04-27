---- ScalaCollider-Swing ----
A Swing GUI front-end for ScalaCollider.
Copyright (c) 2008-2010 Hanns Holger Rutz. All rights reserved.
Licensed under the GNU General Public License v2 (see "licenses" folder).

PREREQUISITES

Scala 2.8 (RC1) / Java 1.6 / SuperCollider 3.3.1+ / ScalaCollider / ScalaOSC / ScalaInterpreterPane

INSTALLATION

The following libraries have to be downloaded and installed into the "libraries" folder:

    - ScalaOSC.jar -> http://github.com/Sciss/ScalaOSC
    - ScalaCollider.jar -> http://github.com/Sciss/ScalaCollider
    - ScalaInterpreterPane.jar -> http://github.com/Sciss/ScalaInterpreterPane
    - prefuse.jar -> http://prefuse.org

COMPILATION

Compile using the included IntelliJ IDEA 9 CE project. The ant build script can be used to create a
plain jar (target "jar"), a full jar including all libraries and scala itself, in order to have a
double-clickable standalone jar (target "standalone"), and to update the included Mac OS X
application bundle (target "osx-app"). They require that the shell environment variable SCALA_HOME
has been properly set. Example:

$ ant standalone
$ java -jar ScalaColliderSwing-full.jar
