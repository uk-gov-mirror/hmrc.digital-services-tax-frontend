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

object SampleData {

  val sampleAddress = UkAddress (
    AddressLine("12 The Street"),
    AddressLine("False Crescent").some,
    AddressLine("Genericford").some,
    AddressLine("Madeupshire").some,
    Postcode("GE12 3CD")
  )

  val sampleCompany = Company (
    CompanyName("TestCo Ltd"),
    sampleAddress
  )

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
}
