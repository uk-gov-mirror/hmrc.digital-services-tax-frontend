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
import uk.gov.hmrc.http.HeaderCarrier

trait DSTService[F[_]] {

  def lookupCompany()(implicit hc: HeaderCarrier): F[Option[Company]]
  def lookupCompany(utr: UTR, postcode: Postcode)(implicit hc: HeaderCarrier): F[Option[Company]]
  def submitRegistration(reg: Registration)(implicit hc: HeaderCarrier): F[Unit]
  def submitReturn(period: Period, ret: Return)(implicit hc: HeaderCarrier): F[Unit]
  def lookupRegistration()(implicit hc: HeaderCarrier): F[Option[Registration]]
  def lookupOutstandingReturns()(implicit hc: HeaderCarrier): F[Set[Period]]

  def natTransform[G[_]](transform: F ~> G): DSTService[G] = {
    val old = this
    new DSTService[G] {

      def lookupCompany()(implicit hc: HeaderCarrier): G[Option[Company]] =
        transform(old.lookupCompany())
      def lookupCompany(utr: UTR, postcode: Postcode)(implicit hc: HeaderCarrier): G[Option[Company]] =
        transform(old.lookupCompany(utr, postcode))        
      def submitRegistration(reg: Registration)(implicit hc: HeaderCarrier): G[Unit] =
        transform(old.submitRegistration(reg))        
      def submitReturn(period: Period, ret: Return)(implicit hc: HeaderCarrier): G[Unit] =
        transform(old.submitReturn(period, ret))
      def lookupRegistration()(implicit hc: HeaderCarrier): G[Option[Registration]] =
        transform(old.lookupRegistration())        
      def lookupOutstandingReturns()(implicit hc: HeaderCarrier): G[Set[Period]] =
        transform(old.lookupOutstandingReturns())                
    }
  }
}
