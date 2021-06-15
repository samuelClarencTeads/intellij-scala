import BuildSourcesClass.*
import java.lang.*

name := "trailing-comma-test-project-2"

version := "0.1"

scalaVersion := "2.12.2"

scalacOptions ++= Seq(
  "1",
  "TrailingCommaMarker"
)
