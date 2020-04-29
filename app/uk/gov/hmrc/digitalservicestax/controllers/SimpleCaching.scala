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

import java.time.LocalDateTime
import ltbs.uniform.common.web._
import scala.collection.immutable.ListMap
import scala.concurrent._, duration._
import cats.syntax.order._

case class SimpleCaching[Key](
  lifetime: Duration = 10.minutes
) {

  @volatile var _data: Map[Key, (String, LocalDateTime, Any)] = ListMap.empty

  def clean(): Unit = {
    _data = _data.filter(_._2._2 + lifetime > LocalDateTime.now)
  }

  def sha256Hash(in: String): String = {
    import java.security.MessageDigest
    import java.math.BigInteger
    val bytes = in.getBytes("UTF-8")
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    String.format("%032x", new BigInteger(1, digest))
  }

  def fromCache[A](id: Key, args: Any*): Option[A] = {
    val checkSum = sha256Hash(args.map{_.toString}.mkString("<:>"))
    _data.get(id) flatMap {
      case (sum, when, untyped) if sum == checkSum && (when + lifetime) > LocalDateTime.now =>
        Some(untyped.asInstanceOf[A])
      case _ =>
        clean()
        None
    }
  }

  def update[A](id: Key, args: Seq[Any], value: A): Unit = {
    val checkSum = sha256Hash(args.map{_.toString}.mkString("<:>"))
    _data = _data + (id -> ((checkSum, LocalDateTime.now, value)))
  }

  def apply[A](id: Key, args: Any*)(action: => Future[A])(implicit ec: ExecutionContext): Future[A] =
    fromCache[A](id, args:_*).fold {
      action.map { x =>
        update(id, args, x)
        x
      }
    }(Future.successful)
}
