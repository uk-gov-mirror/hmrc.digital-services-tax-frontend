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

import enumeratum.EnumFormats
import ltbs.uniform.interpreters.playframework.DB
import play.api.libs.json._
import shapeless.tag.@@
import cats.syntax.option._
import uk.gov.hmrc.digitalservicestax.connectors.MongoPersistence.Wrapper
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.digitalservicestax.connectors.MongoPersistence.Wrapper
import uk.gov.hmrc.digitalservicestax.repo.JourneyState

trait SimpleJson {

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
  implicit val safeIfFormat         = validatedStringFormat(SafeId, "SafeId")
  implicit val formBundleNoFormat   = validatedStringFormat(FormBundleNumber, "FormBundleNumber")
  implicit val internalIdFormat     = validatedStringFormat(InternalId, "internal id")  
  implicit val emailFormat          = validatedStringFormat(Email, "email")
  implicit val countryCodeFormat    = validatedStringFormat(CountryCode, "country code")
  implicit val sortCodeFormat       = validatedStringFormat(SortCode, "sort code")
  implicit val accountNumberFormat  = validatedStringFormat(AccountNumber, "account number")
  implicit val ibanFormat           = validatedStringFormat(IBAN, "IBAN number")
  implicit val periodKeyFormat      = validatedStringFormat(Period.Key, "Period Key")
  implicit val restrictiveFormat    = validatedStringFormat(RestrictiveString, "name")
  implicit val dstRegNoFormat       =
    validatedStringFormat(DSTRegNumber, "Digital Services Tax Registration Number")

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
}

object BackendAndFrontendJson extends SimpleJson {

  implicit val readsUnit: Reads[Unit] = Reads[Unit] { _ => JsSuccess(()) }

  implicit val foreignAddressFormat: OFormat[ForeignAddress] = Json.format[ForeignAddress]
  implicit val ukAddressFormat: OFormat[UkAddress] = Json.format[UkAddress]
  implicit val addressFormat: OFormat[Address] = Json.format[Address]
  implicit val companyFormat: OFormat[Company] = Json.format[Company]
  implicit val contactDetailsFormat: OFormat[ContactDetails] = Json.format[ContactDetails]
  implicit val companyRegWrapperFormat: OFormat[CompanyRegWrapper] = Json.format[CompanyRegWrapper]
  implicit val registrationFormat: OFormat[Registration] = Json.format[Registration]
  implicit val activityFormat: Format[Activity] = EnumFormats.formats(Activity)
  implicit val groupCompanyFormat: Format[GroupCompany] = Json.format[GroupCompany]
  implicit lazy val journeyStateFormatter: Format[JourneyState] = Json.format[JourneyState]

  import Enrolment.idFormat
  implicit val enrolmentWrites = Json.writes[Enrolment]

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


  implicit val groupCompanyMapFormat: OFormat[Map[GroupCompany, Money]] = new OFormat[Map[GroupCompany, Money]] {
    override def reads(json: JsValue): JsResult[Map[GroupCompany, Money]] = {
      JsSuccess(json.as[Map[String, JsNumber]].map { case (k, v) =>
        k.split(":") match {
          case Array(name, utrS) =>
            GroupCompany(NonEmptyString(name), Some(UTR(utrS))) -> v.value
          case Array(name) =>
            GroupCompany(NonEmptyString(name), None) -> v.value
        }
      })
    }

    override def writes(o: Map[GroupCompany, Money]): JsObject = {
      JsObject(o.toSeq.map { case (k, v) =>
        s"${k.name}:${k.utr.getOrElse("")}" -> JsNumber(v)
      })
    }
  }

  implicit def optFormatter[A](implicit innerFormatter: Format[A]): Format[Option[A]] =
    new Format[Option[A]] {
      def reads(json: JsValue): JsResult[Option[A]] = json match {
        case JsNull => JsSuccess(none[A])
        case a      => innerFormatter.reads(a).map{_.some}
      }
      def writes(o: Option[A]): JsValue =
        o.map{innerFormatter.writes}.getOrElse(JsNull)
    }

  implicit val domesticBankAccountFormat: OFormat[DomesticBankAccount] = Json.format[DomesticBankAccount]
  implicit val foreignBankAccountFormat: OFormat[ForeignBankAccount] = Json.format[ForeignBankAccount]
  implicit val bankAccountFormat: OFormat[BankAccount] = Json.format[BankAccount]
  implicit val repaymentDetailsFormat: OFormat[RepaymentDetails] = Json.format[RepaymentDetails]
  implicit val returnFormat: OFormat[Return] = Json.format[Return]

  implicit val periodFormat: OFormat[Period] = Json.format[Period]

  val readCompanyReg = new Reads[CompanyRegWrapper] {
    override def reads(json: JsValue): JsResult[CompanyRegWrapper] = {
      JsSuccess(CompanyRegWrapper(
        Company(
          {
            json \ "organisation" \ "organisationName"
          }.as[NonEmptyString], {
            json \ "address"
          }.as[Address]
        ),
        safeId = SafeId({json \ "safeId"}.as[String]).some))
    }

  }
  implicit val formatMap: OFormat[DB] = new OFormat[DB] {
    def writes(o: DB) = JsObject ( o.map {
      case (k,v) => (k.mkString("/"), JsString(v))
    }.toSeq )

    def reads(json: JsValue): JsResult[DB] = json match {
      case JsObject(data) => JsSuccess(data.map {
        case (k, JsString(v)) => (k.split("/").toList, v)
        case e => throw new IllegalArgumentException(s"cannot parse $e")
      }.toMap)
      case e => JsError(s"expected an object, got $e")
    }
  }

}
