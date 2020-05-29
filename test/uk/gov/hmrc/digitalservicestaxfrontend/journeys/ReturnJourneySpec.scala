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

package uk.gov.hmrc.digitalservicestaxfrontend.journeys

import cats.implicits._
import ltbs.uniform.interpreters.logictable._
import uk.gov.hmrc.digitalservicestax.data._, SampleData._
import uk.gov.hmrc.digitalservicestax.journeys.ReturnJourney
import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.digitalservicestax.data.Activity.SocialMedia

import scala.util.matching.Regex

class ReturnJourneySpec extends FlatSpec with Matchers {

  val lossKeys: Regex = {s"^report-(" + Activity.values.map(Activity.toUrl).mkString("|") + ")-loss"}.r

  implicit val sampleMoneyAsk = instances[Money](sampleMoney)
  implicit val sampleActivitySetAsk = instances(sampleActivitySet)
  implicit val sampleRepaymentDetailsAsk = instances(sampleRepaymentDetails)
  implicit val samplePercentAsk = instances(samplePercent)
  implicit val sampleGroupCompanyListAsk = instances(sampleGroupCompanyList)
  implicit val sampleBooleanAsk = instancesF {
    case lossKeys(_) => List(false)
    case _ => List(true)
  }

  val defaultInterpreter = new TestReturnInterpreter

  "Return.alternativeCharge length" should "be the same as length of reported activities" in {
    implicit val sampleActivitySetAsk = instances(Set[Activity](SocialMedia))
    val ret:Return = ReturnJourney.returnJourney(
      new TestReturnInterpreter,
      samplePeriod,
      sampleReg
    ).value.run.asOutcome()
    ret.alternateCharge.size shouldBe 1
  }

  "Return.alternateCharge " should "be empty when no alternate charge is reported" in {
    implicit val sampleBooleanAsk = instancesF {
      case "report-alternative-charge" => List(false)
      case _ => List(true)
    }
    val ret:Return = ReturnJourney.returnJourney(
      new TestReturnInterpreter,
      samplePeriod,
      sampleReg
    ).value.run.asOutcome()
    ret.alternateCharge  should be ('empty)
  }

  "Return.alternateCharge " should "not be empty when an alternate charge is reported" in {
    val ret:Return = ReturnJourney.returnJourney(
      defaultInterpreter,
      samplePeriod,
      sampleReg
    ).value.run.asOutcome()

    ret.alternateCharge  should be ('nonEmpty)
  }

  "Return.crossBorderReliefAmount" should "be zero when report-cross-border-transaction-relief is false" in {
    implicit val sampleBooleanAsk = instancesF {
      case "report-cross-border-transaction-relief" => List(false)
      case _ => List(true)
    }
    val ret:Return = ReturnJourney.returnJourney(
      new TestReturnInterpreter,
      samplePeriod,
      sampleReg
    ).value.run.asOutcome()

    ret.crossBorderReliefAmount shouldEqual BigDecimal(0)
  }

  "Return.repayment" should "be nonEmpty when the user has asked for a repayment" in {
    val ret:Return = ReturnJourney.returnJourney(
      defaultInterpreter,
      samplePeriod,
      sampleReg
    ).value.run.asOutcome()

    ret.repayment  should be ('nonEmpty)
  }

  "Return.repayment" should "be empty when the user has not asked for a repayment" in {
    implicit val sampleBooleanAsk = instancesF {
      case "repayment" => List(false)
      case _ => List(true)
    }
    val ret:Return = ReturnJourney.returnJourney(
      new TestReturnInterpreter,
      samplePeriod,
      sampleReg
    ).value.run.asOutcome()
    ret.repayment  should be ('empty)
  }

}
