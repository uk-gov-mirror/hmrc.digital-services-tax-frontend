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

package uk.gov.hmrc.digitalservicestax.frontend

import play.api.libs.json._


object Country {

  sealed abstract case class Country private(name: String, code: String)
  object Country {
    def unQuote(quotedString: String): String =
      quotedString.slice(1,quotedString.length-1)

    def apply(name: String, code: String): Country = {
      new Country(unQuote(name), unQuote(code).replace("country:","")){}
    }
  }

  val countriesJson: JsValue = {
    val stream = getClass
    .getResourceAsStream(
    "/public/location-autocomplete-canonical-list.json"
    )
    val raw = scala.io.Source.fromInputStream(stream).getLines.mkString
    val json = Json.parse(raw)
    stream.close
    json
  }

  val countries: List[Country] =
    countriesJson
      .as[JsArray].value
      .filter(x => x(1).toString().matches("^\"country:.{2}\"$"))
      .map(y => Country(y(0).toString,y(1).toString)).toList


  def name(code: String) =
    countries
      .find(x => x.code == code)
      .map(_.name)
      .getOrElse("unknown")


}
