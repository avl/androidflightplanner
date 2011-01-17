#!/bin/bash
cd bin

export JUNIT4=/home/anders/saker/junit-4.9b1.jar

java -ea -classpath .:$JUNIT4:/home/anders/Downloads/jmock/jmock-2.5.1/jmock-2.5.1.jar:/home/anders/Downloads/jmock/hamcrest-core-1.1.jar:/home/anders/Downloads/jmock/hamcrest-library-1.1.jar:/home/anders/workspace/flightplanner/bin/ org.junit.runner.JUnitCore se.flightplannertest.map3d.TestPlayfield

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3d.TestAirspaceDrawer

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3d.TestBitOps.java

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3d.TestTriangulator

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3d.TestThing

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3d.TestTriangleStore

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3d.TestVertexStore

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3Lodcalc

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.map3d.TestElevMap

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestVector

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestAirspaceSerialization

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestLine

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestPolygon

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestBspTree

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestBBTree

java -ea -classpath .:$JUNIT4:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestAirspaceTree

java -ea -classpath .:$JUNIT4:\
/home/anders/workspace/flightplanner/bin/:\
/home/anders/workspace/flightplannertest/json/ \
org.junit.runner.JUnitCore se.flightplannertest.TestAirspaceSigPointTree

