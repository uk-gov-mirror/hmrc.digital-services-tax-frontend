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
import ltbs.uniform.common.web.{FutureAdapter, GenericWebTell, JourneyConfig, ListingTell, ListingTellRow, WebMonad}
import ltbs.uniform.interpreters.playframework._
import ltbs.uniform.{ErrorTree, UniformMessages}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.{Html, HtmlFormat}
import HtmlFormat.escape

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, FrontendHeaderCarrierProvider}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier
import play.twirl.api.HtmlFormat

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

  val hodCache = SimpleCaching[(InternalId, String)]()

  def hod(internalId: InternalId)(implicit hc: HeaderCarrier) = new DSTService[WebMonad[*, Html]] {
    val fa = FutureAdapter[Html]()
    import fa._
    import concurrent.duration._
    import interpreter._

    def lookupCompany(utr: UTR, postcode: Postcode): WebMonad[Option[CompanyRegWrapper],Html] =
      alwaysRerun(hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company-args"), utr, postcode)(
        backend.lookupCompany(utr, postcode)
      ))

    def lookupCompany(): WebMonad[Option[CompanyRegWrapper],Html] =
      alwaysRerun(hodCache[Option[CompanyRegWrapper]]((internalId, "lookup-company"))(
        backend.lookupCompany()
      ))

    def lookupOutstandingReturns(): WebMonad[Set[Period],Html] = 
      alwaysRerun(hodCache[Set[Period]]((internalId, "lookup-outstanding-returns"))(
        backend.lookupOutstandingReturns()
      ))

    def lookupRegistration(): WebMonad[Option[Registration],Html] = 
      alwaysRerun(hodCache[Option[Registration]]((internalId, "lookup-reg"))(
        backend.lookupRegistration()
      ))

    def submitRegistration(reg: Registration): WebMonad[Unit,Html] =
      alwaysRerun(backend.submitRegistration(reg))

    def submitReturn(period: Period,ret: Return): WebMonad[Unit,Html] = 
      alwaysRerun(backend.submitReturn(period, ret))

    def lookupFinancialDetails(): WebMonad[List[FinancialTransaction],Html] = 
      alwaysRerun(backend.lookupFinancialDetails())
    
  }

  val interpreter = DSTInterpreter(config, this, messagesApi)

  implicit def autoGroupListingTell = new ListingTell[Html, GroupCompany] {
    def apply(rows: List[ListingTellRow[GroupCompany]], messages: UniformMessages[Html]): Html =
      views.html.uniform.listing(rows.map {
        case ListingTellRow(value, editLink, deleteLink) => (HtmlFormat.escape(value.name), editLink, deleteLink)
      }, messages)
  }

  implicit val addressTell = new GenericWebTell[Address, Html] {
    override def render(in: Address, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<p>${in.lines.map{x => s"<span class='govuk-body-m'>${x.escapeHtml}</span>"}.mkString("<br/>")}</p>"
      )
  }

  implicit val kickoutTell = new GenericWebTell[Kickout, Html] {
    override def render(in: Kickout, key: String, messages: UniformMessages[Html]): Html =
      views.html.end.kickout(key)(messages, config)
  }

  implicit val groupCoTell = new GenericWebTell[GroupCompany, Html] {
    override def render(in: GroupCompany, key: String, messages: UniformMessages[Html]): Html =
      Html("")
  }

  implicit val companyTell = new GenericWebTell[Company, Html] {
    override def render(in: Company, key: String, messages: UniformMessages[Html]): Html =
      Html(
        s"<p class='govuk-body-l' id='${key}-content'>" +
          s"${in.name.toString.escapeHtml}</br>" +
          s"<span class='govuk-body-m'>" +
          s"${in.address.lines.map{_.escapeHtml}.mkString("</br>")}" +
          s"</span>" +
          "</p>"
      )
  }

  implicit val booleanTell = new GenericWebTell[Boolean, Html] {
    override def render(in: Boolean, key: String, messages: UniformMessages[Html]): Html =
      Html(in.toString)
  }

  implicit val cyaRegTell = new GenericWebTell[CYA[Registration], Html] {
    override def render(in: CYA[Registration], key: String, messages: UniformMessages[Html]): Html =
      views.html.cya.check_your_registration_answers(s"$key.reg", in.value)(messages)
  }

  implicit val cyaRetTell = new GenericWebTell[CYA[Return], Html] {
    override def render(in: CYA[Return], key: String, messages: UniformMessages[Html]): Html =
      views.html.cya.check_your_return_answers(s"$key.ret", in.value)(messages)
  }

  implicit val confirmRegTell = new GenericWebTell[Confirmation[Registration], Html] {
    override def render(in: Confirmation[Registration], key: String, messages: UniformMessages[Html]): Html = {
      val reg = in.value
      views.html.end.confirmation(key: String, reg.companyReg.company.name: String, reg.contact.email: Email)(messages)
    }
  }

  implicit val confirmRetTell = new GenericWebTell[Confirmation[Return], Html] {
    override def render(in: Confirmation[Return], key: String, messages: UniformMessages[Html]): Html =
      views.html.end.confirmation_return(key: String)(messages)
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
          backend.submitRegistration(_).map { _ => Redirect(routes.JourneyController.registrationComplete) }
      }

      case Some(reg) => index(request)
    }
  }

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
                      s"${msg("confirmation.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}"
                  )(views.html.end.confirmation_return("confirmation")(msg)))
                }
              }
          }
        }
    } 
  }

  def registrationComplete: Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)

    backend.lookupRegistration().flatMap {
      case None =>
        Future.successful(
          Redirect(routes.JourneyController.registerAction(" "))
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

  def index: Action[AnyContent] = authorisedAction.async { implicit request =>
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

  def financialDetails: Action[AnyContent] = Action.async { implicit request =>
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)

    import java.time.LocalDate
    val f = backend.lookupFinancialDetails()

    f.map{ lineItems => 
      Ok(views.html.main_template(
        title =
          s"${msg("landing.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}",
        mainClass = Some("full-width")
      )(views.html.financial_details(lineItems, msg)))
    }


  }

  def accessibilityStatement: Action[AnyContent] = Action { implicit request =>
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)
    Ok(views.html.accessibility_statement(s"${msg("accessibility-statement.title")}"))
  }

}
