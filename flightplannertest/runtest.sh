#!/bin/bash
cd bin
java -classpath .:/home/anders/junit4.8.2/junit-4.8.2.jar:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestVector

java -classpath .:/home/anders/junit4.8.2/junit-4.8.2.jar:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestLine

java -classpath .:/home/anders/junit4.8.2/junit-4.8.2.jar:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestPolygon

java -classpath .:/home/anders/junit4.8.2/junit-4.8.2.jar:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestBspTree

java -classpath .:/home/anders/junit4.8.2/junit-4.8.2.jar:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestBBTree

java -classpath .:/home/anders/junit4.8.2/junit-4.8.2.jar:/home/anders/workspace/flightplanner/bin/ \
    org.junit.runner.JUnitCore se.flightplannertest.TestAirspaceTree

java -classpath .:/home/anders/junit4.8.2/junit-4.8.2.jar:\
/home/anders/workspace/flightplanner/bin/:\
/home/anders/workspace/flightplannertest/json/ \
org.junit.runner.JUnitCore se.flightplannertest.TestAirspaceSigPointTree

