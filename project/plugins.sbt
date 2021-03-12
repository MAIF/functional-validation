resolvers += Resolver.jcenterRepo
resolvers += Opts.resolver.sonatypeSnapshots

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6") // Apache 2.0

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0") // Apache 2.0

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12") // Apache 2.0

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.0") // Apache 2.0

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.1.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7-SNAPSHOT")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.6")
