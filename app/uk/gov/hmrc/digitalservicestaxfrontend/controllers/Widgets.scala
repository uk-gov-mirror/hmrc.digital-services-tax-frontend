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

package uk.gov.hmrc.digitalservicestaxfrontend.controllers

import java.time.LocalDate

import cats.data.Validated
import cats.implicits._
import enumeratum._
import ltbs.uniform.common.web.{FormField, FormFieldStats}
import ltbs.uniform.interpreters.playframework.Path
import ltbs.uniform.validation.Rule._
import ltbs.uniform.validation._
import ltbs.uniform.{ErrorMsg, ErrorTree, Input, RichInput, UniformMessages, _}
import play.twirl.api.Html
import uk.gov.hmrc.digitalservicestaxfrontend._

trait Widgets {
//TODO fix commented out FormFields
  implicit val twirlStringField: FormField[String, Html] = twirlStringFields()

  def twirlStringFields(autoFields: Option[String] = None): FormField[String, Html] = new FormField[String, Html] {
    def decode(out: Input): Either[ErrorTree, String] =
      out.toStringField().toEither

    def encode(in: String): Input = Input.one(List(in))

    def render(
      key: List[String],
      path: Path,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      val existingValue: String =
        data.valueAtRoot.flatMap{_.headOption}.getOrElse("")
      views.html.uniform.string(key, existingValue, errors, messages, autoFields)
    }
  }

//  implicit val twirlBigDecimalField: FormField[BigDecimal, Html] =
//    twirlStringField.simap(
//      nonEmpty[String].apply(_).toEither >>= {x => Either.catchOnly[NumberFormatException]{
//        BigDecimal.apply(x)
//      }.leftMap(_ => ErrorTree.oneErr(ErrorMsg("format")))
//    })(_.toString)

  implicit val twirlBoolField = new FormField[Boolean, Html] {
    val True = true.toString.toUpperCase
    val False = false.toString.toUpperCase

    override def stats = FormFieldStats(children = 2)

    def decode(out: Input): Either[ErrorTree, Boolean] =
      out.toField[Boolean](
        x => nonEmpty[String].apply(x) andThen ( y => Validated.catchOnly[IllegalArgumentException](y.toBoolean)
          .leftMap(_ => ErrorMsg("invalid").toTree))
      ).toEither

    def encode(in: Boolean): Input = Input.one(List(in.toString))

    def render(
      key: List[String],
      path: Path,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      val options = if (key.contains("about-you") || key.contains("user-employed")) List(False, True) else List(True, False)
      val existingValue = data.toStringField().toOption
      views.html.uniform.radios(key,
        options,
        existingValue,
        errors,
        messages)
    }
  }

//  implicit val twirlDateField = new FormField[LocalDate, Html] {
//
//    override def stats = FormFieldStats(children = 3)
//
//    def decode(out: Input): Either[ErrorTree, LocalDate] = {
//
//      def intAtKey(key: String): Validated[List[String], Int] =
//        Validated.fromOption(
//          out.valueAt(key).flatMap{_.find(_.trim.nonEmpty)},
//          List(key)
//        ).andThen{
//          x => Validated.catchOnly[NumberFormatException](x.toInt).leftMap(_ => List(key))
//        }
//
//        (
//          intAtKey("year"),
//          intAtKey("month"),
//          intAtKey("day")
//        ).tupled
//        .leftMap{x => ErrorMsg(x.reverse.mkString("-and-") + ".empty").toTree}
//        .toEither
//        .flatMap{ case (y,m,d) =>
//          Either.catchOnly[java.time.DateTimeException]{
//            LocalDate.of(y,m,d)
//          }.leftMap(_ => ErrorTree.oneErr(ErrorMsg("not-a-date")))
//        }
//    }
//
//    def encode(in: LocalDate): Input = Map(
//      List("year") → in.getYear(),
//      List("month") → in.getMonthValue(),
//      List("day") → in.getDayOfMonth()
//    ).mapValues(_.toString.pure[List])
//
//    def render(
//      key: List[String],
//      path: Path,
//      data: Input,
//      errors: ErrorTree,
//      messages: UniformMessages[Html]
//    ): Html = {
//      views.html.uniform.date(
//        key,
//        data,
//        errors,
//        messages
//      )
//    }
//  }
//
//  implicit def twirlAddressField[T](
//    implicit gen: shapeless.LabelledGeneric.Aux[Address,T],
//    ffhlist: FormField[T, Html]
//  )= new FormField[Address, Html] {
//
//    def decode(out: Input): Either[ErrorTree, Address] = {
//      // Not used due to international postcodes
//      // val postCodeRegex = """([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z]))))\s?[0-9][A-Za-z]{2}|.{0})"""
//
//      def standardValidation: String => Validated[ErrorTree, String] =
//        maxLength[String](40, "limit") followedBy
//          matchesRegex("""^[a-zA-Z0-9',-./ ]*$""", "invalid")
//
//      (
//        out.stringSubField("line1", nonEmpty[String] followedBy standardValidation),
//        out.stringSubField("line2", standardValidation(_)),
//        out.stringSubField("town", nonEmpty[String] followedBy standardValidation),
//        out.stringSubField("county", standardValidation(_)),
//        out.stringSubField("postcode", nonEmpty[String] followedBy maxLength(40, "limit"))
//      ).mapN(Address).toEither
//    }
//
//    def encode(in: Address): Input =
//      ffhlist.encode(gen.to(in))
//
//    def render(
//      key: List[String],
//      path: Path,
//      data: Input,
//      errors: ErrorTree,
//      messages: UniformMessages[Html]
//    ): Html = {
//      views.html.uniform.address(
//        key,
//        data,
//        errors,
//        messages
//      )
//    }
//  }
//
//  implicit def enumeratumField[A <: EnumEntry](implicit enum: Enum[A]): FormField[A, Html] = new FormField[A, Html] {
//
//    override def stats = new FormFieldStats(children = enum.values.length)
//
//    def decode(out: Input): Either[ErrorTree,A] = {out.toField[A](x =>
//      nonEmpty[String].apply(x) andThen
//        ( y => Validated.catchOnly[NoSuchElementException](enum.withName(y)).leftMap(_ => ErrorTree.oneErr(ErrorMsg("invalid"))))
//    )}.toEither
//
//    def encode(in: A): Input = Input.one(List(in.entryName))
//    def render(key: List[String],path: Path,data: Input,errors: ErrorTree,messages: UniformMessages[Html]): Html = {
//      val options = enum.values.map{_.entryName}
//      val existingValue = decode(data).map{_.entryName}.toOption
//      views.html.uniform.radios(
//        key,
//        options,
//        existingValue,
//        errors,
//        messages
//      )
//    }
//  }
}
