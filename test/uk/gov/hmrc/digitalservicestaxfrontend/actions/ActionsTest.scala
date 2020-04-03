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

package uk.gov.hmrc.digitalservicestax.actions

import java.net.URI

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.outworkers.util.domain.ShortString
import com.outworkers.util.samplers._
import ltbs.uniform.UniformMessages
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import play.api.libs.json.{JsArray, JsObject, JsResultException, JsString, Json}
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, Enrolments, PlayAuthConnector}
import uk.gov.hmrc.digitalservicestax.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import uk.gov.hmrc.digitalservicestax.data.InternalId
import uk.gov.hmrc.digitalservicestax.TestInstances._
import uk.gov.hmrc.digitalservicestax.util.FakeApplicationSpec
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ActionsTest extends FakeApplicationSpec with BeforeAndAfterEach with ScalaCheckDrivenPropertyChecks {

  lazy val authConnector: PlayAuthConnector = new DefaultAuthConnector(httpClient, servicesConfig)

  // Run wiremock server on local machine with specified port.
  val inet = new URI(authConnector.serviceUrl)
  val wireMockServer = new WireMockServer(wireMockConfig().port(inet.getPort))

  WireMock.configureFor(inet.getHost, inet.getPort)

  override def beforeEach {
    wireMockServer.start()
  }

  override def afterEach {
    wireMockServer.stop()
  }

  lazy val action = new AuthorisedAction(mcc, authConnector)(appConfig, global, messagesApi)

  "it should test an authorised action against auth connector retrievals" in {
    forAll { (enrolments: Enrolments, id: InternalId, role: CredentialRole, ag: AffinityGroup) =>
      val jsonResponse = JsObject(Seq(
        Retrievals.allEnrolments.propertyNames.head -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
        Retrievals.internalId.propertyNames.head -> JsString(id),
        Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
        Retrievals.affinityGroup.propertyNames.head -> Json.toJson(ag)
      ))

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(aResponse().withStatus(200).withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(FakeRequest(), {
        _: AuthorisedRequest[_] => Future.successful(Results.Ok)
      })

      whenReady(req) { res =>
        res.header.status mustEqual Status.OK
      }
    }
  }

  "it should use a standard body parser for actions" in {
    action.parser.getClass mustBe mcc.parsers.anyContent.getClass
  }

  "it should throw an exception in AuthorisedAction if internalId is missing from the retrieval" in {

    forAll { (enrolments: Enrolments, role: CredentialRole, ag: AffinityGroup) =>
      val jsonResponse = JsObject(Seq(
        Retrievals.allEnrolments.propertyNames.head -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
        Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
        Retrievals.affinityGroup.propertyNames.head -> Json.toJson(ag)
      ))

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(FakeRequest(), {
        _: AuthorisedRequest[_] => Future.successful(Results.Ok)
      })

      whenReady(req.failed) { res =>
        res.getMessage mustEqual "No internal ID for user"
        res mustBe an [RuntimeException]
      }
    }
  }

  "it should throw an exception in AuthorisedAction if the affinity group is not support" in {
    forAll { (enrolments: Enrolments, id: InternalId, role: CredentialRole)  =>
      val jsonResponse = JsObject(Seq(
        Retrievals.allEnrolments.propertyNames.head -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
        Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
        Retrievals.internalId.propertyNames.head -> JsString(id),
        Retrievals.affinityGroup.propertyNames.head -> JsString(gen[ShortString].value)
      ))

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(aResponse().withStatus(200).withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(FakeRequest(), {
        _: AuthorisedRequest[_] => Future.successful(Results.Ok)
      })

      whenReady(req.failed) { res =>
        res mustBe an [JsResultException]
      }
    }
  }

  "it should throw an exception in AuthorisedAction if an invalid internal ID is returned from the retrieval" in {
    forAll { (enrolments: Enrolments, role: CredentialRole, ag: AffinityGroup) =>
      val jsonResponse = JsObject(Seq(
        Retrievals.internalId.propertyNames.head -> JsString("___bla"),
        Retrievals.allEnrolments.propertyNames.head -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
        Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
        Retrievals.affinityGroup.propertyNames.head -> Json.toJson(ag)
      ))

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(aResponse().withStatus(200).withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(FakeRequest(), {
        _: AuthorisedRequest[_] => Future.successful(Results.Ok)
      })

      whenReady(req.failed) { res =>
        res.getMessage mustEqual "Invalid internal ID"
        res mustBe an [IllegalStateException]
      }
    }
  }


  "it should produce an error template using the error handler function" in {
    val handler = app.injector.instanceOf[ErrorHandler]
    val page = gen[ShortString].value
    val heading = gen[ShortString].value
    val message = gen[ShortString].value

    implicit val req = FakeRequest()

    val msg = handler.standardErrorTemplate(
      page,
      heading,
      message
    )
    import ltbs.uniform.interpreters.playframework._

    implicit val messages: UniformMessages[Html] = {
      messagesApi.preferred(req).convertMessagesTwirlHtml()
    }

    implicit val conf: AppConfig = appConfig

    import uk.gov.hmrc.digitalservicestax.views

    msg mustEqual views.html.error_template(page, heading, message)
  }
}
