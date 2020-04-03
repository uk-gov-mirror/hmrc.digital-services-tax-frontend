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

package uk.gov.hmrc.digitalservicestax.journeys

import com.outworkers.util.domain.ShortString
import uk.gov.hmrc.digitalservicestax.repo.{JourneyState, JourneyStateStoreImpl}
import uk.gov.hmrc.digitalservicestax.util.FakeApplicationSpec
import com.outworkers.util.samplers._
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._

import scala.concurrent.ExecutionContext.Implicits.global

class JourneysStateStoreTest extends FakeApplicationSpec {

  val store = app.injector.instanceOf[JourneyStateStoreImpl]

  "should correctly configure an expiration date" in {
    store.expireAfterSeconds mustEqual appConfig.mongoJourneyStoreExpireAfter.toSeconds
  }

  "should store a journey state by used id" in {
    val user = gen[ShortString].value
    val state = gen[JourneyState]

    val chain = for {
      _ <- store.storeState(user, state)
      res <- store.getState(user)
    } yield res

    whenReady(chain) { res =>
      res mustEqual state
    }
  }

  "should fail to retrieve a non existing state" in {
    val user = gen[ShortString].value

    val chain = for {
      res <- store.getState(user)
    } yield res

    whenReady(chain) { res =>
      res mustEqual JourneyState()
    }
  }

  "throw an error if the JSON format is invalid" in {
    val user = gen[ShortString].value
    val state = gen[JourneyState]


    val chain = for {
      _ <- store.cacheRepository
        .createOrUpdate(
          user,
          store.cacheRepositoryKey,
          Json.toJson(JsObject(Seq("Invalid layering" -> JsString(state.test))))
        ).map(_=> ())
      beforeDelete <- store.getState(user)
    } yield beforeDelete

    whenReady(chain) { res =>
      res mustEqual JourneyState()
    }

  }

  "should store a journey state and remove it" in {
    val user = gen[ShortString].value
    val state = gen[JourneyState]

    val chain = for {
      _ <- store.storeState(user, state)
      beforeDelete <- store.getState(user)
      _ <- store.clear(user)
      afterDelete <- store.getState(user)
    } yield beforeDelete -> afterDelete

    whenReady(chain) { case (beforeDelete, afterDelete) =>
      beforeDelete mustEqual state
      afterDelete mustEqual JourneyState()
    }
  }
}
