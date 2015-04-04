name := "fp-in-scala"

scalaVersion := "2.11.0"

resolvers ++= Seq(
    "Sonatype Releases"  at "http://oss.sonatype.org/content/repositories/releases"
)

val scalazV = "7.0.6"

libraryDependencies ++= Seq(
    "org.scalaz"        %%	"scalaz-core"				% scalazV,
    "com.chuusai" %% "shapeless" % "2.2.0-RC3",
    "org.scalatest"		%%	"scalatest"					% "2.2.0"   % "test"
)