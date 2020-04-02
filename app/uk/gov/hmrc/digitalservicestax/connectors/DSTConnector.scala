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

import play.api.libs.json._
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.http.{HeaderCarrier, OptionHttpReads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

@Singleton
class DSTConnector (
  val http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext, hc: HeaderCarrier)
    extends DSTService[Future] with OptionHttpReads {

  val backendURL: String = servicesConfig.baseUrl("digital-services-tax") + "/digital-services-tax"

  implicit val readsUnit = Reads[Unit] { _ => JsSuccess(()) }

  def submitRegistration(reg: Registration): Future[Unit] =
    http.POST[Registration, Unit](s"$backendURL/registration", reg)

  def submitReturn(period: Period, ret: Return): Future[Unit] = {
    val encodedKey = java.net.URLEncoder.encode(period.key, "UTF-8")
    http.POST[Return, Unit](s"$backendURL/returns/${encodedKey}", ret)
  }

  def lookupCompany(): Future[Option[CompanyRegWrapper]] =
    http.GET[Option[CompanyRegWrapper]](s"$backendURL/lookup-company")

  def lookupCompany(utr: UTR, postcode: Postcode): Future[Option[CompanyRegWrapper]] =
    http.GET[Option[CompanyRegWrapper]](s"$backendURL/lookup-company/$utr/$postcode")

  def lookupRegistration(): Future[Option[Registration]] =
    http.GET[Option[Registration]](s"$backendURL/registration")

  def lookupOutstandingReturns(): Future[Set[Period]] =
    http.GET[List[Period]](s"$backendURL/returns").map{_.toSet}

  private val hodCache =
    controllers.SimpleCaching[(InternalId, String)]()

  def hod(internalId: InternalId)(implicit hc: HeaderCarrier) = {
    val backend = this

    import ltbs.uniform.common.web.{FutureAdapter, GenericWebTell, WebMonad}
    import play.twirl.api.Html

    new DSTService[WebMonad[*, Html]] {
    val fa = FutureAdapter[Html]()
    import fa._

    def lookupCompany(utr: UTR, postcode: Postcode): WebMonad[Option[CompanyRegWrapper],Html] =
      alwaysRerun(hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company-args"), utr, postcode)(
        backend.lookupCompany(utr, postcode)
      ))

    def lookupCompany(): WebMonad[Option[CompanyRegWrapper],Html] =
      alwaysRerun(hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company"))(
        backend.lookupCompany()
      ))

    def lookupOutstandingReturns(): WebMonad[Set[Period],Html] = 
      alwaysRerun(hodCache[Set[Period]]((internalId, "lookup-outstanding-returns"))(
        backend.lookupOutstandingReturns()
      ))

    def lookupRegistration(): WebMonad[Option[Registration],Html] = 
      alwaysRerun(hodCache[Option[Registration]]((internalId, "lookup-reg"))(
        backend.lookupRegistration()
      ))

    def submitRegistration(reg: Registration): WebMonad[Unit,Html] =
      alwaysRerun(backend.submitRegistration(reg))

    def submitReturn(period: Period,ret: Return): WebMonad[Unit,Html] = 
      alwaysRerun(backend.submitReturn(period, ret))

    }
  }

}
