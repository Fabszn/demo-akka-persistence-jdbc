/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
name := "demo-akka-persistence-jdbc"

organization := "com.github.dnvriend"

version := "1.0.0"

scalaVersion := "2.11.7"

resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

libraryDependencies ++= {
  val akkaVersion = "2.4.1"
  val akkaStreamAndHttpVersion = "2.0.2"
  val slickVersion = "3.1.1" 
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence-query-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamAndHttpVersion,
    "com.github.dnvriend" %% "akka-persistence-jdbc" % "2.1.1",
    "org.postgresql" % "postgresql" % "9.4-1206-jdbc42",
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,    
    "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaStreamAndHttpVersion % Test,
    "org.scalatest" %% "scalatest" % "2.2.4" % Test
  )
}

fork in Test := true

javaOptions in Test ++= Seq("-Xms30m","-Xmx30m")

parallelExecution in Test := false

licenses +=("Apache-2.0", url("http://opensource.org/licenses/apache2.0.php"))

// enable scala code formatting //
import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)

// enable updating file headers //
import de.heikoseeberger.sbtheader.license.Apache2_0

headers := Map(
  "scala" -> Apache2_0("2016", "Dennis Vriend"),
  "conf" -> Apache2_0("2016", "Dennis Vriend", "#")
)

enablePlugins(AutomateHeaderPlugin)

import spray.revolver.RevolverPlugin.Revolver
Revolver.settings

Revolver.enableDebugging(port = 5050, suspend = false)

mainClass in Revolver.reStart := Some("com.github.dnvriend.Launch")