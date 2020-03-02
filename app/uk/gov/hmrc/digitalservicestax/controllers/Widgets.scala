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

package uk.gov.hmrc.digitalservicestax.controllers

import java.time.LocalDate

import ltbs.uniform
import ltbs.uniform.common.web.GenericWebTell

//import cats.data.Validated
import cats.implicits._
import enumeratum._
import ltbs.uniform.common.web.{FormField, FormFieldStats}
import ltbs.uniform.interpreters.playframework.Breadcrumbs
import ltbs.uniform.validation.Rule._
import ltbs.uniform.validation._
import ltbs.uniform.{NonEmptyString => _, RichEither => _, _}
import play.twirl.api.Html
import uk.gov.hmrc.digitalservicestax._
import uk.gov.hmrc.digitalservicestax.data._
import shapeless.tag.{@@}


import cats.implicits._
import cats.{Monoid, Applicative, Monad, Eq, Semigroup}
import cats.data.{NonEmptyList, Validated}
import shapeless.tag, tag.{@@}
import collection.immutable.ListMap
import uniform.validation.{Rule, Transformation}

trait Widgets {

  implicit val twirlStringField: FormField[String, Html] = twirlStringFields()

  def twirlStringFields(autoFields: Option[String] = None): FormField[String, Html] = new FormField[String, Html] {
    def decode(out: Input): Either[ErrorTree, String] =
      out.toStringField().toEither

    def encode(in: String): Input = Input.one(List(in))

    def render(
      key: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      val existingValue: String = data.valueAtRoot.flatMap{_.headOption}.getOrElse("")
      views.html.uniform.string(key, existingValue, errors, messages, autoFields)
    }
  }

  def validatedVariant[BaseType](validated: ValidatedType[BaseType])(
    implicit baseForm: FormField[BaseType, Html]
  ): FormField[BaseType @@ validated.Tag, Html] =
    baseForm.simap{x =>
      Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }{x => x: BaseType}

  implicit def postcodeField    = validatedVariant(Postcode)
  implicit def nesField         = validatedVariant(NonEmptyString)
  implicit def utrField         = validatedVariant(UTR)
  implicit def countrycodeField = validatedVariant(CountryCode)
  implicit def emailField       = validatedVariant(Email)
  implicit def phoneField       = validatedVariant(PhoneNumber)
  implicit def percentField     = validatedVariant(Percent)
  implicit def accountField     = validatedVariant(AccountNumber)
  implicit def sortCodeField    = validatedVariant(SortCode)
  implicit def ibanField        = validatedVariant(IBAN)

