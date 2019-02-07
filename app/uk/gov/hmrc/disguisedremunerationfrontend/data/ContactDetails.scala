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

package dr

import ltbs.uniform._
import org.atnos.eff._
import cats.implicits._


case class ContactDetails (
  address: Address,
  telephoneAndEmail: (String, String)
)

object ContactDetails {
  type Stack = Fx.fx2[UniformAsk[Address,?], UniformAsk[(String,String),?]]

  def program[R
      : _uniform[Address,?]
      : _uniform[(String,String),?]
  ]: Eff[R, ContactDetails] = for {
    address           <- uask[R,Address]("address")
    telephoneAndEmail <- uask[R,(String,String)]("telephoneAndEmail")
  } yield (ContactDetails(address,telephoneAndEmail))

}
