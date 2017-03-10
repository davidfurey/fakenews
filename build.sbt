name := """score"""
organization := "com.gu.hackday"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies +=  "com.typesafe.akka" %% "akka-agent" % "2.4.14"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.gu.hackday.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.gu.hackday.binders._"
