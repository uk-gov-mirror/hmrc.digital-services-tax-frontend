/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestax
package connectors

import javax.inject.{Inject, Singleton}
import data._, BackendAndFrontendJson._
import uk.gov.hmrc.http.{controllers => _, _}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.AnyContent
import actions.AuthorisedRequest
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider

@Singleton
class DSTConnector @Inject() (
  val http: HttpClient,
  servicesConfig: ServicesConfig,
  ec: ExecutionContext
) extends OptionHttpReads with FrontendHeaderCarrierProvider {

  implicit val eci = ec

  private val hodCache =
    controllers.SimpleCaching[(InternalId, String)]()

  val backendURL: String = servicesConfig.baseUrl("digital-services-tax") + "/digital-services-tax"

  def uncached(implicit request: AuthorisedRequest[AnyContent]) = new DSTService[Future] {

    def submitRegistration(reg: Registration): Future[Unit] =
      http.POST[Registration, Unit](s"$backendURL/registration", reg)

    def submitReturn(period: Period, ret: Return): Future[Unit] = {
      val encodedKey = java.net.URLEncoder.encode(period.key, "UTF-8")
      http.POST[Return, Unit](s"$backendURL/returns/${encodedKey}", ret)
    }

    def lookupCompany(): Future[Option[CompanyRegWrapper]] =
      http.GET[Option[CompanyRegWrapper]](s"$backendURL/lookup-company")

    def lookupCompany(utr: UTR, postcode: Postcode): Future[Option[CompanyRegWrapper]] = {
      val escaped = postcode.replaceAll("\\s+", "")
      http.GET[Option[CompanyRegWrapper]](s"$backendURL/lookup-company/$utr/$escaped")
    }

    def lookupRegistration(): Future[Option[Registration]] =
      http.GET[Option[Registration]](s"$backendURL/registration")

    def lookupOutstandingReturns(): Future[Set[Period]] =
      http.GET[List[Period]](s"$backendURL/returns").map{_.toSet}

  }

  def cached(implicit request: AuthorisedRequest[AnyContent]) = new DSTService[Future] {
    val internalId = request.internalId

    def lookupCompany(utr: UTR, postcode: Postcode): Future[Option[CompanyRegWrapper]] =
      hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company-args"), utr, postcode)(
        uncached.lookupCompany(utr, postcode)
      )

    def lookupCompany(): Future[Option[CompanyRegWrapper]] =
      hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company"))(
        uncached.lookupCompany()
      )

    def lookupOutstandingReturns(): Future[Set[Period]] =
      hodCache[Set[Period]]((internalId, "lookup-outstanding-returns"))(
        uncached.lookupOutstandingReturns()
      )

    def lookupRegistration(): Future[Option[Registration]] =
      hodCache[Option[Registration]]((internalId, "lookup-reg"))(
        uncached.lookupRegistration()
      )

    def submitRegistration(reg: Registration): Future[Unit] = {
      import cats.implicits._
      uncached.submitRegistration(reg) >>
      hodCache.invalidate((internalId, "lookup-reg")).pure[Future]
    }

    def submitReturn(period: Period,ret: Return): Future[Unit] = {
      import cats.implicits._      
      uncached.submitReturn(period, ret) >> 
      hodCache.invalidate((internalId, "lookup-outstanding-returns")).pure[Future]
    }
  }

  val fa = ltbs.uniform.common.web.FutureAdapter[play.twirl.api.Html]()
  def uniformCached(implicit request: AuthorisedRequest[AnyContent]) = 
    cached.natTransform(fa.alwaysRerun)

  def uniformUncached(implicit request: AuthorisedRequest[AnyContent]) = 
    uncached.natTransform(fa.alwaysRerun)


}
