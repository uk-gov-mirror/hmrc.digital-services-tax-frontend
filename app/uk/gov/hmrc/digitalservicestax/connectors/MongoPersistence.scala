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
import reactivemongo.api.{ Cursor, DefaultDB, MongoConnection, AsyncDriver }
import reactivemongo.api.bson._
import concurrent.{Future,ExecutionContext}
import java.util.UUID
import scala.util.Try

import play.api._, mvc._

case class Wrapper(
  id: String,
  data: DB
)

case class MongoPersistence[A <: Request[AnyContent]] (
  mongoUri: String = "mongodb://localhost:27017/digital-services-tax",
  databaseName: String,
  collectionName: String
)(getSession: A => String)(implicit ec: ExecutionContext) extends PersistenceEngine[A] {

  val driver = AsyncDriver()
  val parsedUri = MongoConnection fromString mongoUri
  val futureConnection = parsedUri.flatMap(driver.connect(_))

  def db1: Future[DefaultDB] = futureConnection.flatMap(_.database(databaseName))
  def collection = db1.map(_.collection(collectionName))

  implicit def dbWriter = new BSONDocumentWriter[DB] {
    def writeTry(t: DB): Try[BSONDocument] = {
      val elems: Iterable[(String, BSONValue)] = t.map {
        case (k,v) => ("/" + k.mkString("/"), BSONString(v))
      }
      Try(BSONDocument(elems))
    }
  }

  implicit def dbReader = new BSONDocumentReader[DB] {
    def readDocument(doc: BSONDocument): Try[DB] = Try(doc.toMap.map {
      case (k,BSONString(v)) => (k.split("/").toList,v)
    })
  }

  implicit def wrapWriter = Macros.writer[Wrapper]
  implicit def wrapReader = Macros.reader[Wrapper]  

  def apply(request: A)(f: DB => Future[(DB, Result)]): Future[Result] = {
    val selector = document("id" -> getSession(request))
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
        collection.flatMap(_.update.one(selector, newDb).map(_ => result))
    }
  }

}
