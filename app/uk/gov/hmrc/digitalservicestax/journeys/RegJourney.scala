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
package journeys

import scala.language.higherKinds
import connectors.DSTService
import data._
import frontend.Kickout
import cats.Monad
import cats.implicits._
import java.time.LocalDate

import ltbs.uniform.{NonEmptyString => _, _}
import ltbs.uniform.validation._

object RegJourney {

  type RegTellTypes = Confirmation[Registration] :: CYA[Registration] :: Address :: Kickout :: Company :: Boolean :: NilTypes
  type RegAskTypes = UTR :: Postcode :: LocalDate :: ContactDetails :: String :: NonEmptyString :: Address :: UkAddress :: Boolean :: OptAddressLine :: NilTypes


  private def message(key: String, args: String*) = {
    import play.twirl.api.HtmlFormat.escape
    Map(key -> Tuple2(key, args.toList.map { escape(_).toString } ))
  }

  def addressLineLimit(addressType: String, addressKey: String): Rule[Address] = {
    Rule.condAtPath[Address](s"$addressType", addressKey)(
      {
        case add: Address if addressKey == "line1"  => add.line1.length <= 40
        case add: Address if addressKey == "line2"  => add.line2.length <= 40
        case add: Address if addressKey == "line3"  => add.line3.length <= 40
        case add: Address if addressKey == "town"   => add.line3.length <= 40
        case add: Address if addressKey == "line4"  => add.line4.length <= 40
        case add: Address if addressKey == "county" => add.line4.length <= 40
        case _ => true
      },
      "limit"
    )
  }

  def registrationJourney[F[_] : Monad](
    interpreter: Language[F, RegTellTypes, RegAskTypes],
    backendService: DSTService[F]
  ): F[Registration] = {
    import interpreter._



    for {
      
      companyRegWrapper <- backendService.lookupCompany() >>= { // gets a CompanyRegWrapper but converts to a company

        // found a matching company
        case Some(companyRW) =>
          for {
            confirmCompany <- interact[Company, Boolean]("confirm-company-details", companyRW.company)
            //TODO Here we need to sign the user out
            _ <- if (!confirmCompany) { tell("details-not-correct", Kickout("details-not-correct")) } else { (()).pure[F] }
          } yield companyRW // useSafeId is false, no utr or safeId sent

        // no matching company found
        case None => ask[Boolean]("check-unique-taxpayer-reference") >>= {
          case false =>
            (
              ask[NonEmptyString]("company-name"),
              ask[Address]("company-registered-office-address")
            ).mapN(Company.apply).map(CompanyRegWrapper(_, useSafeId = true))
          case true =>
            for {
              utr <- ask[UTR]("enter-utr")
              postcode <- ask[Postcode]("company-registered-office-postcode")
              companyOpt <- backendService.lookupCompany(utr, postcode)
              //The user shouln't be signed out here
              _ <- if (companyOpt.isEmpty) end("cannot-find-company", Kickout("details-not-correct")) else { (()).pure[F] }
              company = companyOpt.get.company
              safeId = companyOpt.get.safeId
              confirmCompany <- interact[Company, Boolean]("confirm-company-details", company)
              //TODO Check if we should be signing the user out
              _ <- if (!confirmCompany) { end("details-not-correct", Kickout("details-not-correct")) } else { (()).pure[F] }
            } yield CompanyRegWrapper(company, utr.some, safeId)
        }
      }

      registration <- (
        companyRegWrapper.pure[F],
        ask[Address](
          "alternate-contact",
          validation =
            addressLineLimit("UkAddress", "line1") alongWith
            addressLineLimit("UkAddress", "line2") alongWith
            addressLineLimit("UkAddress", "town") alongWith
            addressLineLimit("UkAddress", "county") alongWith
            addressLineLimit("ForeignAddress", "line1") alongWith
            addressLineLimit("ForeignAddress", "line2") alongWith
            addressLineLimit("ForeignAddress", "line3") alongWith
            addressLineLimit("ForeignAddress", "line4")
        ) when interact[Address, Boolean]("company-contact-address", companyRegWrapper.company.address).map{x => !x},
        ask[Boolean]("check-if-group") >>= {
          case true =>
            for {
              parentName <- ask[NonEmptyString]("ultimate-parent-company-name",
                validation =
                  Rule.cond[NonEmptyString](
                    _.length < 160,
                    "error.length"
                  ) followedBy
                  Rule.cond[NonEmptyString](
                    _.matches("""^[a-zA-Z0-9- '&\\/]{1,105}$"""),
                    "format"
                  )
              )
              parentAddress <- ask[Address](
                "ultimate-parent-company-address",
                customContent = message("ultimate-parent-company-address.heading", parentName)
              )
            } yield Company(parentName, parentAddress).some
          case false =>
            Option.empty[Company].pure[F]
        },
        ask[ContactDetails]("contact-details"),
        ask[Boolean]("check-liability-date") flatMap {
          case true => LocalDate.of(2020,4,6).pure[F]
          case false =>
            ask[LocalDate]("liability-start-date",
              validation = Rule.min(LocalDate.of(2020,4,6))
            )
        },
        ask[LocalDate]("accounting-period-end-date"),
        None.pure[F]
      ).mapN(Registration.apply)
      _ <- tell("check-your-answers", CYA(registration))
    } yield (registration)
  }

}