  implicit val intField: FormField[Int,Html] =
    twirlStringFields().simap(x => 
      {
        Rule.nonEmpty[String].apply(x) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toInt)
      }.toEither
    )(_.toString)

  implicit val byteField: FormField[Byte,Html] =
    twirlStringFields().simap(x => 
      {
        Rule.nonEmpty[String].apply(x) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toByte)
      }.toEither
    )(_.toString)

  implicit val longField: FormField[Long,Html] =
    twirlStringFields().simap(x => 
      {
        Rule.nonEmpty[String].apply(x) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(_.toLong)
      }.toEither
    )(_.toString)

  implicit val bigdecimalField: FormField[BigDecimal,Html] =
    twirlStringFields().simap(x => 
      {
        Rule.nonEmpty[String].apply(x) andThen
        Transformation.catchOnly[NumberFormatException]("not-a-number")(BigDecimal.apply)
      }.toEither
    )(_.toString)


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
      path: Breadcrumbs,
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

  implicit val twirlDateField: FormField[LocalDate, Html] =
    new FormField[LocalDate, Html] {
    override def stats = FormFieldStats(children = 3)

    def decode(out: Input): Either[ErrorTree, LocalDate] = {

      def intAtKey(key: String): Validated[List[String], Int] =
        Validated.fromOption(
          out.valueAt(key).flatMap{_.find(_.trim.nonEmpty)},
          List(key)
        ).andThen{
          x => Validated.catchOnly[NumberFormatException](x.toInt).leftMap(_ => List(key))
        }

      (
        intAtKey("year"),
        intAtKey("month"),
        intAtKey("day")
      ) .tupled
        .leftMap{x => ErrorMsg(x.reverse.mkString("-and-") + ".empty").toTree}
        .toEither
        .flatMap{ case (y,m,d) =>
          Either.catchOnly[java.time.DateTimeException]{
            LocalDate.of(y,m,d)
          }.leftMap(_ => ErrorTree.oneErr(ErrorMsg("not-a-date")))
        }
    }

      def encode(in: LocalDate): Input = Map(
        List("year") → in.getYear(),
        List("month") → in.getMonthValue(),
        List("day") → in.getDayOfMonth()
      ).mapValues(_.toString.pure[List])

      def render(
        key: List[String],
        path: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
    messages: UniformMessages[Html]
      ): Html = {
        views.html.uniform.date(
          key,
          data,
          errors,
          messages
        )
      }
    }

  implicit def twirlUKAddressField[T](
    implicit gen: shapeless.LabelledGeneric.Aux[UkAddress,T],
    ffhlist: FormField[T, Html]
  ): FormField[UkAddress, Html] = new FormField[UkAddress, Html] {

    def decode(out: Input): Either[ErrorTree, UkAddress] = {
      // Not used due to international postcodes
      // val postCodeRegex = """([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9]?[A-Za-z]))))\s?[0-9][A-Za-z]{2}|.{0})"""

      //TODO check transformations with ltbs
      (
        out.subField[NonEmptyString]("line1", {Transformation.catchOnly[IllegalArgumentException]("not-a-none-empty-string")(NonEmptyString(_))}),
        out.stringSubField("line2"),
        out.stringSubField("town"),
        out.stringSubField("county"),
        out.subField[Postcode]("postcode", {Transformation.catchOnly[IllegalArgumentException]("not-a-postcode")(Postcode(_))})
      ).mapN(UkAddress).toEither
    }

    def encode(in: UkAddress): Input = ffhlist.encode(gen.to(in))

    def render(
      key: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      // TODO pass thru fieldKey
      views.html.uniform.address(
        key,
        data,
        errors,
        messages
      )
    }
  }

  implicit def enumeratumField[A <: EnumEntry](implicit enum: Enum[A]): FormField[A, Html] =
    new FormField[A, Html] {

      override def stats = new FormFieldStats(children = enum.values.length)

      def decode(out: Input): Either[ErrorTree,A] = {out.toField[A](x =>
        nonEmpty[String].apply(x) andThen
          ( y => Validated.catchOnly[NoSuchElementException](enum.withName(y)).leftMap(_ => ErrorTree.oneErr(ErrorMsg("invalid"))))
      )}.toEither

      def encode(in: A): Input = Input.one(List(in.entryName))
      def render(key: List[String],path: Breadcrumbs,data: Input,errors: ErrorTree,messages: UniformMessages[Html]): Html = {
        val options = enum.values.map{_.entryName}
        val existingValue = decode(data).map{_.entryName}.toOption
        views.html.uniform.radios(
          key,
          options,
          existingValue,
          errors,
          messages
        )
      }
    }

  implicit def enumeratumSetField[A <: EnumEntry](implicit enum: Enum[A]): FormField[Set[A], Html] =
    new FormField[Set[A], Html] {
      override def stats = new FormFieldStats(children = enum.values.length)

      def decode(out: Input): Either[ErrorTree,Set[A]] = {
        val i: List[String] = out.valueAtRoot.getOrElse(Nil)
        val r: List[Either[ErrorTree, A]] = i.map{x =>
          Either.catchOnly[NoSuchElementException](enum.withName(x))
            .leftMap(_ => ErrorTree.oneErr(ErrorMsg("invalid")))
        }
        r.sequence.map{_.toSet}
      }

      // Members declared in ltbs.uniform.common.web.Codec
      def encode(in: Set[A]): Input =
        Map(Nil -> in.toList.map{_.entryName})

      // Members declared in ltbs.uniform.common.web.FormField
      def render(
        key: List[String],
        breadcrumbs: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Html = {
        val options = enum.values.map{_.entryName}
        val existingValues: Set[String] = decode(data).map{_.map{_.entryName}}.getOrElse(Set.empty)
        views.html.uniform.checkboxes(
          key,
          options,
          existingValues,
          errors,
          messages
        )
      }

    }

  implicit def unitTell: GenericWebTell[UkAddress, Html] = new GenericWebTell[UkAddress, Html] {
    def render(in: UkAddress, key: String, messages: UniformMessages[Html]): Html = Html("heyeyeye")
  }



  // implicit def oddballField2(
  //   implicit threeBools: FormField[(
  //     Boolean,
  //     Boolean,
  //     Boolean
  //   ), Html]
  // ): FormField[Set[data.Activity],Html] = {
  //   import data.Activity, Activity.{SocialMedia, SearchEngine, OnlineMarketplace}
  //   threeBools.imap{case (a,b,c) =>
  //       Set.apply[Option[Activity]](
  //         Some(SocialMedia).filter(_ => a),
  //         Some(SearchEngine).filter(_ => b),
  //         Some(OnlineMarketplace).filter(_ => c)          
  //       ).flatten
  //   }{ theset => 
  //     (
  //       theset.contains(SocialMedia),
  //       theset.contains(SearchEngine),
  //       theset.contains(OnlineMarketplace)        
  //     )
  //   }

  // }
  
}
