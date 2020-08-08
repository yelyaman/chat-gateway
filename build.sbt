
name := "chat-gateway"

version := "0.0.1"

scalaVersion := "2.12.7"

resolvers ++= Seq(
  "Millhouse Bintray"  at "http://dl.bintray.com/themillhousegroup/maven"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.7",
  "com.typesafe.akka" %% "akka-http"   % "10.1.12",
  "com.typesafe.akka" %% "akka-stream" % "2.6.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.7",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "kz.coders" %% "domain-library" % "0.0.1",
  "org.json4s" %% "json4s-native"  % "3.6.6",
  "org.json4s" %% "json4s-jackson" % "3.6.6",
  "com.rabbitmq" % "amqp-client" % "5.9.0",
  "com.themillhousegroup" %% "scoup" % "0.4.7",
  "com.google.cloud" % "google-cloud-dialogflow" % "2.1.0"
)

scalafmtOnCompile := true