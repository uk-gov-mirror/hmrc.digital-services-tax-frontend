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

import data._
import connectors._

import akka.http.scaladsl.model.headers.LinkParams.title
import cats.implicits._
import config.AppConfig
import javax.inject.{Inject, Singleton}

import ltbs.uniform.UniformMessages
import ltbs.uniform.interpreters.playframework.RichPlayMessages
import play.api.i18n.{I18nSupport, MessagesApi, Messages}
import play.api.mvc._
import play.twirl.api.Html
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.AuthorisedAction
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class JourneyController @Inject()(
  authorisedAction: AuthorisedAction,
  val http: HttpClient,
  val authConnector: AuthConnector,
  servicesConfig: ServicesConfig,
  val mongo: play.modules.reactivemongo.ReactiveMongoApi
)(
  implicit ec: ExecutionContext,
  val messagesApi: MessagesApi,
  val appConfig: AppConfig
) extends ControllerHelpers
  with FrontendHeaderCarrierProvider
  with I18nSupport
  with AuthorisedFunctions {

  def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)

  def index: Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] =
      implicitly[Messages].convertMessagesTwirlHtml(false)

    backend.lookupRegistration().flatMap {
      case None =>
        Future.successful(
          Redirect(routes.RegistrationController.registerAction(" "))
        )
      case Some(reg) if reg.registrationNumber.isDefined =>
        backend.lookupOutstandingReturns().map { periods => 
          Ok(views.html.main_template(
            title =
              s"${msg("landing.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}",
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

  def financialDetails: Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] =
      implicitly[Messages].convertMessagesTwirlHtml(false)

    backend.lookupRegistration().flatMap {
      case Some(_) =>
        backend.lookupFinancialDetails().map{ lineItems =>
          Ok(views.html.main_template(
            title =
              s"${msg("landing.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}",
            mainClass = Some("full-width")
          )(views.html.financial_details(lineItems, msg)))
        }
      case None =>
        Future.successful(
          Redirect(routes.RegistrationController.registerAction(" "))
        )        
    }
  }

  def accessibilityStatement: Action[AnyContent] = Action { implicit request =>
    implicit val msg: UniformMessages[Html] =
      implicitly[Messages].convertMessagesTwirlHtml(false)
    Ok(views.html.accessibility_statement(s"${msg("accessibility-statement.title")}"))
  }

}
