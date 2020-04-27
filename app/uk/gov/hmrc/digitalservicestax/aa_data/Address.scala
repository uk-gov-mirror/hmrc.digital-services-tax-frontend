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

sealed trait Address {
  def line1: MandatoryAddressLine
  def line2: Option[MandatoryAddressLine]
  def line3: Option[MandatoryAddressLine]
  def line4: Option[MandatoryAddressLine]
  def countryCode: CountryCode
  def postalCode: String
  def lines: List[String] =
    (line1 :: line2.getOrElse(MandatoryAddressLine("")) :: line3.getOrElse(MandatoryAddressLine("")) :: line4.getOrElse(MandatoryAddressLine("")) :: postalCode :: Country.name(countryCode) :: Nil).filter(_.nonEmpty)
}

case class UkAddress(
  line1: MandatoryAddressLine,
  line2: Option[MandatoryAddressLine],
  line3: Option[MandatoryAddressLine],
  line4: Option[MandatoryAddressLine],
  postalCode: Postcode
) extends Address {
  def countryCode = CountryCode("GB")
  override def lines: List[String] = super.lines.init
}

case class ForeignAddress(
  line1: MandatoryAddressLine,
  line2: Option[MandatoryAddressLine],
  line3: Option[MandatoryAddressLine],
  line4: Option[MandatoryAddressLine],
  countryCode: CountryCode
) extends Address {
  def postalCode = ""
}
