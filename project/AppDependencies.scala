import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val uniformVersion = "4.10.0"

  val compile = Seq(

    "uk.gov.hmrc"             %% "govuk-template"           % "5.54.0-play-26",
    "uk.gov.hmrc"             %% "play-ui"                  % "8.9.0-play-26",
    "com.beachape"            %% "enumeratum-play-json"     % "1.6.0",
    "com.luketebbs.uniform"   %% "interpreter-play26"       % uniformVersion,
    "uk.gov.hmrc"             %% "mongo-caching"            % "6.12.0-play-26",
    "org.reactivemongo"       %% "play2-reactivemongo"      % "0.18.6-play26", // TODO fix /home/luke/hmrc/digital-services-tax-frontend/app/uk/gov/hmrc/digitalservicestax/connectors/MongoPersistence.scala:85:21 for v0.20.10 upgrade
    "uk.gov.hmrc"             %% "auth-client"              % "3.0.0-play-26",
    "uk.gov.hmrc"             %% "play-language"            % "4.2.0-play-26",
    "commons-validator"       % "commons-validator"         % "1.6",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.7.0",
    "fr.marcwrobel"           % "jbanking"                  % "2.0.0"
  )

  val test = Seq(
    "org.scalatest"             %% "scalatest"                % "3.0.8"                 % Test,
    "org.jsoup"                 %  "jsoup"                    % "1.13.1"                % Test,
    "com.typesafe.play"         %% "play-test"                % current                 % Test,
    "org.scalacheck"            %% "scalacheck"               % "1.14.3"                % Test,
    "uk.gov.hmrc"               %% "stub-data-generator"      % "0.5.3"                 % Test,
    "io.chrisdavenport"         %% "cats-scalacheck"          % "0.2.0"                 % Test,
    "com.beachape"              %% "enumeratum-scalacheck"    % "1.6.0"                 % Test,
    "wolfendale"                %% "scalacheck-gen-regexp"    % "0.1.2"                 % Test,
    "com.outworkers"            %% "util-samplers"            % "0.57.0"                % Test,
    "com.github.tomakehurst"    %  "wiremock-jre8"            % "2.25.1"                % Test,
    "uk.gov.hmrc"               %% "reactivemongo-test"       % "4.19.0-play-26"        % Test,
    "org.pegdown"               %  "pegdown"                  % "1.6.0"                 % Test,
    "org.scalatestplus.play"    %% "scalatestplus-play"       % "3.1.3"                 % Test,
    "com.softwaremill.macwire"  %% "macros"                   % "2.3.3"                 % Test,
    "com.softwaremill.macwire"  %% "macrosakka"               % "2.3.3"                 % Test,
    "com.softwaremill.macwire"  %% "proxy"                    % "2.3.3"                 % Test,
    "com.softwaremill.macwire"  %% "util"                     % "2.3.3"                 % Test
  )

}
