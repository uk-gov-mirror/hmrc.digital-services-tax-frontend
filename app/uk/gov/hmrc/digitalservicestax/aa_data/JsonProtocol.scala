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
import shapeless.tag.@@
object JsonProtocol {

  def validatedStringFormat(A: ValidatedType[String], name: String) = new Format[String @@ A.Tag] {
    override def reads(json: JsValue): JsResult[String @@ A.Tag] = json match {
      case JsString(value) =>
        A.validateAndTransform(value) match {
          case Some(v) => JsSuccess(A(v))
          case None => JsError(s"Expected a valid $name, got $value instead")
        }
      case xs : JsValue => JsError(JsPath -> JsonValidationError(Seq(s"""Expected a valid $name, got $xs instead""")))
    }

    override def writes(o: String @@ A.Tag): JsValue = JsString(o)
  }

  implicit val nonEmptyStringFormat: Format[NonEmptyString] = new Format[NonEmptyString] {
    override def reads(json: JsValue): JsResult[NonEmptyString] = json match {
      case JsString(value) if value.nonEmpty => JsSuccess(NonEmptyString.apply(value))
      case _ => JsError((JsPath \ "value") -> JsonValidationError(Seq(s"Expected non empty string, got $json")))
    }

    override def writes(o: NonEmptyString): JsValue = JsString(o)
  }

  implicit val postcodeFormat       = validatedStringFormat(Postcode, "postcode")
  implicit val phoneNumberFormat    = validatedStringFormat(PhoneNumber, "phone number")
  implicit val utrFormat            = validatedStringFormat(UTR, "UTR")
  implicit val emailFormat          = validatedStringFormat(Email, "email")
  implicit val countryCodeFormat    = validatedStringFormat(CountryCode, "country code")
  implicit val sortCodeFormat       = validatedStringFormat(SortCode, "sort code")
  implicit val accountNumberFormat  = validatedStringFormat(AccountNumber, "account number")
  implicit val ibanFormat           = validatedStringFormat(IBAN, "IBAN number")

  implicit val foreignAddressFormat: OFormat[ForeignAddress] = Json.format[ForeignAddress]
  implicit val ukAddressFormat: OFormat[UkAddress] = Json.format[UkAddress]
  implicit val addressFormat: OFormat[Address] = Json.format[Address]
  implicit val companyFormat: OFormat[Company] = Json.format[Company]
  implicit val contactDetailsFormat: OFormat[ContactDetails] = Json.format[ContactDetails]
  implicit val registrationFormat: OFormat[Registration] = Json.format[Registration]
  implicit val activityFormat: Format[Activity] = EnumFormats.formats(Activity)
  implicit val groupCompanyFormat: Format[GroupCompany] = Json.format[GroupCompany]

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

    override def writes(o: Map[Activity, Percent]): JsValue = {
      JsObject(o.toSeq.map { case (k, v) =>
        k.entryName -> JsNumber(BigDecimal(v))
      })
    }
  }

<<<<<<< HEAD
  implicit val groupCompanyMapFormat: Format[Map[GroupCompany, Money]] = new Format[Map[GroupCompany, Money]] {
=======
  implicit val groupCompanyMapFormat: OFormat[Map[GroupCompany, Money]] = new OFormat[Map[GroupCompany, Money]] {
>>>>>>> 2ba61e30a4d7cb09103a49466084d745b43dc080
    override def reads(json: JsValue): JsResult[Map[GroupCompany, Money]] = {
      JsSuccess(json.as[Map[String, JsNumber]].map { case (k, v) =>

        val Array(name, utr) = k.split(":")
        GroupCompany(NonEmptyString(name), UTR(utr)) -> v.value
      })
    }

    override def writes(o: Map[GroupCompany, Money]): JsObject = {
      JsObject(o.toSeq.map { case (k, v) =>
        s"${k.name}:${k.utr}" -> JsNumber(v)
      })
    }
  }

  implicit val domesticBankAccountFormat: OFormat[DomesticBankAccount] = Json.format[DomesticBankAccount]
  implicit val foreignBankAccountFormat: OFormat[ForeignBankAccount] = Json.format[ForeignBankAccount]
  implicit val bankAccountFormat: OFormat[BankAccount] = Json.format[BankAccount]
  implicit val repaymentDetailsFormat: OFormat[RepaymentDetails] = Json.format[RepaymentDetails]
  implicit val returnFormat: OFormat[Return] = Json.format[Return]
}
