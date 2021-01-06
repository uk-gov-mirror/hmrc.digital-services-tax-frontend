/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationSpec
import scala.concurrent.duration._

class ConfigTest extends FakeApplicationSpec {

  "should load a non empty assets prefix" in {
    appConfig.assetsPrefix.nonEmpty mustEqual true
  }

  "should load an analytics token from the configuration" in {
    appConfig.analyticsToken.nonEmpty mustEqual true
  }

  "should load an analytics host from the configuration" in {
    appConfig.analyticsHost.nonEmpty mustEqual true
  }

  "should load an app name from the configuration" in {
    appConfig.appName.nonEmpty mustEqual true
  }

  "should load a GG Login URL from the configuration" in {
    appConfig.ggLoginUrl.nonEmpty mustEqual true
  }

  "should load a service name from the configuration" in {
    appConfig.serviceName.nonEmpty mustEqual true
  }

  "should load a partial URL from the configuration" in {
    appConfig.reportAProblemPartialUrl.nonEmpty mustEqual true
  }

  "should load a report JSON partial URL from the configuration" in {
    appConfig.reportAProblemNonJSUrl.nonEmpty mustEqual true
  }

  "should load a MongoDB short lived store duration" in {
    appConfig.mongoShortLivedStoreExpireAfter mustEqual 30.days
  }

  "should load a MongoDB expiry config duration" in {
    appConfig.mongoShortLivedStoreExpireAfter mustEqual 30.days
  }

  "should load a DST index page URL from the configuration" in {
    appConfig.dstIndexPage.nonEmpty mustEqual true
  }

  "should load a DST sign out page URL from the configuration" in {
    appConfig.signOutDstUrl.nonEmpty mustEqual true
  }

  "should load a sign out DST url from the configuration" in {
    appConfig.dstIndexPage.nonEmpty mustEqual true
  }

  "should load a Feedback URL the configuration" in {
    appConfig.feedbackSurveyUrl.nonEmpty mustEqual true
  }

  "should load a beta feedback auth URL the configuration" in {
    appConfig.betaFeedbackUrlAuth.nonEmpty mustEqual true
  }
}
