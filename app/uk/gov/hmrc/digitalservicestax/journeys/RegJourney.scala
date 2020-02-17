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

import data._

import cats.Monad
import cats.implicits._
import java.time.LocalDate
import ltbs.uniform.{NonEmptyString => _, _}
import ltbs.uniform.validation._

object RegJourney {

  type RegTellTypes = Confirmation[Registration] :: CYA[Registration] :: Address :: UkAddress :: Kickout :: Company :: Boolean :: NilTypes
  type RegAskTypes = UTR :: Postcode :: LocalDate :: ContactDetails :: String :: NonEmptyString :: Address :: UkAddress :: Boolean :: NilTypes

  def registrationJourney[F[_] : Monad](
    interpreter: Language[F, RegTellTypes, RegAskTypes],
    backendService: BackendService[F]
  )(utr: UTR): F[Registration] = {
    import interpreter._

    for {
      company <- backendService.matchedCompany() >>= {

        // found a matching company
        case Some(company) =>
          for {
            confirmCompany <- interact[Company, Boolean]("confirm-company", company)
            _ <- if (!confirmCompany) { tell("logout-prompt", Kickout("logout-prompt")) } else { (()).pure[F] }
          } yield (company)

        // no matching company found
        case None => ask[Boolean]("check-unique-taxpayer-reference") >>= {
          case false =>
            (
              ask[NonEmptyString]("company-name"),
              ask[Address]("company-registered-office-address")
            ).mapN(Company.apply)
          case true =>
            for {
              utr <- ask[UTR]("enter-utr")
              postcode <- ask[Postcode]("postcode")
              company <- backendService.lookup(utr, postcode) map {
                _.getOrElse(throw new IllegalStateException("lookup failed"))
              }
              confirmCompany <- interact[Company, Boolean]("confirm-company-details", company)
              _ <- if (!confirmCompany) { tell("logout-prompt", Kickout("logout-prompt")) } else { (()).pure[F] }
            } yield (company)
        }
      }
    
      registration <- (
        utr.pure[F],
        company.pure[F],
        ask[Address]("alternate-contact") when
          interact[Address, Boolean]("company-contact-address", company.address).map{x => !x},
        (
          ask[NonEmptyString]("ultimate-parent-company-name"),
          ask[Address]("ultimate-parent-company-address")
        ).mapN(Company.apply) when ask[Boolean]("check-if-group"),
        ask[ContactDetails]("contact-details"),
        (ask[LocalDate]("liability-start-date") when ask[Boolean]("check-liability-date")).map{_.getOrElse(LocalDate.of(2020, 4,1))},
        ask[LocalDate]("accounting-period-end-date")
      ).mapN(Registration.apply)
      _ <- tell("check-your-answers", CYA(registration))
      _ <- tell("accounting-period-end-date", Confirmation(registration))
    } yield (registration)
  }

}
