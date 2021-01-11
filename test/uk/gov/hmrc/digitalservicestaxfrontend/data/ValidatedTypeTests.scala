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

package uk.gov.hmrc.digitalservicestaxfrontend.data

import java.time.LocalDate

import cats.kernel.Monoid
import org.scalacheck.Gen
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import cats.implicits._
import org.scalactic.anyvals.PosInt
import uk.gov.hmrc.digitalservicestax.frontend._
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestaxfrontend.TestInstances._

class ValidatedTypeTests extends FlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  it should "fail to parse a validated tagged type using an of method" in {
    intercept[IllegalArgumentException] {
      Postcode("this is not a postcode")
    }
  }

  it should "concatenate lines in a UKAddress value" in {
    forAll { ukAddress: UkAddress =>
      ukAddress.lines shouldEqual List(
        ukAddress.line1,
        ukAddress.line2.getOrElse(""),
        ukAddress.line3.getOrElse(""),
        ukAddress.line4.getOrElse(""),
        ukAddress.postalCode
      ).filter(_.nonEmpty)
    }
  }

  it should "validate IBAN numbers from a series of concrete examples" in {
    forAll(Gen.oneOf(ibanList), minSuccessful(PosInt(86))) { source: String =>
      IBAN.of(source) shouldBe defined
    }
  }



  it should "concatenate lines in a ForeignAddress value" in {
    forAll { foreignAddress: ForeignAddress =>
      foreignAddress.lines shouldEqual List(
        foreignAddress.line1,
        foreignAddress.line2.getOrElse(""),
        foreignAddress.line3.getOrElse(""),
        foreignAddress.line4.getOrElse(""),
        Country.name(foreignAddress.countryCode)
      ).filter(_.nonEmpty)
    }
  }

  it should "correctly subtract 3 months and add one day to a period" in {
    forAll { period: Period =>
      period.paymentDue shouldEqual period.end.minusMonths(3).plusDays(1)
    }
  }

  it should "correctly define the class name of a validated type" in {
    AccountNumber.className shouldEqual "AccountNumber$"
  }

  it should "store percentages as bytes and initialise percent monoids with byte monoids" in {
    Monoid[Percent].empty shouldEqual Monoid[Byte].empty
  }

  it should "add up percentages using monoidal syntax" in {

    val generator = for {
      p1 <- Gen.chooseNum(0, 100)
      p2 = 100 - p1
    } yield p1.toByte -> p2.toByte

    forAll(generator) { case (p1, p2) =>
      whenever(p1 >= 0 && p2 >= 0) {
        val addedPercent = Monoid.combineAll(Seq(Percent(p1), Percent(p2)))
        val addedBytes = Monoid.combineAll(Seq(p1, p2))
        addedPercent shouldEqual Percent(addedBytes)
      }
    }
  }

  it should "allow comparing local dates with cats syntax" in {
    import cats.syntax.order._

    val comparison = LocalDate.now.plusDays(1) compare LocalDate.now()
    comparison shouldEqual 1
  }

  it should "encode an Activity as a URL" in {
    forAll(Gen.oneOf(Activity.values)) { a =>
      val expected = a.toString.replaceAll("(^[A-Z].*)([A-Z])", "$1-$2").toLowerCase
      Activity.toUrl(a) shouldEqual expected
    }
  }
}

