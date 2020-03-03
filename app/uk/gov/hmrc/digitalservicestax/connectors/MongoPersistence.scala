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
// import reactivemongo.api.{ Cursor, DefaultDB, MongoConnection, AsyncDriver }
//import reactivemongo.api.bson._
import play.modules.reactivemongo._
import concurrent.{Future,ExecutionContext}
import java.util.UUID
import scala.util.Try
import play.api._, mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.play.json._, collection._

object MongoPersistence {
  case class Wrapper(
    session: String,
    data: DB
  )
}

case class MongoPersistence[A <: Request[AnyContent]] (
  reactiveMongoApi: ReactiveMongoApi, 
  collectionName: String
)(getSession: A => String)(implicit ec: ExecutionContext) extends PersistenceEngine[A] {
  import MongoPersistence.Wrapper
  import reactiveMongoApi.database

  def collection: Future[JSONCollection] =
    database.map(_.collection[JSONCollection](collectionName))

  implicit val formatMap = new OFormat[DB] {
    // Members declared in play.api.libs.json.OWrites
    def writes(o: DB) = JsObject ( o.map {
      case (k,v) => (k.mkString("/"), JsString(v))
    }.toSeq )
  
    // Members declared in play.api.libs.json.Reads
    def reads(json: JsValue): JsResult[DB] = json match {
      case JsObject(data) => JsSuccess(data.map {
        case (k, JsString(v)) => (k.split("/").toList, v)
        case e => throw new IllegalArgumentException(s"cannot parse $e")
      }.toMap)
      case e => JsError(s"expected an object, got $e")
    }

  }
  implicit val formatWrapper = Json.format[Wrapper]

  def apply(request: A)(f: DB => Future[(DB, Result)]): Future[Result] = {
    val selector = Json.obj("session" -> getSession(request))
    println(s">>>>>>>>>>session: ${getSession(request)}")
    collection.flatMap(_.find(selector).one[Wrapper]).flatMap {
      case Some(Wrapper(_, data)) =>
        println(s">>>>>>>>>>read: $data")
        f(data)
      case None =>
        println(s">>>>>>>>>>read empty")
        f(DB.empty)
    } flatMap { case (newDb, result) =>
        println(s">>>>>>>>>>write: $newDb")        
        collection.flatMap(_.update(selector, Wrapper(getSession(request), newDb), upsert = true).map{
          case wr: reactivemongo.api.commands.WriteResult if wr.writeErrors.isEmpty => result
          case e => throw new Exception(s"$e")
        })
    }
  }

}
