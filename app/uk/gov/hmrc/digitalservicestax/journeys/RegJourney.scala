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

import uk.gov.hmrc.digitalservicestax.frontend._

import ltbs.uniform.{NonEmptyString => _, _}
import ltbs.uniform.validation._

object RegJourney {

  type RegTellTypes = Confirmation[Registration] :: CYA[Registration] :: Address :: Kickout :: Company :: Boolean :: NilTypes
  type RegAskTypes = UTR :: Postcode :: LocalDate :: ContactDetails :: String :: NonEmptyString :: Address :: UkAddress :: ForeignAddress :: Boolean :: MandatoryAddressLine :: CompanyName :: NilTypes


  private def message(key: String, args: String*) = {
    import play.twirl.api.HtmlFormat.escape
    Map(key -> Tuple2(key, args.toList.map { escape(_).toString } ))
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
            for {
              companyName <- ask[CompanyName]("company-name")
              companyAddress <- ask[Boolean](
                "check-company-registered-office-address",
                customContent = (
                    message("check-company-registered-office-address.heading", companyName) ++
                    message("check-company-registered-office-address.required", companyName)
                  )
              ) >>=[Address] {
                case true =>
                  ask[UkAddress](
                    "company-registered-office-uk-address",
                      customContent = message("company-registered-office-uk-address.heading", companyName)
                    ).map(identity)
                case false =>
                  ask[ForeignAddress](
                    "company-registered-office-international-address",
                    customContent = message("company-registered-office-international-address.heading", companyName)
                  ).map(identity)
              }
            } yield CompanyRegWrapper(Company(companyName, companyAddress), useSafeId = true)
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
        (
          ask[Boolean]("check-contact-address") >>=[Address] {
            case true =>
              ask[UkAddress](
                "contact-uk-address"
              ).map(identity)
            case false =>
              ask[ForeignAddress](
                "contact-international-address"
              ).map(identity)
          }
        ) when interact[Address, Boolean]("company-contact-address", companyRegWrapper.company.address).map{x => !x},
        ask[Boolean]("check-if-group") >>= {
          case true =>
            for {
              parentName <- ask[CompanyName]("ultimate-parent-company-name")
              parentAddress <-
                ask[Boolean](
                  "check-ultimate-parent-company-address",
                  customContent = (
                      message("check-ultimate-parent-company-address.heading", parentName) ++
                      message("check-ultimate-parent-company-address.required", parentName)
                    )
                ) >>=[Address] {
                  case true =>
                    ask[UkAddress](
                      "ultimate-parent-company-uk-address",
                      customContent = message("ultimate-parent-company-uk-address.heading", parentName)
                    ).map(identity)
                  case false =>
                    ask[ForeignAddress](
                      "ultimate-parent-company-international-address",
                      customContent =
                        message("ultimate-parent-company-international-address.heading", parentName)
                    ).map(identity)
                }
            } yield Company(parentName, parentAddress).some
          case false =>
            Option.empty[Company].pure[F]
        },
        ask[ContactDetails]("contact-details"),
        for (
        startDate <- ask[Boolean]("check-liability-date") flatMap {
          case true => LocalDate.of(2020,4,6).pure[F]
          case false =>
            ask[LocalDate]("liability-start-date",
              validation =
                Rule.min(LocalDate.of(2020,4,6), "minimum-date") followedBy
                Rule.max(LocalDate.now.plusYears(1), "maximum-date"),
              customContent = message("liability-start-date.maximum-date", formatDate(LocalDate.now.plusYears(1)))
            )
        }
        periodEnd <- ask[LocalDate](
          "accounting-period-end-date",
          customContent = message("accounting-period-end-date.maximum-date", formatDate(LocalDate.now.plusYears(1)))
        )) yield()
        None.pure[F]
      ).mapN(Registration.apply)
      _ <- tell("check-your-answers", CYA(registration))
    } yield (registration)
  }

}
