import BuildSourcesClass.*
import java.lang.*

name := "trailing-comma-test-project-1"

version := "0.1"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "1",
  "TrailingCommaMarker"
)
