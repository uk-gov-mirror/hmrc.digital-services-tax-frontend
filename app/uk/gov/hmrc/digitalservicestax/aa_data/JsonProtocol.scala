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

package uk.gov.hmrc.digitalservicestax.aa_data

import enumeratum.EnumFormats
import play.api.libs.json._
import uk.gov.hmrc.digitalservicestax.data._

object JsonProtocol {

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

  implicit val activityFormat: Format[Activity] = EnumFormats.formats(Activity)
  implicit val groupCompanyFormat: Format[GroupCompany] = Json.format[GroupCompany]

  //implicit val moneyFormat: Format[Money] = Json.format[BigDecimal]


  implicit val sortCodeFormat: Format[SortCode] = new Format[SortCode] {
    override def reads(json: JsValue): JsResult[SortCode] = {
      json match {
        case JsString(value) =>
          SortCode.validateAndTransform(value) match {
            case Some(validCode) => JsSuccess(SortCode(validCode))
            case None => JsError(s"Expected a valid sort code definition, got $value instead.")
          }

        case xs : JsValue => JsError(
          JsPath -> JsonValidationError(Seq(s"""Expected a valid sort code string, got $xs instead"""))
        )
      }
    }

    override def writes(o: SortCode): JsValue = {
      JsString(o)
    }
  }

  implicit val accountNumberFormat: Format[AccountNumber] = new Format[AccountNumber] {
    override def reads(json: JsValue): JsResult[AccountNumber] = {
      json match {
        case JsString(value) =>
          SortCode.validateAndTransform(value) match {
            case Some(validCode) => JsSuccess(AccountNumber(validCode))
            case None => JsError(s"Expected a valid sort code definition, got $value instead.")
          }

        case xs : JsValue => JsError(
          JsPath -> JsonValidationError(Seq(s"""Expected a valid sort code string, got $xs instead"""))
        )
      }
    }

    override def writes(o: AccountNumber): JsValue = {
      JsString(o)
    }
  }

  implicit val percentFormat: Format[Percent] = new Format[Percent] {
    override def reads(json: JsValue): JsResult[Percent] = {
      json match {
        case JsNumber(value) =>
          Percent.validateAndTransform(value.toByte) match {
            case Some(validCode) => JsSuccess(Percent(validCode))
            case None => JsError(s"Expected a valid percentage, got $value instead.")
          }

        case xs: JsValue => JsError(
          JsPath -> JsonValidationError(Seq(s"""Expected a valid percentage, got $xs instead"""))
        )
      }
    }

    override def writes(o: Percent): JsValue = JsNumber(BigDecimal(o))
  }

  implicit val activityMapFormat: Format[Map[Activity, Percent]] = new Format[Map[Activity, Percent]] {
    override def reads(json: JsValue): JsResult[Map[Activity, Percent]] = {
      JsSuccess(json.as[Map[String, JsNumber]].map { case (k, v) =>
        Activity.values.find(_.entryName == k).get -> Percent.apply(v.value.toByte)
      })
    }

    override def writes(o: Map[Activity, Percent]): JsValue = ???
  }
  implicit val groupCompanyMapFormat: OFormat[Map[GroupCompany, Money]] = Json.format[Map[GroupCompany, Money]]

  implicit val ibanFormat: Format[IBAN] = new Format[IBAN] {
    override def reads(json: JsValue): JsResult[IBAN] = {
      json match {
        case JsString(value) =>
          IBAN.validateAndTransform(value) match {
            case Some(validCode) => JsSuccess(IBAN(validCode))
            case None => JsError(s"Expected a valid IBAN number definition, got $value instead.")
          }

        case xs : JsValue => JsError(
          JsPath -> JsonValidationError(Seq(s"""Expected a string with an IBAN number, got $xs instead"""))
        )
      }
    }

    override def writes(o: IBAN): JsValue = {
      JsString(o)
    }
  }

  implicit val domesticBankAccountFormat: OFormat[DomesticBankAccount] = Json.format[DomesticBankAccount]
  implicit val foreignBankAccountFormat: OFormat[ForeignBankAccount] = Json.format[ForeignBankAccount]
  implicit val bankAccountFormat: OFormat[BankAccount] = Json.format[BankAccount]
  implicit val repaymentDetailsFormat: OFormat[RepaymentDetails] = Json.format[RepaymentDetails]

  implicit val returnFormat: OFormat[Return] = Json.format[Return]
}
