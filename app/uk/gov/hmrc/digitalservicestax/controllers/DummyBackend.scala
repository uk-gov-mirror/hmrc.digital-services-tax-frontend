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

package uk.gov.hmrc.digitalservicestax.controllers

import uk.gov.hmrc.digitalservicestax.data._
import scala.concurrent._

case class DummyBackend(implicit ec: ExecutionContext) extends BackendService[Future] {

  def matchedCompany(): Future[Option[Company]] =
    Future.successful(None)          

  def lookup(utr: UTR, postcode: Postcode): Future[Option[Company]] =
    Future.successful(Some(Company(NonEmptyString("Dotcom Bubble ltd."), UkAddress(NonEmptyString("51 MySpace Apartments"),"1 Bebo Street","Aolton","Askjeeveshire", Postcode("AB12 3CD")))))

}
