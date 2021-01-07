/*
 * Copyright 2021 HM Revenue & Customs
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

  type RegTellTypes = Confirmation[Registration] :: CYA[(Registration, Boolean, Boolean)] :: Address :: Kickout :: Company :: Boolean :: NilTypes
  type RegAskTypes = UTR :: Postcode :: LocalDate :: ContactDetails :: UkAddress :: ForeignAddress :: Boolean :: CompanyName :: NilTypes


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
      globalRevenues <- ask[Boolean]("global-revenues")
      _ <- if (!globalRevenues) { end("global-revenues-not-eligible", Kickout("global-revenues-not-eligible")) } else { (()).pure[F] }
      ukRevenues <- ask[Boolean]("uk-revenues")
      _ <- if (!ukRevenues) { end("uk-revenues-not-eligible", Kickout("uk-revenues-not-eligible")) } else { (()).pure[F] }

      companyRegWrapper <- backendService.lookupCompany() >>= { // gets a CompanyRegWrapper but converts to a company

        // found a matching company
        case Some(companyRW) =>
          for {
            confirmCompany <- interact[Company, Boolean]("confirm-company-details", companyRW.company)
            //TODO Here we need to sign the user out
            _ <- if (!confirmCompany) { end("details-not-correct", Kickout("details-not-correct")) } else { (()).pure[F] }
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


      registration <- {
        for {
          companyRegWrapper <- companyRegWrapper.pure[F]
          contactAddress <- (
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
            ) when interact[Address, Boolean]("company-contact-address", companyRegWrapper.company.address).map{x => !x}
          isGroup <- ask[Boolean]("check-if-group")
          ultimateParent <- if(isGroup){
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
           } else {
             Option.empty[Company].pure[F]
           }
          contactDetails <- ask[ContactDetails]("contact-details")
          groupMessage = if(isGroup) "group" else "company"
          liabilityDate <- ask[Boolean] (
            "check-liability-date",
            customContent = message("check-liability-date.required", groupMessage)
          ) flatMap {
              case true => LocalDate.of(2020, 4, 1).pure[F]
              case false =>
                ask[LocalDate]("liability-start-date",
                  validation =
                    Rule.min(LocalDate.of(2020, 4, 1), "minimum-date") followedBy
                    Rule.max(LocalDate.now.plusYears(1), "maximum-date"),
                  customContent =
                    message("liability-start-date.maximum-date", groupMessage, formatDate(LocalDate.now.plusYears(1))) ++
                    message("liability-start-date.minimum-date", groupMessage) ++
                    message("liability-start-date.day-and-month-and-year.empty", groupMessage) ++
                    message("liability-start-date.day.empty", groupMessage) ++
                    message("liability-start-date.month.empty", groupMessage) ++
                    message("liability-start-date.year.empty", groupMessage) ++
                    message("liability-start-date.day-and-month.empty", groupMessage) ++
                    message("liability-start-date.day-and-year.empty", groupMessage) ++
                    message("liability-start-date.month-and-year.empty", groupMessage)
                )
          }
          periodEndDate <- ask[LocalDate] (
            "accounting-period-end-date",
            validation =
              liabilityDate match {
                case ld if ld == LocalDate.of(2020, 4, 1) =>
                  Rule.min(LocalDate.of(2020, 4, 2), "minimum-date") followedBy
                  Rule.max(liabilityDate.plusYears(1).minusDays(1), "fixed-maximum-date")
                case _ =>
                  Rule.min(LocalDate.of(2020, 4, 2), "minimum-date") followedBy
                  Rule.max(liabilityDate.plusYears(1), "maximum-date")
              },
            customContent =
              message("accounting-period-end-date.maximum-date", groupMessage, formatDate(liabilityDate.plusYears(1).plusDays(1))) ++
              message("accounting-period-end-date.fixed-maximum-date", groupMessage, formatDate(liabilityDate.plusYears(1).minusDays(1))) ++
              message("accounting-period-end-date.minimum-date", groupMessage) ++
              message("accounting-period-end-date.day-and-month-and-year.empty", groupMessage) ++
              message("accounting-period-end-date.day.empty", groupMessage) ++
              message("accounting-period-end-date.month.empty", groupMessage) ++
              message("accounting-period-end-date.year.empty", groupMessage) ++
              message("accounting-period-end-date.day-and-month.empty", groupMessage) ++
              message("accounting-period-end-date.day-and-year.empty", groupMessage) ++
              message("accounting-period-end-date.month-and-year.empty", groupMessage)
          )
          emptyRegNo <- None.pure[F]
        } yield Registration(companyRegWrapper, contactAddress , ultimateParent, contactDetails, liabilityDate, periodEndDate, emptyRegNo)
      }

      _ <- tell("check-your-answers", CYA((registration, globalRevenues, ukRevenues)))
    } yield (registration)
  }

}
