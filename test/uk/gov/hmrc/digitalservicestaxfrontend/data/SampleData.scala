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

import cats.implicits._
import uk.gov.hmrc.digitalservicestax.data.Activity.{OnlineMarketplace, SearchEngine, SocialMedia}


object SampleData {

  val sampleAddress = UkAddress (
    AddressLine("12 The Street"),
    AddressLine("False Crescent").some,
    AddressLine("Genericford").some,
    AddressLine("Madeupshire").some,
    Postcode("GE12 3CD")
  )

  val internationalAddress = ForeignAddress(
    AddressLine("Ave La Gare"),
    None,
    None,
    Some(AddressLine("Paris")),
    CountryCode("FR")
  )

  val sampleCompanyName = CompanyName("TestCo Ltd")
  val sampleCompany = Company (
    sampleCompanyName,
    sampleAddress
  )

  val utrLookupCompanyName = "UTR Lookup Ltd."
  val utrLookupCompanyRegWrapper =
    CompanyRegWrapper(sampleCompany.copy(name = CompanyName(utrLookupCompanyName)))

  val sampleCompanyRegWrapper = CompanyRegWrapper(
    sampleCompany
  )

  val sampleContact = ContactDetails(
    RestrictiveString("John"),
    RestrictiveString("Smith"),
    PhoneNumber("01234 567890"),
    Email("john.smith@mailprovider.co.uk")
  )

  val sampleReg = Registration (
    sampleCompanyRegWrapper,
    None,
    None,
    sampleContact,
    LocalDate.of(2020, 7, 1),
    LocalDate.of(2021, 4, 5)
  )

  val sampleMoney: Money = Money(BigDecimal(100.00).setScale(2))
  val sampleActivitySet: Set[Activity] = Set(SocialMedia, SearchEngine, OnlineMarketplace)
  val sampleForeignBankAccount: ForeignBankAccount = ForeignBankAccount(IBAN("GB82 WEST 1234 5698 7654 32"))
  val sampleDomesticBankAccount: DomesticBankAccount = DomesticBankAccount(
    SortCode("11-22-33"),
    AccountNumber("88888888"),
    Some(BuildingSocietyRollNumber("ABC - 123"))
  )
  val sampleRepaymentDetails: RepaymentDetails = RepaymentDetails(
    AccountName("DigitalService"),
    sampleDomesticBankAccount
  )
  val samplePercent: Percent = Percent(50)
  val sampleGroupCompanyList: List[GroupCompany] = List(GroupCompany(
    sampleCompanyName,
    Some(UTR("1234567891"))
  ))

  val samplePeriod: Period = Period(
    LocalDate.now,
    LocalDate.now,
    LocalDate.now,
    Period.Key("FOOO")
  )
  
}
