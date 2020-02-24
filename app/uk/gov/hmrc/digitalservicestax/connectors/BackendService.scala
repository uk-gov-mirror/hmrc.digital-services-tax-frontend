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

  def submitRegistration(reg: Registration): F[Unit]
  def submitReturn(ret: Return): F[Unit]
  def matchedCompany(): F[Option[Company]]
  def lookup(utr: UTR, postcode: Postcode): F[Option[Company]]

  def natTransform[G[_]](transform: F ~> G): BackendService[G] = {
    val old = this
    new BackendService[G] {
      def submitRegistration(reg: Registration): G[Unit] =
        transform(old.submitRegistration(reg))        
      def submitReturn(ret: Return): G[Unit] =
        transform(old.submitReturn(ret))                
      def matchedCompany(): G[Option[Company]] =
        transform(old.matchedCompany())
      def lookup(utr: UTR, postcode: Postcode): G[Option[Company]] =
        transform(old.lookup(utr, postcode))
    }
  }
}
