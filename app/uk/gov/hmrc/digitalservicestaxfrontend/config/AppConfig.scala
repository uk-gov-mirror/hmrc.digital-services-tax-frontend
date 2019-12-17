/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.digitalservicestaxfrontend.config

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.Duration

@Singleton
class AppConfig @Inject()(val config: Configuration, servicesConfig: ServicesConfig, environment: Environment) {

  protected def mode: Mode = environment.mode

  private def loadConfig(key: String) = config.get[String](key)

  private val contactBaseUrl = servicesConfig.baseUrl("contact-frontend")
  private val assetsUrl         = loadConfig("assets.url")
  private val serviceIdentifier = "MyService"

  lazy val assetsPrefix: String   = assetsUrl + loadConfig("assets.version")
  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String  = loadConfig(s"google-analytics.host")

  lazy val appName: String = loadConfig("appName")
  private lazy val companyAuthFrontend = servicesConfig.getConfString("company-auth.url", "")
  private lazy val companyAuthSignInPath = servicesConfig.getConfString("company-auth.sign-in-path", "")
  private lazy val companyAuthSignOutPath = servicesConfig.getConfString("company-auth.sign-out-path", "")
  lazy val ggLoginUrl: String = s"$companyAuthFrontend$companyAuthSignInPath"

  lazy val reportAProblemPartialUrl: String = s"$contactBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifier"
  lazy val reportAProblemNonJSUrl: String   = s"$contactBaseUrl/contact/problem_reports_nonjs?service=$serviceIdentifier"

  val mongoShortLivedStoreExpireAfter: Duration = servicesConfig.getDuration("mongodb.shortLivedCache.expireAfter")
  val mongoJourneyStoreExpireAfter: Duration    = servicesConfig.getDuration("mongodb.journeyStore.expireAfter")

  lazy val dstIndexPage: String = loadConfig("dst-index-page-url")
  lazy val signOutDstUrl: String = s"$companyAuthFrontend$companyAuthSignOutPath?continue=$feedbackSurveyUrl"
  lazy val feedbackSurveyUrl: String = loadConfig("microservice.services.feedback-survey.url")
}
