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

import java.time.LocalDate

import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.digitalservicestax.frontend._
import TestInstances._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DateFormatTests extends FlatSpec with ScalaCheckDrivenPropertyChecks with Matchers {

  it should "create a formatter for the time right now" in {
    formattedTimeNow.nonEmpty shouldEqual true
  }

  it should "create a date string for a date pattern" in {
    val localDt = LocalDate.of(2020, 3, 1)
    formatDate(localDt) shouldEqual "1 March 2020"
  }

}
