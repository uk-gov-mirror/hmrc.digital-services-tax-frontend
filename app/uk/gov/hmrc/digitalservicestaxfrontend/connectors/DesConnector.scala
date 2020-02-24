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

package uk.gov.hmrc.digitalservicestaxfrontend.connectors

import java.net.URLEncoder.encode
import java.time.{Clock, LocalDate}

import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.libs.json.{Json, OWrites}
import play.api.{Logger, Mode}

import scala.concurrent.{ExecutionContext, Future}

class DesConnector(
  val http: HttpClient,
  val mode: Mode,
  servicesConfig: ServicesConfig,
  auditing: AuditConnector
)(implicit clock: Clock, executionContext: ExecutionContext)
  extends DesHelpers(servicesConfig) with OptionHttpReads {

  val desURL: String = servicesConfig.baseUrl("des")
  val serviceURL: String = "soft-drinks"

  // DES return 503 in the event of no subscription for the UTR, we are expected to treat as 404, hence this override
  implicit override def readOptionOf[P](implicit rds: HttpReads[P]): HttpReads[Option[P]] = new HttpReads[Option[P]] {
    def read(method: String, url: String, response: HttpResponse): Option[P] = response.status match {
      case 204 | 404 | 503 | 403 => None
      case 429 =>
        Logger.error("[RATE LIMITED] Received 429 from DES - converting to 503")
        throw Upstream5xxResponse("429 received from DES - converted to 503", 429, 503)
      case _ => Some(rds.read(method, url, response))
    }
  }

}
