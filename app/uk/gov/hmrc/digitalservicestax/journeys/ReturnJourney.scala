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

object ReturnJourney {

  type ReturnTellTypes = Confirmation[Return] :: CYA[Return] :: GroupCompany :: NilTypes
  type ReturnAskTypes = Set[Activity] :: Money :: RepaymentDetails :: Percent :: Boolean :: List[GroupCompany] :: NilTypes

  private def message(key: String, args: String*) = {
    import play.twirl.api.HtmlFormat.escape
    Map(key -> Tuple2(key, args.toList.map { escape(_).toString } ))
  }

  def returnJourney[F[_] : Monad](
    interpreter: Language[F, ReturnTellTypes, ReturnAskTypes]
  ): F[Return] = {
    import interpreter._

    def askAlternativeCharge(applicableActivities: Set[Activity]): F[Map[Activity, Percent]] = {

      def askActivityReduced(actType: Activity): F[Percent] =
        { ask[Percent](s"report-${Activity.toUrl(actType)}-operating-margin") emptyUnless
          ask[Boolean](s"report-${Activity.toUrl(actType)}-loss").map { x => !x }}

      def askActivity(actType: Activity): F[Option[Percent]] = 
        { ask[Percent](s"report-${Activity.toUrl(actType)}-operating-margin") emptyUnless
          ask[Boolean](s"report-${Activity.toUrl(actType)}-loss").map { x => !x }} when
        ask[Boolean](s"report-${Activity.toUrl(actType)}-alternative-charge")
      
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


    def askAmountForCompanies(companies: List[GroupCompany]): F[Map[GroupCompany, Money]] = {
      companies.zipWithIndex.map{ case (co, i) => 
        interact[GroupCompany, Money](
          s"company-liabilities-$i",
          co,
          customContent = message(s"company-liabilities-$i.heading", co.name)
        ).map{(co, _)}
      }.sequence.map{_.toMap}
    }

    for {
      groupCos <- ask[List[GroupCompany]]("manage-companies", validation = Rule.minLength(1))
      activities <- ask[Set[Activity]]("select-activities")

      dstReturn <- (
        askAlternativeCharge(activities), 
        ask[Money]("relief-deducted") emptyUnless ask[Boolean]("report-cross-border-transaction-relief"),
        askAmountForCompanies(groupCos),
        ask[Money]("allowance-deducted"),
        ask[Money]("group-liability"),
        ask[RepaymentDetails]("bank-details") when ask[Boolean]("repayment")
      ).mapN(Return.apply)
      _ <- tell("check-your-answers", CYA(dstReturn))
    } yield dstReturn
  }

}
