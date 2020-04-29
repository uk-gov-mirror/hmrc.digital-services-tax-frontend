import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "digital-services-tax-frontend"

PlayKeys.playDefaultPort := 8740

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*views.*;.*prod.*;.*test.*;app.routes;testOnlyDoNotUseInAppConf;prod.*;controllers.javascript;com.kenshoo.play.metrics.*;",
    ScoverageKeys.coverageExcludedFiles := "<empty>;.*BuildInfo.*;.*Routes.*;testOnlyDoNotUseInAppConf;",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

scalacOptions in ThisBuild += "-Ypartial-unification"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Ypartial-unification"
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(
    resolvers += Resolver.jcenterRepo,
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    scalacOptions -= "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    TwirlKeys.templateImports ++= Seq(
      "ltbs.uniform._",
      "ltbs.uniform.interpreters.playframework._"
    )
  ).settings(scoverageSettings)

libraryDependencies ++= Seq(
  ws
)

routesImport += "uk.gov.hmrc.digitalservicestax.data._"
TwirlKeys.templateImports += "uk.gov.hmrc.digitalservicestax.data._"
