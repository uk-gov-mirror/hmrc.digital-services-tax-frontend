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

package uk.gov.hmrc.digitalservicestaxfrontend.actions

import cats.syntax.semigroup._
import javax.inject.Inject
import ltbs.uniform.UniformMessages
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.Results.{Continue, Forbidden, Ok, Redirect}
import play.api.mvc._
import play.api.http.Status._
import play.twirl.api.{Html, HtmlFormat}
import ltbs.uniform.UniformMessages
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, Verify}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentialRole, internalId}
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.controllers.{DSTInterpreter, JourneyController, routes}
import uk.gov.hmrc.digitalservicestax.data.{InternalId, Registration}
import uk.gov.hmrc.digitalservicestax.views
import uk.gov.hmrc.digitalservicestax.views.html.main_template
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}


class AuthorisedAction @Inject()(
  mcc: MessagesControllerComponents,
  val authConnector: AuthConnector
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext, val messagesApi: MessagesApi)
  extends ActionBuilder[AuthorisedRequest, AnyContent] with ActionRefiner[Request, AuthorisedRequest] with AuthorisedFunctions {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    import ltbs.uniform.interpreters.playframework.RichPlayMessages
    implicit val req: Request[A] = request
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    implicit val msg: UniformMessages[Html] = messagesApi.preferred(request).convertMessagesTwirlHtml(escapeHtml = false) |+|
      UniformMessages.bestGuess.map(HtmlFormat.escape)

    val retrieval =  allEnrolments and credentialRole and internalId and affinityGroup

    authorised(AuthProviders(GovernmentGateway, Verify) and Organisation and User).retrieve(retrieval) { case enrolments ~ role ~ id ~ affinity  =>

      val internalIdString = id.getOrElse(throw new RuntimeException("No internal ID for user"))
      val internalId = InternalId.of(internalIdString).getOrElse(
        throw new IllegalStateException("Invalid internal ID")
      )

      Future.successful(Right(AuthorisedRequest(internalId, enrolments, request)))

    } recover {
      case af: UnsupportedAffinityGroup =>
        Logger.warn(s"invalid account affinity type, with message ${af.msg}, for reason ${af.reason}",
          af)
        Left(Ok(
          main_template(
            title =
              s"${msg("common.title.short")} - ${msg("common.title")}"
          )(views.html.errors.incorrect_account_affinity()(msg))
        ))
      case ex: UnsupportedCredentialRole =>
        Logger.warn(
          s"unsupported credential role on account, with message ${ex.msg}, for reason ${ex.reason}",
          ex)
        Left(Ok(
          main_template(
            title =
              s"${msg("common.title.short")} - ${msg("common.title")}"
          )(views.html.errors.incorrect_account_cred_role()(msg))
        ))
      case _ : NoActiveSession =>
        Logger.info(s"Recover - no active session")
        Left(
          Redirect(routes.AuthenticationController.signIn())
        )
    }
  }

  override def parser = mcc.parsers.anyContent

}

case class AuthorisedRequest[A](
  internalId: InternalId,
  enrolments: Enrolments,
  request: Request[A]
) extends WrappedRequest(request)

