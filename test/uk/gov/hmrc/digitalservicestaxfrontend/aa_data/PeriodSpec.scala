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

package uk.gov.hmrc.digitalservicestax.data

import org.scalatest.{Matchers, FlatSpec}
import java.time.LocalDate

class PeriodSpec extends FlatSpec with Matchers {

  import SampleData._

  "Period's" should "be calculated from the Registration" in {
    sampleReg.period(2020) should be (Some(Period(
      LocalDate.of(2020, 7, 1),
      LocalDate.of(2021, 4, 4)
    )))
  }
}
