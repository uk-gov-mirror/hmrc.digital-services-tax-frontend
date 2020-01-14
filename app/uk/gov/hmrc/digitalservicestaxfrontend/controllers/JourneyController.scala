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

package uk.gov.hmrc.digitalservicestaxfrontend.controllers

import akka.http.scaladsl.model.headers.LinkParams.title
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.digitalservicestaxfrontend.aa_data.JsonConversion._
import ltbs.uniform.{UniformMessages, ErrorTree}
import ltbs.uniform.interpreters.playframework._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.digitalservicestaxfrontend.aa_data.JourneyState
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, FrontendHeaderCarrierProvider}
import uk.gov.hmrc.digitalservicestaxfrontend.config.AppConfig
import uk.gov.hmrc.digitalservicestaxfrontend.repo.JourneyStateStore
import uk.gov.hmrc.digitalservicestaxfrontend.views
import ltbs.uniform.common.web.{WebMonad, FutureAdapter}
import uk.gov.hmrc.digitalservicestaxfrontend.data._
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

  lazy val interpreter = DSTInterpreter(appConfig, this, messagesApi)
  import interpreter._

  implicit val persistence: PersistenceEngine[Request[AnyContent]] =
    UnsafePersistence()

  implicit def crapWebTell[A] = new interpreter.WebTell[A] {
    def render(in: A, key: String, messages: UniformMessages[Html]): Html =
      HtmlFormat.escape(in.toString)
  }

  def registerAction(targetId: String) = Action.async { implicit request: Request[AnyContent] =>
    import interpreter._

    val playProgram = registrationJourney[interpreter.WM](
      create[RegTellTypes, RegAskTypes](interpreter.messages(request)),
      hod
    )

    playProgram.run(targetId, purgeStateUponCompletion = true) {
      i: Registration => Future(Ok(s"$i"))
    }
  }

  def returnAction(targetId: String) = Action.async { implicit request: Request[AnyContent] =>
    import interpreter._

    val playProgram = returnJourney[interpreter.WM](
      create[ReturnTellTypes, ReturnAskTypes](interpreter.messages(request))
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
