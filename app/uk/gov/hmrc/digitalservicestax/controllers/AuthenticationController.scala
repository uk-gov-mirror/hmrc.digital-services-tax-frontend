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

package uk.gov.hmrc.digitalservicestax.controllers

import javax.inject.Inject
import ltbs.uniform.UniformMessages
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class AuthenticationController @Inject()(mcc: MessagesControllerComponents)
  (implicit appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def signIn: Action[AnyContent] = Action { implicit request =>
    Redirect(appConfig.ggLoginUrl, Map("continue" -> Seq(appConfig.dstIndexPage), "origin" -> Seq(appConfig.appName)))
  }

  def signOut: Action[AnyContent] = Action { implicit request =>
    Redirect(appConfig.signOutDstUrl).withNewSession
  }

  def timeIn(referrer: String): Action[AnyContent] = Action { implicit request =>
    Redirect(appConfig.ggLoginUrl, Map("continue" -> Seq(referrer), "origin" -> Seq(appConfig.appName)))
  }

  def timeOut: Action[AnyContent] = Action { implicit request =>
    lazy val interpreter = DSTInterpreter(appConfig, this, messagesApi)
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)

    Ok(uk.gov.hmrc.digitalservicestax.views.html.end.time_out()).withNewSession
  }
}

