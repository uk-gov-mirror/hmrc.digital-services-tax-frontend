import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val uniformVersion = "4.9.0"

  val compile = Seq(

    "uk.gov.hmrc"             %% "govuk-template"           % "5.52.0-play-26",
    "uk.gov.hmrc"             %% "play-ui"                  % "8.8.0-play-26",
    "com.beachape"            %% "enumeratum-play-json"     % "1.5.13",
    "com.luketebbs.uniform"   %% "interpreter-play26"       % uniformVersion,
    "uk.gov.hmrc"             %% "mongo-caching"            % "6.8.0-play-26",
    "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.24.0-play-26",
    "org.reactivemongo"       %% "play2-reactivemongo"      % "0.18.6-play26",
    "uk.gov.hmrc"             %% "auth-client"              % "2.35.0-play-26",
    "uk.gov.hmrc"             %% "play-language"            % "4.1.0",
    "commons-validator"       % "commons-validator"         % "1.6",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.5.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.5.0" % Test classifier "tests",
    "org.scalatest"           %% "scalatest"                % "3.0.8"                 % "test",
    "org.jsoup"               %  "jsoup"                    % "1.10.2"                % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0"                % "test",
    "uk.gov.hmrc"             %% "stub-data-generator"      % "0.5.3"                 % "test",
    "org.typelevel"           %% "cats-core"                % "2.0.0"                 % "test",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.2"                 % "test, it"
  )

}
