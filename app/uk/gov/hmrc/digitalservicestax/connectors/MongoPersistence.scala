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

package uk.gov.hmrc.digitalservicestax.connectors

import ltbs.uniform._, interpreters.playframework._

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import java.time.LocalDateTime
// import reactivemongo.api.{ Cursor, DefaultDB, MongoConnection, AsyncDriver }
//import reactivemongo.api.bson.BSONDocument
import play.modules.reactivemongo._
import concurrent.{Future,ExecutionContext}
import java.util.UUID
import scala.util.Try
import play.api._, mvc._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import play.api.libs.json._
import reactivemongo.play.json._, collection._

object MongoPersistence {
  case class Wrapper(
    session: String,
    data: DB,
    timestamp: LocalDateTime = LocalDateTime.now
  )

  implicit val formatMap: OFormat[DB] = new OFormat[DB] {
    def writes(o: DB) = JsObject ( o.map {
      case (k,v) => (k.mkString("/"), JsString(v))
    }.toSeq )

    def reads(json: JsValue): JsResult[DB] = json match {
      case JsObject(data) => JsSuccess(data.map {
        case (k, JsString(v)) => (k.split("/").toList, v)
        case e => throw new IllegalArgumentException(s"cannot parse $e")
      }.toMap)
      case e => JsError(s"expected an object, got $e")
    }
  }  

}

case class MongoPersistence[A <: Request[AnyContent]] (
  reactiveMongoApi: ReactiveMongoApi, 
  collectionName: String,
  expireAfter: Duration,
  useMongoTTL: Boolean = false
)(getSession: A => String)(implicit ec: ExecutionContext) extends PersistenceEngine[A] {
  import MongoPersistence._
  import reactiveMongoApi.database

  def killDate: LocalDateTime = LocalDateTime.now.minusNanos(expireAfter.toNanos)

  def reaver: Future[Unit] = {
    val selector = Json.obj(
      "timestamp" -> Json.obj("$lt" -> killDate)
    )

    collection.map {
      _.delete.one(selector, Some(100))
    }
  }

  lazy val collection: Future[JSONCollection] = {
    database.map(_.collection[JSONCollection](collectionName)).flatMap { c =>

      import reactivemongo.api.indexes._

      val sessionIndex = Index(
        key = Seq("session" -> IndexType.Ascending),
        unique = true
      )

      val timestamp = Index(
        key = Seq("timestamp" -> IndexType.Ascending),
        unique = false //,
//        expire = Some(expireAfter.toSeconds.toInt)
      )

      val im = c.indexesManager

      if (useMongoTTL) { 
        Future.sequence(List(
          im.ensure(sessionIndex),
          im.ensure(timestamp)
        )).map(_ => c)
      } else {
        im.ensure(sessionIndex).map(_ => c)
      }
    }
  }

  implicit val wrapperFormat = Json.format[Wrapper]

  def apply(request: A)(f: DB => Future[(DB, Result)]): Future[Result] = {
    val selector = Json.obj("session" -> getSession(request))
    collection.flatMap(_.find(selector).one[Wrapper]).flatMap {
      case Some(Wrapper(_, data, d)) if {d isBefore killDate} & !useMongoTTL =>
        reaver.flatMap{_ => f(DB.empty)}
      case Some(Wrapper(_, data, _)) =>
        f(data)      
      case None =>
        f(DB.empty)
    } flatMap { case (newDb, result) =>
        val wrapper = Wrapper(getSession(request), newDb)
        collection.flatMap(_.update(selector, wrapper, upsert = true).map{
          case wr: reactivemongo.api.commands.WriteResult if wr.writeErrors.isEmpty => result
          case e => throw new Exception(s"$e")
        })
    }
  }

}
