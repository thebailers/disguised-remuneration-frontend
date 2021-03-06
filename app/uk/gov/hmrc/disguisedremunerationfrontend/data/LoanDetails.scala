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

case class LoanDetails(
  year: Int,
  hmrcApproved: Boolean,
  amount: Money,
  genuinelyRepaid: Option[Money],
  writtenOff: Option[WrittenOff]
) {
  // TODO move this to a pimp
  def toListString = {
    List(
      "£" ++ amount.toString,
      genuinelyRepaid.fold("£" ++ "0")(gr => "£" ++ gr.toString),
      writtenOff.fold("£" ++ "0")(wo =>"£" ++ wo.amount.toString),
      writtenOff.fold("£" ++ "0")(wo =>"£" ++ wo.taxPaid.toString)
    )
  }
}

import java.time.format.DateTimeFormatter

import cats.implicits._
import org.atnos.eff._
import ltbs.uniform._
import uk.gov.hmrc.disguisedremunerationfrontend.data.Scheme.MoneyRegex

object LoanDetails {

  type Stack = Fx.fx4[
    UniformAsk[Boolean, ?],
    UniformAsk[Money, ?],
    UniformAsk[Option[Money], ?],
    UniformAsk[WrittenOff, ?]
    ]

  def program[R
  : _uniformCore
  : _uniformAsk[Boolean, ?]
  : _uniformAsk[Money, ?]
  : _uniformAsk[Option[Money], ?]
  : _uniformAsk[WrittenOff, ?]
  ](year: Int, default: Option[LoanDetails] = None): Eff[R, LoanDetails] = {
    val (startDate, endDate) = year.toFinancialYear
    for {
      approved <- ask[Boolean]("details-hmrc-approved").defaultOpt(default.map(_.hmrcApproved)).in[R]
      amount <- ask[Money]("details-amount")
        .defaultOpt(default.map(_.amount))
        .validating(
          "format",
          x => x.matches(MoneyRegex)
        )
        .withCustomContentAndArgs(
          ("details-amount.heading",
            ("details-amount.heading.range",
            List(startDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY")),
              endDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY")))
            ))
        ).in[R]
      repaid <-  ask[Money]("details-genuinely-repaid-amount")
        .defaultOpt(default.flatMap(_.genuinelyRepaid))
        .validating(
          "format",
          x => x.matches(MoneyRegex)
        ).in[R] when
        ask[Boolean]("details-genuinely-repaid")
          .defaultOpt(default.map(_.genuinelyRepaid != 0))
          .withCustomContentAndArgs(
            ("details-genuinely-repaid.heading",
              ("details-genuinely-repaid.heading.range",
                List(startDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY")),
                  endDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY")))
              ))
          ).in[R]
      writtenOff <- ask[WrittenOff]("details-written-off-amount")
        .defaultOpt(default.flatMap(_.writtenOff))
        //TODO Need a feature to validate both together, right now it is sequential
//          .validating(
//            "both-format",
//            {
//              case WrittenOff(amount, tax) => amount.matches(MoneyRegex) && tax.matches(MoneyRegex)
//              case _ => true
//            }
//          )
        .validating(
          "amount-format",
            x => x.amount.matches(MoneyRegex)
        )
        .validating(
          "tax-format",
           x => x.taxPaid.matches(MoneyRegex)
        )
        .in[R] when
        ask[Boolean]("details-written-off")
          .defaultOpt(default.map(_.writtenOff.isDefined))
          .withCustomContentAndArgs(
            ("details-written-off.heading",
              ("details-written-off.heading.range",
                List(startDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY")),
                  endDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY")))
              ))
          ).in[R]
    } yield {
      LoanDetails(year, approved, amount, repaid, writtenOff)
    }
  }
}

