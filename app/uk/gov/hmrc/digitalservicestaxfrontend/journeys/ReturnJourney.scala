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

package uk.gov.hmrc.digitalservicestaxfrontend
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
  type ReturnAskTypes = RepaymentDetails :: Set[Activity] :: Long :: Int :: Boolean :: List[GroupCompany] :: NilTypes

  def returnJourney[F[_] : Monad](
    interpreter: Language[F, ReturnTellTypes, ReturnAskTypes]
  ): F[Return] = {
    import interpreter._

    def askAlternativeCharge(applicableActivities: Set[Activity]): F[Map[Activity, Int]] = {

      def askActivity(actType: Activity): F[Option[Int]] = 
      { ask[Int](s"${actType}-margin") emptyUnless
        ask[Boolean](s"${actType}-loss").map{x => !x} } when
      ask[Boolean](s"${actType}-applying")
      
      val allEntries: List[F[(Activity, Option[Int])]] =
        applicableActivities.toList.map{ actType =>
          askActivity(actType).map{ (actType, _)}
        }
      allEntries.sequence.map{_.collect{
        case (a, Some(x)) => (a,x)
      }.toMap}
    } emptyUnless ask[Boolean]("alternative-charge")


    def askAmountForCompanies(companies: List[GroupCompany]): F[Map[GroupCompany, Long]] = {
      companies.zipWithIndex.map{ case (co, i) => 
        interact[GroupCompany, Long](s"amount-for-company-$i", co).map{(co, _)}
      }.sequence.map{_.toMap}
    }

    for {
      groupCos <- ask[List[GroupCompany]]("group-companies")
      activities <- ask[Set[Activity]]("applicable-activities")

      dstReturn <- (
        askAlternativeCharge(activities), 
        ask[Long]("cross-border-relief-amount") emptyUnless ask[Boolean]("cross-border-relief"), 
        askAmountForCompanies(groupCos),
        ask[Long]("allowance-amount"),
        ask[Long]("total-liability"),
        ask[RepaymentDetails]("repayment") when ask[Boolean]("repayment-needed")
      ).mapN(Return.apply)
      _ <- tell("check-your-answers", CYA(dstReturn))
      _ <- tell("confirmation", Confirmation(dstReturn))
    } yield (dstReturn)
  }

}
