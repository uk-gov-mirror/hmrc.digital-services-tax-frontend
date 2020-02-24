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

import akka.http.scaladsl.model.headers.LinkParams.title
import javax.inject.{Inject, Singleton}
import ltbs.uniform.{ErrorTree, UniformMessages}
import ltbs.uniform.interpreters.playframework._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, FrontendHeaderCarrierProvider}
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.repo.JourneyStateStore
import uk.gov.hmrc.digitalservicestax.views
import ltbs.uniform.common.web.{FutureAdapter, GenericWebTell, WebMonad}
import play.twirl.api.HtmlFormat.escape
import uk.gov.hmrc.digitalservicestax.data._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyController @Inject()(
  mcc: MessagesControllerComponents
)(
  implicit val appConfig: AppConfig,
  ec: ExecutionContext,
  implicit val messagesApi: MessagesApi
)extends ControllerHelpers
  with FrontendHeaderCarrierProvider
  with I18nSupport {

  val hod = DummyBackend().natTransform[WebMonad[?, Html]]{
    import cats.~>
    new (Future ~> WebMonad[?, Html]) {
      def apply[A](in: Future[A]): WebMonad[A, Html] = FutureAdapter[Html].alwaysRerun(in)
    }
  }

  val interpreter = DSTInterpreter(appConfig, this, messagesApi)
  import interpreter._

  implicit val persistence: PersistenceEngine[Request[AnyContent]] =
    UnsafePersistence()

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
      Html(s"Boogaloo")
  }

  implicit val confirmRetTell = new GenericWebTell[Confirmation[Return], Html] {
    override def render(in: Confirmation[Return], key: String, messages: UniformMessages[Html]): Html =
      Html(s"Boogaloo")
  }

  implicit val cyaRetTell = new GenericWebTell[CYA[Return], Html] {
    override def render(in: CYA[Return], key: String, messages: UniformMessages[Html]): Html =
      Html(s"Boogaloo")
  }

  implicit val groupCoTell = new GenericWebTell[GroupCompany, Html] {
    override def render(in: GroupCompany, key: String, messages: UniformMessages[Html]): Html =
      Html(s"Boogaloo")
  }

  def registerAction(targetId: String) = Action.async { implicit request: Request[AnyContent] =>
    import interpreter._
    import journeys.RegJourney._

    val playProgram = registrationJourney[WM](
      create[RegTellTypes, RegAskTypes](messages(request)),
      hod
    )(UTR("1234567890"))

    playProgram.run(targetId, purgeStateUponCompletion = true) {
      i: Registration => Future(Ok(s"$i"))
    }
  }

  def returnAction(targetId: String) = Action.async { implicit request: Request[AnyContent] =>
    import interpreter._
    import journeys.ReturnJourney._

    val playProgram = returnJourney[WM](
      create[ReturnTellTypes, ReturnAskTypes](messages(request))
    )

    playProgram.run(targetId, purgeStateUponCompletion = true) {
      i: Return => Future(Ok(s"$i"))
    }
  }

  def index: Action[AnyContent] = Action { implicit request =>
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)

      Ok(views.html.main_template(
        title =
          s"${msg("common.title.short")} - ${msg("common.title")}"
      )(views.html.hello_world()))

  }

}
