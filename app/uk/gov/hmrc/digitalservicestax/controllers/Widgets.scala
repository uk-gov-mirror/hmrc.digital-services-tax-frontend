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

import cats.data.Validated
import cats.implicits._
import enumeratum._
import ltbs.uniform.common.web.GenericWebTell
import ltbs.uniform.common.web.{FormField, FormFieldStats}
import ltbs.uniform.interpreters.playframework.Breadcrumbs
import ltbs.uniform.validation.Rule._
import ltbs.uniform.validation._
import ltbs.uniform.{NonEmptyString => _, _}
import play.twirl.api.Html
import play.twirl.api.HtmlFormat.Appendable
import shapeless.tag, tag.{@@}
import uk.gov.hmrc.digitalservicestax._
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.frontend.{RichAddress, Kickout}

trait Widgets {

  def appConfig: AppConfig

  implicit val twirlStringField: FormField[String, Html] = twirlStringFields()

  type CustomStringRenderer =
    (List[String], String, ErrorTree, UniformMessages[Html], Option[String]) => Appendable

  def twirlStringFields(
    autoFields: Option[String] = None,
    customRender: CustomStringRenderer = views.html.uniform.string.apply(_,_,_,_,_)
  ): FormField[String, Html] = new FormField[String, Html] {
    def decode(out: Input): Either[ErrorTree, String] =
      out.toStringField().toEither

    def encode(in: String): Input = Input.one(List(in))

    def render(
      pageKey: List[String],
      fieldKey: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      val existingValue: String = data.valueAtRoot.flatMap{_.headOption}.getOrElse("")
      customRender(fieldKey, existingValue, errors, messages, autoFields)
    }
  }

  def validatedVariant[BaseType](validated: ValidatedType[BaseType])(
    implicit baseForm: FormField[BaseType, Html]
  ): FormField[BaseType @@ validated.Tag, Html] =
    baseForm.simap{x =>
      Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }{x => x: BaseType}

  def validatedString(
    validated: ValidatedType[String],
    maxLen: Int = Integer.MAX_VALUE
  )(
    implicit baseForm: FormField[String, Html]
  ): FormField[String @@ validated.Tag, Html] =
    baseForm.simap{
      case x if x.trim.isEmpty => Left(ErrorMsg("required").toTree)
      case l if l.length > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x => Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }{x => x: String}

  def inlineOptionString(
    validated: ValidatedType[String],
    maxLen: Int = Integer.MAX_VALUE
  ): FormField[Option[String @@ validated.Tag], Html] =
    twirlStringField.simap{
      case "" => Right(None)
      case l if l.length > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x  => Either.fromOption(
        validated.of(x).map(Some(_)), ErrorMsg("invalid").toTree
      )
    }{
      case None => ""
      case Some(x) => x.toString
    }

  def validatedBigDecimal(
    validated: ValidatedType[BigDecimal],
    maxLen: Int = Integer.MAX_VALUE
  )(
    implicit baseForm: FormField[BigDecimal, Html]
  ): FormField[BigDecimal @@ validated.Tag, Html] =
    baseForm.simap{
      case l if l.precision > maxLen => Left(ErrorMsg("length.exceeded").toTree)
      case x => Either.fromOption(validated.of(x), ErrorMsg("invalid").toTree)
    }{x => x: BigDecimal}

  implicit def postcodeField                  = validatedString(Postcode)
  implicit def nesField                       = validatedVariant(NonEmptyString)
  implicit def utrField                       = validatedString(UTR)
  implicit def emailField                     = validatedString(Email, 132)
  implicit def phoneField       = validatedString(PhoneNumber, 24)(twirlStringFields(
    customRender = views.html.uniform.phonenumber.apply _
  ))
  implicit def percentField                   = validatedVariant(Percent)
  implicit def moneyField                     = validatedBigDecimal(Money, 15)
  implicit def accountNumberField             = validatedString(AccountNumber)
  implicit def BuildingSocietyRollNumberField = inlineOptionString(BuildingSocietyRollNumber, 18)
  implicit def accountNameField               = validatedString(AccountName, 35)
  implicit def sortCodeField    = validatedString(SortCode)(twirlStringFields(
    customRender = views.html.uniform.string(_,_,_,_,_,"form-control form-control-1-4")
  ))
  
  implicit def ibanField                      = validatedString(IBAN, 34)
  implicit def companyNameField               = validatedString(CompanyName, 105)
  implicit def mandatoryAddressField          = validatedString(AddressLine, 35)
  implicit def optAddressField                = inlineOptionString(AddressLine, 35)
  implicit def restrictField                  = validatedString(RestrictiveString, 35)

