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
import ltbs.uniform._
import ltbs.uniform.validation._

object ReturnJourney {

  type ReturnTellTypes = Confirmation[Return] :: CYA[Return] :: GroupCompany :: NilTypes
  type ReturnAskTypes = RepaymentDetails :: Set[Activity] :: Money :: Percent :: Boolean :: List[GroupCompany] :: NilTypes

  def returnJourney[F[_] : Monad](
    interpreter: Language[F, ReturnTellTypes, ReturnAskTypes]
  ): F[Return] = {
    import interpreter._

    def askAlternativeCharge(applicableActivities: Set[Activity]): F[Map[Activity, Percent]] = {

      //TODO Refactor

      def askActivityReduced(actType: Activity): F[Percent] =
        { ask[Percent](s"report-$actType-operating-margin") emptyUnless
          ask[Boolean](s"report-$actType-loss")}

      def askActivity(actType: Activity): F[Option[Percent]] = 
        { ask[Percent](s"report-$actType-operating-margin") emptyUnless
          ask[Boolean](s"report-$actType-loss").map { x => !x }} when
        ask[Boolean](s"report-$actType-alternative-charge")
      
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
        interact[GroupCompany, Money](s"amount-for-company-$i", co).map{(co, _)}
      }.sequence.map{_.toMap}
    }

    for {
      groupCos <- ask[List[GroupCompany]]("manage-companies", validation = Rule.minLength(1))
      activities <- ask[Set[Activity]]("select-activities")

      dstReturn <- (
        askAlternativeCharge(activities), 
        ask[Money]("relief-deducted") emptyUnless ask[Boolean]("report-cross-border-transaction-relief"),
        askAmountForCompanies(groupCos),
        ask[Money]("allowance-amount"),
        ask[Money]("total-liability"),
        ask[RepaymentDetails]("repayment") when ask[Boolean]("repayment-needed")
      ).mapN(Return.apply)
      _ <- tell("check-your-answers", CYA(dstReturn))
      _ <- tell("confirmation", Confirmation(dstReturn))
    } yield dstReturn
  }

}
