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

package uk.gov.hmrc.digitalservicestax.connectors

import uk.gov.hmrc.digitalservicestax.data._
import scala.language.higherKinds
import cats.~>

trait BackendService[F[_]] {

  def lookupCompany(): F[Option[Company]]
  def lookupCompany(utr: UTR, postcode: Postcode): F[Option[Company]]
  def submitRegistration(reg: Registration): F[Unit]
  def submitReturn(period: Period, ret: Return): F[Unit]
  def lookupRegistration(): F[Option[Registration]]
  def lookupOutstandingReturns(): F[Set[Period]]

  def natTransform[G[_]](transform: F ~> G): BackendService[G] = {
    val old = this
    new BackendService[G] {

      def lookupCompany(): G[Option[Company]] =
        transform(old.lookupCompany())
      def lookupCompany(utr: UTR, postcode: Postcode): G[Option[Company]] =
        transform(old.lookupCompany(utr, postcode))        
      def submitRegistration(reg: Registration): G[Unit] =
        transform(old.submitRegistration(reg))        
      def submitReturn(period: Period, ret: Return): G[Unit] =
        transform(old.submitReturn(period, ret))
      def lookupRegistration(): G[Option[Registration]] =
        transform(old.lookupRegistration())        
      def lookupOutstandingReturns(): G[Set[Period]] =
        transform(old.lookupOutstandingReturns())                
    }
  }
}