  implicit def optUtrField: FormField[Option[UTR], Html] = inlineOptionString(UTR)

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
        Rule.nonEmpty[String].apply(x.replace(",", "")) andThen
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
      pageKey: List[String],
      fieldKey: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      val options = if (pageKey.contains("about-you") || pageKey.contains("user-employed")) List(False, True) else List(True, False)
      val existingValue = data.toStringField().toOption
      views.html.uniform.radios(fieldKey,
        options,
        existingValue,
        errors,
        messages)
    }
  }

  implicit val twirlCountryCodeField = new FormField[CountryCode, Html] {

    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]): Html =
      views.html.helpers.country_select(
        fieldKey.mkString("."),
        data.values.flatten.headOption,
        errors.nonEmpty,
        messages
      )

    override def encode(in: CountryCode): Input =
      validatedVariant(CountryCode).encode(in)

    override def decode(out: Input): Either[ErrorTree, CountryCode] =
      validatedVariant(CountryCode).decode(out)

  }

  implicit val twirlDateField: FormField[LocalDate, Html] =
    new FormField[LocalDate, Html] {
    override def stats = FormFieldStats(children = 3)

    def decode(out: Input): Either[ErrorTree, LocalDate] = {

      def stringAtKey(key: String): Validated[List[String], String] =
        Validated.fromOption(
          out.valueAt(key).flatMap{_.find(_.trim.nonEmpty)},
          List(key)
        )

      (
        stringAtKey("year"),
        stringAtKey("month"),
        stringAtKey("day")
      ).tupled
       .leftMap{x => ErrorMsg(x.reverse.mkString("-and-") + ".empty").toTree}
       .toEither
       .flatMap{ case (ys,ms,ds) =>

         val asNumbers: Either[Exception, (Int,Int,Int)] =
           Either.catchOnly[NumberFormatException]{
             (ys.toInt, ms.toInt, ds.toInt)
           }

         asNumbers.flatMap { case (y,m,d) =>
           Either.catchOnly[java.time.DateTimeException]{
             LocalDate.of(y,m,d)
           }
         }.leftMap(_ => ErrorTree.oneErr(ErrorMsg("not-a-date")))
       }
    }

      def encode(in: LocalDate): Input = Map(
        List("year") → in.getYear(),
        List("month") → in.getMonthValue(),
        List("day") → in.getDayOfMonth()
      ).mapValues(_.toString.pure[List])

      def render(
        pageKey: List[String],        
        fieldKey: List[String],
        path: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Html = {
        views.html.uniform.date(
          fieldKey,
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

    override def stats = FormFieldStats(children = 5)

    def decode(out: Input): Either[ErrorTree, UkAddress] = ffhlist.decode(out).map(gen.from)
    def encode(in: UkAddress): Input = ffhlist.encode(gen.to(in))

    def render(
      pagekey: List[String],
      fieldKey: List[String],      
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      // TODO pass thru fieldKey
      views.html.uniform.address(
        fieldKey,
        data,
        errors,
        messages,
        "UkAddress"
      )
    }
  }

  implicit def twirlForeignAddressField[T](
    implicit gen: shapeless.LabelledGeneric.Aux[ForeignAddress,T],
    ffhlist: FormField[T, Html]
  ): FormField[ForeignAddress, Html] = new FormField[ForeignAddress, Html] {

    override def stats = FormFieldStats(children = 5)

    def decode(out: Input): Either[ErrorTree, ForeignAddress] = ffhlist.decode(out).map(gen.from)
    def encode(in: ForeignAddress): Input = ffhlist.encode(gen.to(in))

    def render(
      pagekey: List[String],
      fieldKey: List[String],
      path: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      views.html.uniform.address(
        fieldKey,
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
      def render(
        pageKey: List[String],
        fieldKey: List[String],        
        path: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Html = {
        val options = enum.values.map{_.entryName}
        val existingValue = decode(data).map{_.entryName}.toOption
        views.html.uniform.radios(
          fieldKey,
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
        pageKey: List[String],
        fieldKey: List[String],
        breadcrumbs: Breadcrumbs,
        data: Input,
        errors: ErrorTree,
        messages: UniformMessages[Html]
      ): Html = {
        val options = enum.values.map{_.entryName}
        val existingValues: Set[String] = decode(data).map{_.map{_.entryName}}.getOrElse(Set.empty)
        views.html.uniform.checkboxes(
          fieldKey,
          options,
          existingValues,
          errors,
          messages
        )
      }

    }

  implicit val addressTell = new GenericWebTell[Address, Html] {
    override def render(in: Address, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<p>${in.lines.map{x => s"<span class='govuk-body-m'>${x.escapeHtml}</span>"}.mkString("<br/>")}</p>"
      )
  }

  implicit val kickoutTell = new GenericWebTell[Kickout, Html] {
    override def render(in: Kickout, key: String, messages: UniformMessages[Html]): Html =
      views.html.end.kickout(key)(messages, appConfig)
  }

  implicit val groupCoTell = new GenericWebTell[GroupCompany, Html] {
    override def render(in: GroupCompany, key: String, messages: UniformMessages[Html]): Html =
      Html("")
  }

  implicit val companyTell = new GenericWebTell[Company, Html] {
    override def render(in: Company, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<p class='govuk-body-l' id='${key}-content'>" +
          s"${in.name.toString.escapeHtml}</br>" +
          s"<span class='govuk-body-m'>" +
          s"${in.address.lines.map{_.escapeHtml}.mkString("</br>")}" +
          s"</span>" +
          "</p>"
      )
  }

  implicit val booleanTell = new GenericWebTell[Boolean, Html] {
    override def render(in: Boolean, key: String, messages: UniformMessages[Html]): Html =
      Html(in.toString)
  }
}
