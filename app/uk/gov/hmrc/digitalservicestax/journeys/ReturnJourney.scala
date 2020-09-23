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
import frontend.formatDate

import cats.Monad
import cats.implicits._
import ltbs.uniform.{NonEmptyString => _, _}
import ltbs.uniform.validation._

import play.twirl.api.{Html}

object ReturnJourney {

  type ReturnTellTypes = Confirmation[Return] :: CYA[(Return, Period)] :: GroupCompany :: NilTypes
  type ReturnAskTypes = Set[Activity] :: Money :: RepaymentDetails :: Percent :: Boolean :: List[GroupCompany] :: NilTypes

  private def message(key: String, args: String*) = {
    import play.twirl.api.HtmlFormat.escape
    Map(key -> Tuple2(key, args.toList.map { escape(_).toString } ))
  }

  def returnJourney[F[_] : Monad](
    interpreter: Language[F, ReturnTellTypes, ReturnAskTypes],
    period: Period,
    registration: Registration
  ): F[Return] = {
    import interpreter._

    val isGroup = registration.ultimateParent.isDefined
    val isGroupMessage = if(isGroup) "group" else "company"

    def askAlternativeCharge(applicableActivities: Set[Activity]): F[Map[Activity, Percent]] = {

      def askActivityReduced(actType: Activity): F[Percent] =
        { ask[Percent](
          s"report-${Activity.toUrl(actType)}-operating-margin",
          customContent =
            message(s"report-${Activity.toUrl(actType)}-operating-margin.heading", isGroupMessage) ++
            message(s"report-${Activity.toUrl(actType)}-operating-margin.required", isGroupMessage) ++
            message(s"report-${Activity.toUrl(actType)}-operating-margin.not-a-number", isGroupMessage)
        ) emptyUnless ask[Boolean](
            s"report-${Activity.toUrl(actType)}-loss",
            customContent =
              message(s"report-${Activity.toUrl(actType)}-loss.heading", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-loss.required", isGroupMessage)
          ).map { x => !x }}

      def askActivity(actType: Activity): F[Option[Percent]] = 
        { ask[Percent](
            s"report-${Activity.toUrl(actType)}-operating-margin",
            customContent =
              message(s"report-${Activity.toUrl(actType)}-operating-margin.heading", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-operating-margin.required", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-operating-margin.not-a-number", isGroupMessage)
        ) emptyUnless ask[Boolean](
            s"report-${Activity.toUrl(actType)}-loss",
            customContent =
              message(s"report-${Activity.toUrl(actType)}-loss.heading", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-loss.required", isGroupMessage)
        ).map { x => !x }} when ask[Boolean](s"report-${Activity.toUrl(actType)}-alternative-charge")
      
      val allEntries: List[F[(Activity, Option[Percent])]] =
        if(applicableActivities.size == 1) {
          applicableActivities.toList.map { actType =>
            askActivityReduced(actType).map { percent =>
              (actType, percent.some)
            }
          }
        } else {
          applicableActivities.toList.map { actType =>
            askActivity(actType).map {
              (actType, _)
            }
          }
        }

      allEntries.sequence.map { _.collect {
        case (a, Some(x)) => a -> x
      }.toMap }
    } emptyUnless ask[Boolean]("report-alternative-charge")


    def askAmountForCompanies(companies: Option[List[GroupCompany]]): F[Map[GroupCompany, Money]] = {
      companies.fold(Map.empty[GroupCompany, Money].pure[F]){_.zipWithIndex.map{ case (co, i) =>
        interact[GroupCompany, Money](
          s"company-liabilities-$i",
          co,
          customContent =
            Html(message(s"company-liabilities-$i.heading", co.name, formatDate(period.start), formatDate(period.end))) ++
            Html(message(s"company-liabilities-$i.required", co.name)) ++
            Html(message(s"company-liabilities-$i.not-a-number", co.name)) ++
            Html(message(s"company-liabilities-$i.length.exceeded", co.name)) ++
            Html(message(s"company-liabilities-$i.invalid", co.name))
        ).map{(co, _)}
      }.sequence.map{_.toMap}}
    }

    for {
      groupCos <- ask[List[GroupCompany]]("manage-companies", validation = Rule.minLength(1)) when isGroup
      activities <- ask[Set[Activity]]("select-activities", validation = Rule.minLength(1))

      dstReturn <- (
        askAlternativeCharge(activities),
        ask[Boolean]("report-cross-border-transaction-relief") >>= {
          case true => ask[Money]("relief-deducted")
          case false => Money(BigDecimal(0).setScale(2)).pure[F]
        },
        askAmountForCompanies(groupCos) emptyUnless isGroup,
        ask[Money]("allowance-deducted"),
        ask[Money]("group-liability",
          customContent =
            Html(message("group-liability.heading", isGroupMessage, formatDate(period.start), formatDate(period.end))) ++
            Html(message("group-liability.required", isGroupMessage, formatDate(period.start), formatDate(period.end))) ++
            Html(message("group-liability.not-a-number", isGroupMessage)) ++
            Html(message("group-liability.length.exceeded", isGroupMessage)) ++
            Html(message("group-liability.invalid", isGroupMessage))
        ),
        ask[RepaymentDetails]("bank-details") when ask[Boolean](
            "repayment",
            customContent =
            Html(message("repayment.heading", formatDate(period.start), formatDate(period.end))) ++
            Html(message("repayment.required", formatDate(period.start), formatDate(period.end)))
        )
      ).mapN(Return.apply)
      _ <- tell("check-your-answers", CYA((dstReturn, period)))
    } yield dstReturn
  }

}
