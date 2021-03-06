/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.disguisedremunerationfrontend.data


import ltbs.uniform._
import org.atnos.eff._
import uk.gov.hmrc.disguisedremunerationfrontend.controllers.EmploymentStatus

case class AboutYou(
  completedBySelf: Boolean,
  alive: Boolean,
  identification: Option[Either[Nino, Utr]] = None,
  deceasedBefore: Option[Boolean] = None,
  employmentStatus: Option[EmploymentStatus] = None,
  actingFor: Option[String] = None
) {

  private def nino: Option[Nino] = identification match {
    case Some(Left(n)) => Some(n)
    case _ => None
  }
}

object AboutYou {

  // Move into utils
  lazy val regExUTR = """^(?:[ \t]*(?:[a-zA-Z]{3})?\d[ \t]*){10}$"""
  lazy val regExNino = """^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$"""

  sealed trait Error
  case object NoNeedToComplete extends Error

  type Stack = Fx.fx5[
    UniformAsk[Boolean,?],
    UniformAsk[String,?],
    UniformAsk[Unit,?],
    UniformAsk[Either[Nino,Utr],?],
    UniformAsk[EmploymentStatus,?]
  ]

  def program[R
      : _uniformCore
      : _uniformAsk[Boolean,?]
      : _uniformAsk[String,?]
      : _uniformAsk[Either[Nino,Utr],?]
      : _uniformAsk[EmploymentStatus,?]
      : _uniformAsk[Unit,?]
  ](default: Option[Option[AboutYou]], nino: Option[Nino], utr: Option[Utr]): Eff[R, Either[Error,Option[AboutYou]]] =
    for {
      completedBy <- ask[Boolean]("aboutyou-completedby")
                       .defaultOpt(default.map(_.isDefined))
      ret <- completedBy match {
        case false =>
          (nino, utr) match {
            case (None, None) =>
              for {
                nino <- ask[Nino]("aboutyou-nino")
                  .defaultOpt(default.flatMap(_.flatMap(_.nino)))
                  .validating(
                    "format",
                    x => x.toUpperCase.replaceAll("\\s","").matches(regExNino)
                  )
                  .in[R]
              } yield {
                Right(Some(AboutYou(completedBySelf = true, alive = true, Some(Left(nino)), None, None, None)))
              }
            case _ =>
              Eff.pure[R, Either[Error, Option[AboutYou]]](Right(None))
          }
        case true => for {
          alive   <- ask[Boolean]("aboutyou-personalive")
                       .defaultOpt(default.flatMap(_.map(_.alive)))
          employmentStatus  <- ask[EmploymentStatus]("aboutyou-employmentstatus")
                                 .defaultOpt(default.flatMap(_.flatMap(_.employmentStatus))).in[R] when !alive
          deceasedBefore  <- ask[Boolean]("aboutyou-deceasedbefore")
                                  .defaultOpt(default.flatMap(_.flatMap(_.deceasedBefore)))
                                  .in[R] when employmentStatus == Some(EmploymentStatus.Employed)
          notRequiredToComplete = deceasedBefore == Some(true)
          _ <- tell[Unit]("aboutyou-noloancharge")("_").in[R] when notRequiredToComplete
          id <- ask[Either[Nino,Utr]]("aboutyou-identity")
                  .defaultOpt(default.flatMap(_.flatMap(_.identification)))
                  .validating(
                    "nino-format",
                    {
                      case Left(nino) => nino.matches(regExNino)
                      case _ => true
                    }
                  )
                  .validating(
                    "utr-format",
                    {
                      case Left(nino) => true
                      case Right(utr) => utr.matches(regExUTR)
                    }
                  )
                  .in[R] when (!notRequiredToComplete)
          personName <- ask[String]("aboutyou-confirmation")
                          .defaultOpt(default.flatMap(_.flatMap(_.actingFor)))
                          .in[R] when !id.isEmpty
        } yield {
          if (notRequiredToComplete)
            Left(NoNeedToComplete)
          else
            Right(Some(AboutYou(false, alive, id, deceasedBefore, employmentStatus, personName)))
        }
      }
  } yield (ret)

}
