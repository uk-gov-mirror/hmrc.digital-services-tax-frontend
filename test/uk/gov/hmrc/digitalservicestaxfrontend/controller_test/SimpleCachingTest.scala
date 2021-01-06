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

package uk.gov.hmrc.digitalservicestaxfrontend.controller_test

import java.util.concurrent.atomic.AtomicInteger

import com.outworkers.util.samplers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.digitalservicestax.controllers.SimpleCaching
import uk.gov.hmrc.digitalservicestax.data.InternalId
import uk.gov.hmrc.digitalservicestaxfrontend.TestInstances._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SimpleCachingTest extends FlatSpec
  with Matchers
  with OptionValues
  with ScalaFutures
  with ScalaCheckDrivenPropertyChecks {


  it should "add an element to the cache" in {
    val cache = SimpleCaching[InternalId]()

    forAll { (internalId: InternalId, checksumKey: String, storedValue: String) =>
      val chain = for {
        _ <- cache.apply[String](internalId, Seq(checksumKey)) {
          Future.successful(storedValue)
        }
        retrieve = cache.fromCache[String](internalId, Seq(checksumKey))
      } yield retrieve

      whenReady(chain) { res =>
        res shouldBe defined
        res.value shouldEqual storedValue
      }
    }
  }

  it should "add an element to the cache and re-use the value on subsequent calls" in {

    val cache = SimpleCaching[InternalId]()

    forAll { (internalId: InternalId, checksumKey: String, storedValue: String) =>
      val counter = new AtomicInteger(0)
      val fut = cache.apply[String](internalId, Seq(checksumKey)) {
        Future.apply {
          counter.getAndIncrement()
          storedValue
        }
      }

      val chain = for {
        _ <- fut
        invkoke <- fut
      } yield invkoke

      whenReady(chain) { res =>
        res shouldEqual storedValue
        counter.get() shouldEqual 1
      }
    }
  }


  it should "update the value of an element in the cache" in {
    val cache = SimpleCaching[InternalId]()
    
    forAll { (internalId: InternalId, checksumKey: String, oldValue: String, newValue: String) =>
      val chain = for {
        _ <- cache.apply[String](internalId, checksumKey) {
          Future.successful(oldValue)
        }
        beforeUpdate = cache.fromCache[String](internalId, checksumKey)
        _ = cache.update(internalId, Seq(checksumKey), newValue)
        afterUpdate = cache.fromCache[String](internalId, checksumKey)
      } yield beforeUpdate -> afterUpdate

      whenReady(chain) { case (beforeUpdate, afterUpdate) =>
        beforeUpdate shouldBe defined
        beforeUpdate.value shouldEqual oldValue

        afterUpdate shouldBe defined
        afterUpdate.value shouldEqual newValue
      }
    }
  }
}
