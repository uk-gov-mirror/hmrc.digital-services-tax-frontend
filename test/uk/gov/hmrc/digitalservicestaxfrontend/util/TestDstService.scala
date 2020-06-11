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

package uk.gov.hmrc.digitalservicestaxfrontend.util

import cats.Id
import cats.implicits._
import uk.gov.hmrc.digitalservicestax.connectors.DSTService
import uk.gov.hmrc.digitalservicestax.data.SampleData.{sampleCompanyRegWrapper, sampleReg, utrLookupCompanyRegWrapper}
import uk.gov.hmrc.digitalservicestax.data._

trait TestDstService extends DSTService[Id] {
  def lookupCompany(): Option[CompanyRegWrapper] = sampleCompanyRegWrapper.some
  def lookupCompany(utr: UTR, postcode: Postcode): Option[CompanyRegWrapper] = utrLookupCompanyRegWrapper.some
  def submitRegistration(reg: Registration): Unit = (())
  def submitReturn(period: Period, ret: Return): Unit = (())
  def lookupRegistration(): Option[Registration] = sampleReg.some
  def lookupOutstandingReturns(): Set[Period] = Set.empty
  def get = this.transform(ToLogic)
  def lookupFinancialDetails: List[FinancialTransaction] = Nil
}
