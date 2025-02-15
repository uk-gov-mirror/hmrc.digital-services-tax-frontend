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

package uk.gov.hmrc.digitalservicestax.connectors

import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import BackendAndFrontendJson._
import scala.concurrent.{ExecutionContext, Future}

class DSTConnector (
  val http: HttpClient,
  servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext, hc: HeaderCarrier)
    extends DSTService[Future] with OptionHttpReads {

  val backendURL: String = servicesConfig.baseUrl("digital-services-tax") + "/digital-services-tax"

  def submitRegistration(reg: Registration): Future[Unit] =
    http.POST[Registration, Unit](s"$backendURL/registration", reg)

  def submitReturn(period: Period, ret: Return): Future[Unit] = {
    val encodedKey = java.net.URLEncoder.encode(period.key, "UTF-8")
    http.POST[Return, Unit](s"$backendURL/returns/$encodedKey", ret)
  }

  def lookupCompany(): Future[Option[CompanyRegWrapper]] =
    http.GET[Option[CompanyRegWrapper]](s"$backendURL/lookup-company")

  def lookupCompany(utr: UTR, postcode: Postcode): Future[Option[CompanyRegWrapper]] =
    http.GET[Option[CompanyRegWrapper]](s"$backendURL/lookup-company/$utr/$postcode")

  def lookupRegistration(): Future[Option[Registration]] =
    http.GET[Option[Registration]](s"$backendURL/registration")

  def lookupOutstandingReturns(): Future[Set[Period]] =
    http.GET[List[Period]](s"$backendURL/returns/outstanding").map{_.toSet}

  def lookupAmendableReturns(): Future[Set[Period]] =
    http.GET[List[Period]](s"$backendURL/returns/amendable").map{_.toSet}

  def lookupAllReturns(): Future[Set[Period]] =
    http.GET[List[Period]](s"$backendURL/returns/all").map{_.toSet}

  def lookupFinancialDetails(): Future[List[FinancialTransaction]] = {
    http.GET[List[FinancialTransaction]](s"$backendURL/financial-transactions").recoverWith {
      case _: NotFoundException => Future.successful(Nil)
      case _ => throw MicroServiceConnectionException("Invalid response from financial transactions microservice.")
    }
  }

  case class MicroServiceConnectionException(msg: String) extends Exception(msg)

}
