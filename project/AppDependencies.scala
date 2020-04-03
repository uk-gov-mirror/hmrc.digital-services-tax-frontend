import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val uniformVersion = "4.10.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "govuk-template"           % "5.52.0-play-26",
    "uk.gov.hmrc"             %% "play-ui"                  % "8.8.0-play-26",
    "com.beachape"            %% "enumeratum-play-json"     % "1.5.13",
    "com.luketebbs.uniform"   %% "interpreter-play26"       % uniformVersion,
    "uk.gov.hmrc"             %% "mongo-caching"            % "6.8.0-play-26",
    "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.26.0-play-26",
    "org.reactivemongo"       %% "play2-reactivemongo"      % "0.18.6-play26",
    "uk.gov.hmrc"             %% "auth-client"              % "2.35.0-play-26",
    "uk.gov.hmrc"             %% "play-language"            % "4.2.0-play-26",
    "commons-validator"       % "commons-validator"         % "1.6",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.5.0",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.4.4" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.4.4" % Provided cross CrossVersion.full,
    "fr.marcwrobel"           % "jbanking"                  % "2.0.0"
  )

  val test = Seq(
    "uk.gov.hmrc"               %% "bootstrap-play-26"        % "1.5.0" % Test classifier "tests",
    "org.scalatest"             %% "scalatest"                % "3.0.8"                 % Test,
    "org.jsoup"                 %  "jsoup"                    % "1.10.2"                % Test,
    "com.typesafe.play"         %% "play-test"                % current                 % Test,
    "org.scalacheck"            %% "scalacheck"               % "1.14.0"                % Test,
    "uk.gov.hmrc"               %% "stub-data-generator"      % "0.5.3"                 % Test,
    "org.typelevel"             %% "cats-core"                % "2.0.0"                 % Test,
    "io.chrisdavenport"         %% "cats-scalacheck"          % "0.2.0"                 % Test,
    "com.beachape"              %% "enumeratum-scalacheck"    % "1.5.16"                % Test,
    "wolfendale"                %% "scalacheck-gen-regexp"    % "0.1.2"                 % Test,
    "com.outworkers"            %% "util-samplers"            % "0.57.0"                % Test,
    "com.github.tomakehurst"    %  "wiremock-jre8"            % "2.25.1"                % Test,
    "uk.gov.hmrc"               %% "reactivemongo-test"       % "4.19.0-play-26"        % Test,
    "org.pegdown"               %  "pegdown"                  % "1.6.0"                 % Test,
    "org.scalatestplus.play"    %% "scalatestplus-play"       % "3.1.2"                 % Test,
    "com.softwaremill.macwire"  %% "macros"                   % "2.3.3"                 % Test,
    "com.softwaremill.macwire"  %% "macrosakka"               % "2.3.3"                 % Test,
    "com.softwaremill.macwire"  %% "proxy"                    % "2.3.3"                 % Test,
    "com.softwaremill.macwire"  %% "util"                     % "2.3.3"                 % Test
  )

}
