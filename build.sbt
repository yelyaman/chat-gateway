ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.12"
ThisBuild / scalafmtOnCompile := true
ThisBuild / resolvers ++= Seq(
  "Millhouse Bintray" at "http://dl.bintray.com/themillhousegroup/maven"
)

val commonDependencies = Seq(
  "com.themillhousegroup" %% "scoup"          % "0.4.7",
  "org.json4s"            %% "json4s-native"  % "3.6.6",
  "org.json4s"            %% "json4s-jackson" % "3.6.6",
  "com.rabbitmq" % "amqp-client" % "5.9.0"
)

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.7",
  "com.typesafe.akka" %% "akka-http"   % "10.1.12",
  "com.typesafe.akka" %% "akka-stream" % "2.6.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.7",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
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
      "com.google.cloud"   % "google-cloud-dialogflow" % "2.1.0"
    ) ++ commonDependencies ++ akkaDependencies
  )
  .dependsOn(domainLibrary)

lazy val httpAdapter = project
  .in(file("http-adapter"))
  .settings(
    version := "0.0.1",
    name := "http-adapter",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp" %% "core" % "1.6.4",
      "de.heikoseeberger" %% "akka-http-json4s" % "1.31.0",
      "kz.coders" %% "domain-library" %  "0.0.1",
    ) ++ commonDependencies ++ akkaDependencies
  )
  .dependsOn(domainLibrary)

lazy val telegramService = project
  .in(file("telegram-service"))
  .settings(
    version := "0.0.1",
    name := "githuber-bot",
    libraryDependencies ++= Seq(
      "com.bot4s" %% "telegram-core" % "4.4.0-RC2",
      "com.softwaremill.sttp" %% "core" % "1.6.4",
      "de.heikoseeberger" %% "akka-http-json4s" % "1.31.0",
      "kz.coders" %% "domain-library" %  "0.0.1"
    ) ++ commonDependencies ++ akkaDependencies
  )
  .dependsOn(domainLibrary)
