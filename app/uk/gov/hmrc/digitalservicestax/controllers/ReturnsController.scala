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
package controllers

import config.AppConfig
import connectors.{DSTConnector, DSTService, MongoPersistence}
import data._
import akka.http.scaladsl.model.headers.LinkParams.title
import javax.inject.{Inject, Singleton}
import ltbs.uniform.common.web.{FutureAdapter, JourneyConfig, WebMonad}
import ltbs.uniform.interpreters.playframework._
import ltbs.uniform.{ErrorTree, UniformMessages}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Action, ControllerHelpers, MessagesControllerComponents}
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class ReturnsController @Inject()(
  authorisedAction: AuthorisedAction,
  mcc: MessagesControllerComponents,
  val http: HttpClient,
  val authConnector: AuthConnector,
  servicesConfig: ServicesConfig,
  val mongo: play.modules.reactivemongo.ReactiveMongoApi  
)(
  implicit val config: AppConfig,
  ec: ExecutionContext,
  implicit val messagesApi: MessagesApi
) extends ControllerHelpers
  with FrontendHeaderCarrierProvider
  with I18nSupport
  with AuthorisedFunctions {

  private def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)
  private val interpreter = DSTInterpreter(config, this, messagesApi)

  def returnAction(periodKeyString: String, targetId: String): Action[AnyContent] = authorisedAction.async {
    implicit request: AuthorisedRequest[AnyContent] =>
      implicit val msg: UniformMessages[Html] = interpreter.messages(request)
    import interpreter._
    import journeys.ReturnJourney._

    val periodKey = Period.Key(periodKeyString)

    implicit val persistence: PersistenceEngine[AuthorisedRequest[AnyContent]] =
      MongoPersistence[AuthorisedRequest[AnyContent]](
        mongo,
        collectionName = "uf-returns",
        config.mongoJourneyStoreExpireAfter
      )(_.internalId)

    backend.lookupRegistration().flatMap{
      case None      => Future.successful(NotFound)
      case Some(reg) =>
        backend.lookupOutstandingReturns().flatMap { periods => 
          periods.find(_.key == periodKey) match {
            case None => Future.successful(NotFound)
            case Some(period) =>
              val playProgram = returnJourney[WM](
                create[ReturnTellTypes, ReturnAskTypes](messages(request))
              )
              playProgram.run(targetId, purgeStateUponCompletion = true, config = JourneyConfig(askFirstListItem = true)) { x =>
                backend.submitReturn(period, x).map{ _ =>

                  Redirect(routes.JourneyController.index)
                  Ok(views.html.main_template(
                    title =
                      s"${msg("common.title.short")} - ${msg("common.title")}"
                  )(views.html.end.confirmation_return("confirmation")(msg)))
                }
              }
          }
        }
    } 
  }

}
