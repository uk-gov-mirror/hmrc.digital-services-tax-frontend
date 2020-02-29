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

import javax.inject.Inject
import play.api.Logger
import play.api.mvc.Results.{Forbidden, Redirect}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.AuthProvider.{GovernmentGateway, Verify}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentialRole, internalId}
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.controllers.routes
import uk.gov.hmrc.digitalservicestax.data.Registration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}


class AuthorisedAction @Inject()(mcc: MessagesControllerComponents, val authConnector: AuthConnector, dstConnector: DSTConnector)
  (implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
  extends ActionBuilder[AuthorisedRequest, AnyContent] with ActionRefiner[Request, AuthorisedRequest] with AuthorisedFunctions {

  def getDstEnrolment(enrolments: Enrolments): Option[EnrolmentIdentifier] = {
    val dst = for {
      enrolment <- enrolments.enrolments if enrolment.key.equalsIgnoreCase("HMRC-DST-ORG")
      dst <- enrolment.getIdentifier("EtmpRegistrationNumber") if dst.value.slice(2, 5) == "DST"
    } yield {
      dst
    }

    dst.headOption
  }

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    implicit val req: Request[A] = request
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    val retrieval =  allEnrolments and credentialRole and internalId and affinityGroup

    authorised(AuthProviders(GovernmentGateway, Verify)).retrieve(retrieval) { case enrolments ~ role ~ id ~ affinity  =>

      val dstNumber = getDstEnrolment(enrolments)

      val internalId = id.getOrElse(throw new RuntimeException("No internal ID for user"))

      val errors: Option[Result] = invalidRole(role)(request).orElse(invalidAffinityGroup(affinity)(request))


       (dstNumber, errors) match {
         case (_, e) if e.nonEmpty => Future.successful(Left(errors.get))
         case (Some(dst), _) => dstConnector.lookupRegistration().map {
           case Some(reg) => Right(AuthorisedRequest(internalId, enrolments, request, dst.value, Some(reg)))
           case None => Right(AuthorisedRequest(internalId, enrolments, request))
         }
         case _ => Future.successful(Right(AuthorisedRequest(internalId, enrolments, request)))
       }

    } recover {
      case _: NoActiveSession =>
        Logger.info(s"Recover - no active session")
        Left(
          Redirect(routes.AuthenticationController.signIn())
        )

    }
  }

  override def parser = mcc.parsers.anyContent

  private def invalidRole(credentialRole: Option[CredentialRole])(implicit request: Request[_]): Option[Result] = {
    credentialRole collect {
      case Assistant => Forbidden(Html("invalidRole"))
    }
  }

  private def invalidAffinityGroup(affinityGroup: Option[AffinityGroup])(implicit request: Request[_]): Option[Result] = {
    affinityGroup match {
      case Some(Agent) | None => Some(Forbidden(Html("invalidAffinity - Agent")))
      case Some(Individual) | None => Some(Forbidden(Html("invalidAffinity - Individual")))
      case _ => None
    }
  }
}

case class AuthorisedRequest[A](
  internalId: String,
  enrolments: Enrolments,
  request: Request[A],
  dstReferenceNo: String = "",
  registration: Option[Registration] = None
) extends WrappedRequest(request)

