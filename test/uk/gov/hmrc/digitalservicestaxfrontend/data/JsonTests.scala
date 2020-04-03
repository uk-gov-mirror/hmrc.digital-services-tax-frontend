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

package uk.gov.hmrc.digitalservicestax.data

import java.time.LocalDate

import com.outworkers.util.samplers._
import enumeratum.scalacheck._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.{Assertion, FlatSpec, Matchers, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json._
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import uk.gov.hmrc.digitalservicestax.data.{Activity, Company, CompanyRegWrapper, CountryCode, Email, GroupCompany, Money, NonEmptyString, Percent, PhoneNumber, Postcode, UTR}
import uk.gov.hmrc.digitalservicestax.TestInstances._
import ltbs.uniform.interpreters.playframework.DB
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.digitalservicestax.repo.JourneyState

class JsonTests extends FlatSpec with Matchers with ScalaCheckDrivenPropertyChecks with OptionValues {

  def testJsonRoundtrip[T : Arbitrary : Format]: Assertion = {
    forAll { sample: T =>
      val js = Json.toJson(sample)

      val parsed = js.validate[T]
      parsed.isSuccess shouldEqual true
      parsed.asOpt.value shouldEqual sample
    }
  }

  def testJsonRoundtrip[T : Format](gen: Gen[T]): Assertion = {
    forAll(gen) { sample: T =>
      val js = Json.toJson(sample)

      val parsed = js.validate[T]
      parsed.isSuccess shouldEqual true
      parsed.asOpt.value shouldEqual sample
    }
  }

  it should "serialize and de-serialise a Postcode instance" in {
    testJsonRoundtrip[Postcode]
  }

  it should "fail to validate a postcode from JSON if the source input doesn't match expected regex" in {
    val parsed = Json.parse(s""" "124124125125125" """).validate[Postcode]
    parsed.isSuccess shouldEqual false
    parsed shouldEqual JsError(s"Expected a valid postcode, got 124124125125125 instead")
  }


  it should "fail to validate a postcode from JSON if the source input is in incorrect format" in {
    val generated = gen[Int]
    val parsed = Json.parse(s"""$generated""").validate[Postcode]
    parsed.isSuccess shouldEqual false
    parsed shouldEqual JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid postcode, got $generated instead""")))
  }

  it should "serialize and de-serialise a PhoneNumber instance" in {
    testJsonRoundtrip[PhoneNumber]
  }


  it should "serialize and de-serialise a NonEmptyString instance" in {
    testJsonRoundtrip[NonEmptyString]
  }

  it should "fail to validate a NonEmptyString from an invalid source" in {
    forAll(Gen.chooseNum(Int.MinValue, Int.MaxValue)) { sample =>
      val parsed = Json.parse(sample.toString).validate[NonEmptyString]

      parsed shouldEqual JsError(
        (JsPath \ "value") -> JsonValidationError(Seq(s"Expected non empty string, got $sample"))
      )
    }
  }

  it should "serialize and de-serialise an Email instance" in {
    testJsonRoundtrip[Email]
  }

  it should "serialize and de-serialise a CountryCode instance" in {
    testJsonRoundtrip[CountryCode]
  }

  it should "serialize and de-serialise a UTR instance" in {
    testJsonRoundtrip[UTR]
  }

  it should "serialize and de-serialise a percent instance" in {
    testJsonRoundtrip[Percent]
  }

  it should "fail to parse a percent from a non 0 - 100 int value" in {

    val invalidPercentages = Gen.chooseNum(-100, -1)

    forAll(invalidPercentages) { sample =>
      val parsed = Json.parse(sample.toString).validate[Percent]
      parsed shouldEqual JsError(s"Expected a valid percentage, got $sample instead.")
    }
  }

  it should "fail to validate a percentage from a non numeric value" in {
    forAll(Sample.generator[ShortString]) { sample =>

      val parsed = Json.parse(s""" "${sample.value}" """).validate[Percent]

      parsed shouldEqual JsError(
        JsPath -> JsonValidationError(Seq(s"""Expected a valid percentage, got "${sample.value}" instead"""))
      )
    }
  }

  it should "serialize and de-serialise a DB instance" in {
    forAll { sample: DB =>
      val js = Json.toJson(sample)

      val parsed = js.validate[DB]
      parsed.isSuccess shouldEqual true
      //parsed.asOpt.value should contain theSameElementsAs sample
    }
  }

  it should "fail to parse a DB instance from a Json primitive" in {
    val source = JsString("bla")
    source.validate[DB] shouldBe a [JsError]
  }

  it should "fail to parse a formatMap from a non object" in {
    val obj = JsString("bla")

    obj.validate[DB] shouldBe JsError(s"expected an object, got $obj")
  }

  it should "serialize and de-serialise a GroupCompany instance" in {
    testJsonRoundtrip[GroupCompany](genGroupCo)
  }

  it should "serialize and de-serialise a Money instance" in {
    testJsonRoundtrip[Money]
  }

  it should "serialize and de-serialise an Activity instance" in {
    testJsonRoundtrip[Activity]
  }

  it should "serialize and de-serialise a Company instance" in {
    testJsonRoundtrip[Company]
  }

  it should "serialize and de-serialise a Map[GroupCompany, Money]" in {
    testJsonRoundtrip[Map[GroupCompany, Money]](gencomap)
  }

  it should "serialize and de-serialise a LocalDate" in {
    testJsonRoundtrip[LocalDate]
  }

  it should "serialize and de-serialise a CompanyRegWrapper" in {
    testJsonRoundtrip[CompanyRegWrapper]
  }

  it should "serialize and de-serialise a Journey State" in {
    testJsonRoundtrip[JourneyState](Sample.generator[JourneyState])
  }

  it should "serialize and de-serialise an optional LocalDate" in {
    testJsonRoundtrip[Option[LocalDate]]
  }


  it should "serialize and de-serialise a set of Enrolments" in {
    testJsonRoundtrip[Set[Enrolment]]
  }

  it should "serialize and de-serialise a Map[Activity, Percent]" in {
    testJsonRoundtrip[Map[Activity, Percent]](genActivityPercentMap)
  }


  it should "serialize and de-serialise a CompanyRegFormat" in {
    testJsonRoundtrip[CompanyRegWrapper]
  }

  it should "serialize an enum entry as a string" in {
    val jsValue = Json.toJson(Activity.SocialMedia)
    jsValue shouldEqual JsString("SocialMedia")
  }
}
