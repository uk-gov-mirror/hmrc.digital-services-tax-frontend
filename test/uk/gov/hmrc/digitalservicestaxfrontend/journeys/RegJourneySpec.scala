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

import java.time.LocalDate

//import cats.Id

//import cats.Id
import org.scalatest.{FlatSpec, Matchers}
import ltbs.uniform._
import interpreters.logictable._
//import ltbs.uniform.{NonEmptyString => _, _}
import uk.gov.hmrc.digitalservicestax.connectors.DSTService
import uk.gov.hmrc.digitalservicestax.data.{Address, CompanyRegWrapper, ContactDetails, NonEmptyString, Period, Postcode, Registration, Return, SampleData, UTR, UkAddress}
import uk.gov.hmrc.digitalservicestax.journeys.RegJourney
import uk.gov.hmrc.digitalservicestax.journeys.RegJourney.{RegAskTypes, RegTellTypes}


class RegJourneySpec extends FlatSpec with Matchers {

  implicit val sampleUtr = new SampleData[UTR] {
    def apply(key: String): List[UTR] = List(UTR("1234567891"))
  }
  implicit val samplePostcode = new SampleData[Postcode] {
    def apply(key: String): List[Postcode] = List(Postcode("BN1 1NB"))
  }
  implicit val sampleDate = new SampleData[LocalDate] {
    def apply(key: String): List[LocalDate] = List(LocalDate.now)
  }
  implicit val sampleContactDetails= new SampleData[ContactDetails] {
    def apply(key: String): List[ContactDetails] = List(SampleData.sampleContact)
  }
  implicit val sampleAddress= new SampleData[Address] {
    def apply(key: String): List[Address] = List(SampleData.sampleAddress)
  }
  implicit val sampleUkAddress= new SampleData[UkAddress] {
    def apply(key: String): List[UkAddress] = List(SampleData.sampleAddress)
  }
  implicit val sampleBoolean= new SampleData[Boolean] {
    def apply(key: String): List[Boolean] = List(true)
  }
  implicit val sampleString= new SampleData[String] {
    def apply(key: String): List[String] = List("foo")
  }
  implicit val sampleNonEmptyString= new SampleData[NonEmptyString] {
    def apply(key: String): List[NonEmptyString] = List(NonEmptyString("foo"))
  }

  val interpreter = LogicTableInterpreter[RegTellTypes, RegAskTypes]() // need implicit instances for every ask and tell (one for all tells0

  val service = new DSTService[cats.Id] {

    override def lookupCompany(): Option[CompanyRegWrapper] = ???

    override def lookupCompany(utr: UTR, postcode: Postcode): Option[CompanyRegWrapper] = ???

    override def submitRegistration(reg: Registration): Unit = ???

    override def submitReturn(period: Period, ret: Return): Unit = ???

    override def lookupRegistration(): Option[Registration] = ???

    override def lookupOutstandingReturns(): Set[Period] = ???

  }

  val outcome = RegJourney.registrationJourney[cats.Id](interpreter, service)
//  val outcome = RegJourney.registrationJourney[cats.Id](interpreter, service).value.run

  "foo" should "do something " in {
    println(s"outcome is $outcome")
    1 shouldBe 1
  }

}
