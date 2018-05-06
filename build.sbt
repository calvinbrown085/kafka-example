val Http4sVersion = "0.18.9"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"

lazy val `kafka-producer` = (project in file("kafka-producer"))
  .enablePlugins(DockerPlugin)
  .settings(
    organization := "com.calvin",
    name := "kafka-example",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.5",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"     %% "specs2-core"          % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.github.pureconfig" %% "pureconfig"       % "0.9.1",
      "org.apache.kafka" % "kafka-clients" % "1.1.0"
    )
  )

lazy val `kafka-consumer` = (project in file("kafka-consumer"))
  .enablePlugins(DockerPlugin)
  .settings(
    organization := "com.calvin",
    name := "kafka-consumer",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.5",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"     %% "specs2-core"          % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.github.pureconfig" %% "pureconfig"       % "0.9.1",
      "org.apache.kafka" % "kafka-clients" % "1.1.0"
    )
  )


val dockerF = dockerfile in docker := {
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (managedClasspath in Compile).value
  val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
  val jarTarget = s"/app/${jarFile.getName}"
  // Make a colon separated classpath with the JAR file
  val classpathString = classpath.files.map("/app/" + _.getName)
      .mkString(":") + ":" + jarTarget

  new Dockerfile {
    // Base image
    from("openjdk:8-jre")
    // Add all files on the classpath
    add(classpath.files, "/app/")
    // Add the JAR file
    add(jarFile, jarTarget)
    // On launch run Java with the classpath and the main class
    entryPoint("java", "-cp", classpathString, mainclass)
  }
}

lazy val root = project.in(file("."))
  .settings(name := "kafka-example-root")
  .aggregate(
    `kafka-producer`, `kafka-consumer`
  )
