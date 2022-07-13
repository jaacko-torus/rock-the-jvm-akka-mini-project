val deps = new {
  val akkaHttp = "10.2.9"
  val akka     = "2.6.8"
  val circe    = "0.14.2"
}

lazy val root = project
  .in(file("."))
  .settings(
    organization := "com.jaackotorus.bank",
    name         := "scala3",
    version      := "0.1.0",
    scalaVersion := "3.1.3",
    libraryDependencies ++= {
      val scala3 = Nil /*Seq(
        "io.circe" %% "circe-core"    % deps.circe,
        "io.circe" %% "circe-generic" % deps.circe,
        "io.circe" %% "circe-parser"  % deps.circe
      )*/
      val scala2 = Seq(
        "io.circe"           %% "circe-core"                    % deps.circe,
        "io.circe"           %% "circe-generic"                 % deps.circe,
        "io.circe"           %% "circe-parser"                  % deps.circe,
        "com.typesafe.akka"  %% "akka-actor-typed"              % deps.akka,
        "com.typesafe.akka"  %% "akka-stream"                   % deps.akka,
        "com.typesafe.akka"  %% "akka-persistence-typed"        % deps.akka,
        "com.typesafe.akka"  %% "akka-actor-typed"              % deps.akka,
        "com.typesafe.akka"  %% "akka-http"                     % deps.akkaHttp,
        "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "3.0.4",
        "de.heikoseeberger"  %% "akka-http-circe"               % "1.39.2"
      ).map(_.cross(CrossVersion.for3Use2_13))
      val java = Seq(
        "com.datastax.oss" % "java-driver-core" % "4.10.0",
        "ch.qos.logback"   % "logback-classic"  % "1.2.9"
      )
      scala3 ++ scala2 ++ java
    }
  )
