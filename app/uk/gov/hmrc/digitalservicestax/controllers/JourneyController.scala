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
import connectors._
import cats.implicits._
import data._
import frontend.Kickout
import repo.JourneyStateStore
import akka.http.scaladsl.model.headers.LinkParams.title
import javax.inject.{Inject, Singleton}
import ltbs.uniform.common.web.{FutureAdapter, GenericWebTell, WebMonad, ListingTell, ListingTellRow}
import ltbs.uniform.interpreters.playframework._
import ltbs.uniform.{ErrorTree, UniformMessages}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.{Html, HtmlFormat}, HtmlFormat.escape
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, FrontendHeaderCarrierProvider}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class JourneyController @Inject()(
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

  def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)

  def hod(implicit hc: HeaderCarrier): DSTService[WebMonad[*, Html]] = backend.natTransform[WebMonad[*, Html]]{
    import cats.~>
    new (Future ~> WebMonad[*, Html]) {
      def apply[A](in: Future[A]): WebMonad[A, Html] = FutureAdapter[Html]().alwaysRerun(in)
    }
  }

  val interpreter = DSTInterpreter(config, this, messagesApi)

  implicit def autoListingTell[A](implicit tell: GenericWebTell[A, Html]) = new ListingTell[Html, A] {
    def apply(rows: List[ListingTellRow[A]], messages: UniformMessages[Html]): Html =
      views.html.uniform.listing(rows.map {
        case ListingTellRow(value, editLink, deleteLink) => (tell.render(value, "a", messages), editLink, deleteLink)
      }, messages)
  }

  implicit val addressTell = new GenericWebTell[Address, Html] {

    override def render(in: Address, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<p>" +
          s"<span>" +
            s"${in.lines.mkString("</span></br><span>")}" +
          s"</span>" +
        "</p>"
      )
  }

  implicit val ukAddressTell = new GenericWebTell[UkAddress, Html] {
    override def render(in: UkAddress, key: String, messages: UniformMessages[Html]): Html =
      Html(s"<span class='govuk-body-m'></br>${in.line1}</br>${in.line2}</br>${in.line3}</br>${in.line4}</br>${in.postalCode}")
  }

  implicit val confirmRegTell = new GenericWebTell[Confirmation[Registration], Html] {
    override def render(in: Confirmation[Registration], key: String, messages: UniformMessages[Html]): Html = {
      val reg = in.value
      views.html.confirmation(key: String, reg.company.name: String, reg.contact.email: Email)(messages)
    }
  }

  implicit val cyaRegTell = new GenericWebTell[CYA[Registration], Html] {
    override def render(in: CYA[Registration], key: String, messages: UniformMessages[Html]): Html =
      views.html.check_your_answers(key, in.value)(messages)
  }

  implicit val kickoutTell = new GenericWebTell[Kickout, Html] {
    override def render(in: Kickout, key: String, messages: UniformMessages[Html]): Html =
      views.html.kickout(key)(messages)
  }

  implicit val companyTell = new GenericWebTell[Company, Html] {
    override def render(in: Company, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<p>" +
          s"<span>${in.name.toString}</span></br>" +
          s"<span>" +
          s"${in.address.lines.mkString("</span></br><span>")}" +
          s"</span>" +
          "</p>"
      )
  }
  implicit val booleanTell = new GenericWebTell[Boolean, Html] {
    override def render(in: Boolean, key: String, messages: UniformMessages[Html]): Html =
      Html(in.toString)
  }

  implicit val confirmRetTell = new GenericWebTell[Confirmation[Return], Html] {
    override def render(in: Confirmation[Return], key: String, messages: UniformMessages[Html]): Html =
      Html(in.toString)
  }

  implicit val cyaRetTell = new GenericWebTell[CYA[Return], Html] {
    override def render(in: CYA[Return], key: String, messages: UniformMessages[Html]): Html =
      Html(in.toString)
  }

  implicit val groupCoTell = new GenericWebTell[GroupCompany, Html] {
    override def render(in: GroupCompany, key: String, messages: UniformMessages[Html]): Html =
      Html(in.toString)
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
          hod
        )
        playProgram.run(targetId, purgeStateUponCompletion = true) {
          backend.submitRegistration(_).map { _ => Redirect(routes.JourneyController.index) }
      }

      case Some(reg) => index(request)
    }
  }

  def returnAction(periodKeyString: String, targetId: String): Action[AnyContent] = authorisedAction.async {
    implicit request: AuthorisedRequest[AnyContent] =>
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
              playProgram.run(targetId, purgeStateUponCompletion = true) {
                backend.submitReturn(period, _).map{ _ => Redirect(routes.JourneyController.index)}
              }
          }
        }
    } 
  }

  def index: Action[AnyContent] = Action.async { implicit request =>
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)

    backend.lookupRegistration().flatMap {
      case None =>
        Future.successful(
          Redirect(routes.JourneyController.registerAction(" "))
        )
      case Some(reg) if reg.registrationNumber.isDefined =>
        backend.lookupOutstandingReturns().map { periods => 
          Ok(views.html.main_template(
            title =
              s"${msg("common.title.short")} - ${msg("common.title")}"
          )(views.html.landing(reg, periods.toList.sortBy(_.start))))
        }
      case Some(reg) =>
        Future.successful(        
          Ok(views.html.main_template(
            title =
              s"${msg("common.title.short")} - ${msg("common.title")}"
          )(views.html.confirmation("registration-sent", reg.company.name, reg.contact.email)(msg)))
        )
    }
  }

}
