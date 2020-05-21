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

import ltbs.uniform.interpreters.logictable._
import uk.gov.hmrc.digitalservicestax.connectors.DSTService
import uk.gov.hmrc.digitalservicestax.data.Registration
import uk.gov.hmrc.digitalservicestax.journeys.RegJourney.{RegAskTypes, RegTellTypes}
import uk.gov.hmrc.digitalservicestaxfrontend.util.TestDstService

package object journeys {

  val testService: DSTService[Logic] = new TestDstService {}.get

  def instances[A](value: A*): SampleData[A] = new SampleData[A] {
    def apply(key: String): List[A] = value.toList
  }

  def instancesF[A](value: String => List[A]): SampleData[A] = new SampleData[A] {
    def apply(key: String): List[A] = value(key)
  }

  type TestRegInterpreter = LogicTableInterpreter[RegTellTypes, RegAskTypes]

  implicit class RichRegJourney (in: List[(List[_], Either[_, Registration])]) {
    def asReg(debug: Boolean = false): Registration = {
      if (debug) {
        in.foreach { case (messages, outcome) =>
          println(messages.mkString("\n"))
          println(s"   => $outcome")
        }
      }
      in.head._2.right.get
    }
  }

}
