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

package uk.gov.hmrc.digitalservicestax.connectors

import uk.gov.hmrc.digitalservicestax.data._
import BackendAndFrontendJson._
import java.net.URLEncoder.encode
import java.time.{Clock, LocalDate}

import javax.inject.Inject
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.libs.json.{JsSuccess, Json, OWrites, Reads}
import play.api.{Logger, Mode}

import scala.concurrent.{ExecutionContext, Future}

class DSTConnector @Inject()(
  val http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends DSTService[Future] with OptionHttpReads {

  val backendURL: String = servicesConfig.baseUrl("digital-services-tax") + "/digital-services-tax"

  implicit val readsUnit = Reads[Unit] { _ => JsSuccess(()) }

  def submitRegistration(reg: Registration)(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[Registration, Unit](s"$backendURL/registration", reg)

  def submitReturn(period: Period, ret: Return)(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[Return, Unit](s"$backendURL/returns/${period.start.getYear}", ret)

  def lookupCompany()(implicit hc: HeaderCarrier): Future[Option[Company]] =
    http.GET[Option[Company]](s"$backendURL/lookup-company")

  def lookupCompany(utr: UTR, postcode: Postcode)(implicit hc: HeaderCarrier): Future[Option[Company]] =
    http.GET[Option[Company]](s"$backendURL/lookup-company/$utr/$postcode")

  def lookupRegistration()(implicit hc: HeaderCarrier): Future[Option[Registration]] =
    http.GET[Option[Registration]](s"$backendURL/registration")

  def lookupOutstandingReturns()(implicit hc: HeaderCarrier): Future[Set[Period]] = ???
//    http.GET[List[Period]](s"$backendURL/returns").map{_.toSet}

}
