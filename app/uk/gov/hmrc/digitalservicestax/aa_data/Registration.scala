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

package uk.gov.hmrc.digitalservicestax
package data

//import services._
import java.time.LocalDate

import play.api.libs.json.{JsObject, JsResult, JsValue, OFormat}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Registration (
  identification: UTR, 
  company: Company,
  alternativeContact: Option[Address],
  ultimateParent: Option[Company],
  contact: ContactDetails,
  dateLiable: LocalDate,
  accountingPeriodEnd: LocalDate
)


object Registration {

  implicit val nonEmptyStringFormat: Format[NonEmptyString] = new Format[NonEmptyString] {
    override def reads(json: JsValue): JsResult[NonEmptyString] = json match {
      case JsString(value) if value.length > 0 => JsSuccess(NonEmptyString.apply(value))
      case _ => JsError((JsPath \ "value") -> JsonValidationError(Seq(s"Expected non empty string, got $json")))
    }

    override def writes(o: NonEmptyString): JsValue = JsString(o)
  }

  implicit val postcodeFormat: Format[Postcode] = new Format[Postcode] {
    override def reads(json: JsValue): JsResult[Postcode] = {
      json match {
        case JsString(value) =>
          Postcode.validateAndTransform(value) match {
            case Some(validPostCode) => JsSuccess(Postcode(validPostCode))
            case None => JsError(s"Expected a valid postcode regex, got $value instead")
          }
        case xs : JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid postcode string, got $xs instead""")))
      }
    }

    override def writes(o: Postcode): JsValue = {
      JsString(o)
    }
  }

  implicit val phoneNumberFormat: Format[PhoneNumber] = new Format[PhoneNumber] {
    override def reads(json: JsValue): JsResult[PhoneNumber] = {
      json match {
        case JsString(value) =>
          PhoneNumber.validateAndTransform(value) match {
            case Some(validPhoneNumber) => JsSuccess(PhoneNumber(validPhoneNumber))
            case None => JsError(s"Expected a valid phone number, got $value instead")
          }
        case xs : JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid phone number string, got $xs instead""")))
      }
    }

    override def writes(o: PhoneNumber): JsValue = {
      JsString(o)
    }
  }

  implicit val utrFormat: Format[UTR] = new Format[UTR] {
    override def reads(json: JsValue): JsResult[UTR] = {
      json match {
        case JsString(value) =>
          UTR.validateAndTransform(value) match {
            case Some(validCode) => JsSuccess(UTR(validCode))
            case None => JsError(s"Expected a valid UTR number, got $value instead.")
          }

        case xs : JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid UTR string, got $xs instead""")))
      }
    }

    override def writes(o: UTR): JsValue = JsString(o)
  }


  implicit val emailFormat: Format[Email] = new Format[Email] {
    override def reads(json: JsValue): JsResult[Email] = {
      json match {
        case JsString(value) =>
          Email.validateAndTransform(value) match {
            case Some(validEmail) => JsSuccess(Email(validEmail))
            case None => JsError(s"Expected a valid address number, got $value instead.")
          }

        case xs : JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid email address string, got $xs instead""")))
      }
    }

    override def writes(o: Email): JsValue = JsString(o)
  }

  implicit val countryCodeFormat: Format[CountryCode] = new Format[CountryCode] {
    override def reads(json: JsValue): JsResult[CountryCode] = {
      json match {
        case JsString(value) =>
          CountryCode.validateAndTransform(value) match {
            case Some(validCode) => JsSuccess(CountryCode(validCode))
            case None => JsError(s"Expected a valid country code definition, got $value instead.")
          }

        case xs : JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid country code string, got $xs instead""")))
      }
    }

    override def writes(o: CountryCode): JsValue = {
      JsString(o)
    }
  }


  implicit val foreignAddressFormat: OFormat[ForeignAddress] = Json.format[ForeignAddress]
  implicit val ukAddressFormat: OFormat[UkAddress] = Json.format[UkAddress]

  implicit val addressFormat: OFormat[Address] = Json.format[Address]
  implicit val companyFormat: OFormat[Company] = Json.format[Company]
  implicit val contactDetailsFormat: OFormat[ContactDetails] = Json.format[ContactDetails]

  implicit val registrationFormat: OFormat[Registration] = Json.format[Registration]
}
