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
import connectors.DSTConnector
import data._
import akka.http.scaladsl.model.headers.LinkParams.title
import javax.inject.{Inject, Singleton}
import ltbs.uniform.interpreters.playframework._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Action, ControllerHelpers}

import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import actions.AuthorisedAction
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class JourneyController @Inject()(
  authorisedAction: AuthorisedAction,
  val authConnector: AuthConnector,
  val connector: DSTConnector
)(
  implicit val config: AppConfig,
  ec: ExecutionContext,
  implicit val messagesApi: MessagesApi
) extends ControllerHelpers
  with FrontendHeaderCarrierProvider
  with I18nSupport
  with AuthorisedFunctions {

  def getMsg(implicit r: play.api.mvc.Request[AnyContent]) =
    messagesApi.preferred(r).convertMessagesTwirlHtml(escapeHtml = false)

  def index: Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg = getMsg
    val backend = connector.uncached

    backend.lookupRegistration().flatMap {
      case None =>
        Future.successful(
          Redirect(routes.RegistrationController.registerAction(" "))
        )
      case Some(reg) if reg.registrationNumber.isDefined =>
        import cats.implicits._
        backend.lookupOutstandingReturns().map { periods => 
          Ok(views.html.main_template(
            title =
              s"${msg("common.title.short")} - ${msg("common.title")}",
              mainClass = Some("full-width")
          )(views.html.landing(reg, periods.toList.sortBy(_.start))))
        }
      case Some(reg) =>
        Future.successful(
          Ok(views.html.main_template(
            title =
              s"${msg("common.title.short")} - ${msg("common.title")}"
          )(views.html.end.pending()(msg)))
        )
    }
  }

  def accessibilityStatement: Action[AnyContent] = Action { implicit request =>
    implicit val msg = getMsg

    Ok(views.html.accessibility_statement(s"${msg("accessibility-statement.title")}"))
  }

}
