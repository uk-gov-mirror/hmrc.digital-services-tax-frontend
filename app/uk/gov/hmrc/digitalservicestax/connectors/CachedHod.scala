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
package connectors

import controllers.SimpleCaching
import data._

import ltbs.uniform.common.web.{WebMonad, FutureAdapter}
import scala.concurrent.{ExecutionContext, Future}
import play.twirl.api.Html

case class CachedDstService(
  backend: DSTService[Future],
  hodCache: SimpleCaching[(InternalId, String)] = SimpleCaching[(InternalId, String)]()
)(
  internalId: InternalId
)(implicit ec: ExecutionContext) extends DSTService[WebMonad[*, Html]] {
    val fa = FutureAdapter[Html]()
    import fa._

    def lookupCompany(utr: UTR, postcode: Postcode): WebMonad[Option[CompanyRegWrapper],Html] =
      alwaysRerun(hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company-args"), utr, postcode)(
        backend.lookupCompany(utr, postcode)
      ))

    def lookupCompany(): WebMonad[Option[CompanyRegWrapper],Html] =
      alwaysRerun(hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company"))(
        backend.lookupCompany()
      ))

    def lookupOutstandingReturns(): WebMonad[Set[Period],Html] = 
      alwaysRerun(hodCache[Set[Period]]((internalId, "lookup-outstanding-returns"))(
        backend.lookupOutstandingReturns()
      ))

    def lookupRegistration(): WebMonad[Option[Registration],Html] = 
      alwaysRerun(hodCache[Option[Registration]]((internalId, "lookup-reg"))(
        backend.lookupRegistration()
      ))

    def submitRegistration(reg: Registration): WebMonad[Unit,Html] =
      alwaysRerun(backend.submitRegistration(reg))

    def submitReturn(period: Period,ret: Return): WebMonad[Unit,Html] = 
      alwaysRerun(backend.submitReturn(period, ret))

    def lookupFinancialDetails(): WebMonad[List[FinancialTransaction], Html] =
      alwaysRerun(hodCache[List[FinancialTransaction]]((internalId, "lookup-financial-details"))(
        backend.lookupFinancialDetails()
      ))

  }
