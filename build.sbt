name := """TOTEM"""
organization := "com.crag"
maintainer := "gonzalo.vera@cragenomica.es"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.10"
libraryDependencies += "com.googlecode.json-simple" % "json-simple" % "1.1.1"
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.5"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"

// libraries installed with R. Update accordingly with your installation
libraryDependencies += "org.rosuda" % "JRI" % "1.0" from "file:///Library/Frameworks/R.framework/Versions/3.6/Resources/library/rJava/jri/JRI.jar"
libraryDependencies += "org.rosuda.REngine" % "REngine" % "2.1.0"
libraryDependencies += "org.rosuda.REngine.JRI" % "JRIEngine" % "1.0" from "file:///Library/Frameworks/R.framework/Versions/3.6/Resources/library/rJava/jri/JRIEngine.jar"

