ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.7"
ThisBuild / scalafmtOnCompile := true
ThisBuild / resolvers ++= Seq(
  "Millhouse Bintray" at "http://dl.bintray.com/themillhousegroup/maven"
)

val commonDependencies = Seq(
  "com.themillhousegroup" %% "scoup"          % "0.4.7",
  "org.json4s"            %% "json4s-native"  % "3.6.6",
  "org.json4s"            %% "json4s-jackson" % "3.6.6"
)

lazy val domainLibrary = project
  .in(file("domain-library"))
  .settings(
    version := "0.0.1",
    name := "domain-library",
    libraryDependencies ++= commonDependencies
  )

lazy val chatGateway = project
  .in(file("chat-gateway"))
  .settings(
    version := "0.0.1",
    name := "chat-gateway",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor"              % "2.6.7",
      "com.typesafe.akka" %% "akka-http"               % "10.1.12",
      "com.typesafe.akka" %% "akka-stream"             % "2.6.7",
      "com.typesafe.akka" %% "akka-slf4j"              % "2.6.7",
      "ch.qos.logback"     % "logback-classic"         % "1.2.3",
      "com.rabbitmq"       % "amqp-client"             % "5.9.0",
      "com.google.cloud"   % "google-cloud-dialogflow" % "2.1.0"
    ) ++ commonDependencies
  )
  .dependsOn(domainLibrary)
