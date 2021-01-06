/*
 * Copyright 2021 HM Revenue & Customs
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
import config.AppConfig
import connectors.{DSTConnector, MongoPersistence}

import javax.inject.Inject
import ltbs.uniform.UniformMessages
import ltbs.uniform.common.web.GenericWebTell
import ltbs.uniform.interpreters.playframework.{PersistenceEngine, tellTwirlUnit}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerHelpers}
import play.modules.reactivemongo.ReactiveMongoApi
import play.twirl.api.Html
import scala.concurrent.{Future, ExecutionContext}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.HttpClient

class RegistrationController @Inject()(
  authorisedAction: AuthorisedAction,
  http: HttpClient,
  servicesConfig: ServicesConfig,
  mongo: ReactiveMongoApi,
  val authConnector: AuthConnector,
  val messagesApi: MessagesApi  
)(implicit
  config: AppConfig,
  ec: ExecutionContext
) extends ControllerHelpers
    with I18nSupport
    with AuthorisedFunctions
    with FrontendHeaderCarrierProvider
{

  val interpreter = DSTInterpreter(config, this, messagesApi)
  private def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)

  private def hod(id: InternalId)(implicit hc: HeaderCarrier) =
    connectors.CachedDstService(backend)(id)

  private implicit val cyaRegTell = new GenericWebTell[CYA[Registration], Html] {
    override def render(in: CYA[Registration], key: String, messages: UniformMessages[Html]): Html =
      views.html.cya.check_your_registration_answers(s"$key.reg", in.value)(messages)
  }

  private implicit val confirmRegTell = new GenericWebTell[Confirmation[Registration], Html] {
    override def render(in: Confirmation[Registration], key: String, messages: UniformMessages[Html]): Html = {
      val reg = in.value
      views.html.end.confirmation(key: String, reg.companyReg.company.name: String, reg.contact.email: Email)(messages)
    }
  }

  def registerAction(targetId: String): Action[AnyContent] = authorisedAction.async { implicit request: AuthorisedRequest[AnyContent] =>
    import interpreter._
    import journeys.RegJourney._

    implicit val persistence: PersistenceEngine[AuthorisedRequest[AnyContent]] =
      MongoPersistence[AuthorisedRequest[AnyContent]](
        mongo,
        collectionName = "uf-registrations",
        config.mongoJourneyStoreExpireAfter        
      )(_.internalId)

    backend.lookupRegistration().flatMap {
      case None =>
        val playProgram = registrationJourney[WM](
          create[RegTellTypes, RegAskTypes](messages(request)),
          hod(request.internalId)
        )
        playProgram.run(targetId, purgeStateUponCompletion = true) {
          backend.submitRegistration(_).map { _ => Redirect(routes.RegistrationController.registrationComplete) }
      }

      case Some(_) =>
        Future.successful(Redirect(routes.JourneyController.index))
    }
  }

  def registrationComplete: Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)

    backend.lookupRegistration().flatMap {
      case None =>
        Future.successful(
          Redirect(routes.RegistrationController.registerAction(" "))
        )
      case Some(reg) =>
        Future.successful(
          Ok(views.html.main_template(
            title =
              s"${msg("registration-sent.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}"
          )(views.html.end.confirmation("registration-sent", reg.companyReg.company.name, reg.contact.email)(msg)))
        )
    }
  }  
}
