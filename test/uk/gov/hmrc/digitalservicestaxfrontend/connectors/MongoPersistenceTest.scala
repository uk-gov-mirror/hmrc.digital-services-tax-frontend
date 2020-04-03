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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import play.api.mvc.{AnyContent, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.digitalservicestax.connectors.MongoPersistence
import uk.gov.hmrc.digitalservicestax.data.InternalId
import uk.gov.hmrc.digitalservicestax.actions.AuthorisedRequest
import uk.gov.hmrc.digitalservicestax.util.FakeApplicationSpec

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.digitalservicestax.TestInstances._

import scala.concurrent.Future

class MongoPersistenceTest extends FakeApplicationSpec with ScalaCheckDrivenPropertyChecks {

  val persistence = MongoPersistence[AuthorisedRequest[AnyContent]](
     reactiveMongoApi = reactiveMongoApi,
     collectionName = "test_persistence",
     expireAfter = appConfig.mongoShortLivedStoreExpireAfter,
     useMongoTTL = true
  ) { req =>
    req.internalId
  }

  "should generate a kill date based on a mongo TTL" in {
    info(persistence.killDate.format(DateTimeFormatter.ISO_DATE))
  }

  "should trigger reaver method to perform a delete" in {
    whenReady(persistence.reaver) { res =>
      info("Reaver method invoked")
    }
  }

  "should generate a kill date and store it in Mongo for an authorised Request" ignore {
    forAll { (user: InternalId, enrolments: Enrolments) =>
      val req = AuthorisedRequest[AnyContent](user, enrolments, FakeRequest())

      val future = persistence(req) { db =>
        Future.successful(db -> Results.Ok("bla"))
      }

      whenReady(future) { res =>
        res.header.status mustBe Status.OK
      }
    }
  }


}
