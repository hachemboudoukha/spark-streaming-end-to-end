ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "spark_streaming",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.0",
      "org.apache.spark" %% "spark-sql" % "3.5.0",
      "org.apache.spark" %% "spark-streaming" % "3.5.0",
      "org.apache.spark" %% "spark-streaming-kafka-0-10" % "3.5.0",
      "org.apache.kafka" % "kafka-clients" % "3.6.0",
      "org.postgresql" % "postgresql" % "42.7.1"
    )
  )
