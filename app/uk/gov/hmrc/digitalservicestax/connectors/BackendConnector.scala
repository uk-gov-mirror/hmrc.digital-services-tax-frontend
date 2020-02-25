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

import uk.gov.hmrc.digitalservicestax.data._, BackendAndFrontendJson._

import java.net.URLEncoder.encode
import java.time.{Clock, LocalDate}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.libs.json.{Json, OWrites, Reads, JsSuccess}
import play.api.{Logger, Mode}

import scala.concurrent.{ExecutionContext, Future}

class BackendConnector(
  val http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext, hc: HeaderCarrier)
    extends BackendService[Future] with OptionHttpReads {

  val backendURL: String = servicesConfig.baseUrl("digital-services-tax")

  implicit val readsUnit = Reads[Unit] { _ => JsSuccess(()) }

  def submitRegistration(reg: Registration): Future[Unit] = 
    http.POST[Registration, Unit](s"$backendURL/register", reg)

  def submitReturn(ret: Return): Future[Unit] = 
    http.POST[Return, Unit](s"$backendURL/return", ret)

  def lookup(utr: UTR, postcode: Postcode): Future[Option[Company]] =
    http.GET[Option[Company]](s"$backendURL/lookup-company/$utr/$postcode")

  def matchedCompany(): Future[Option[Company]] =
    http.GET[Option[Company]](s"$backendURL/lookup-company")    

}
