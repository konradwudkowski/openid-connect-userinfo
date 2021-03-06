/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.openidconnect.userinfo.services

import org.joda.time.LocalDate
import uk.gov.hmrc.openidconnect.userinfo.connectors.ThirdPartyDelegatedAuthorityConnector
import uk.gov.hmrc.openidconnect.userinfo.domain.{DesAddress, Address, UserInfo, DesUserInfo}
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{UnauthorizedException, HeaderCarrier}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait UserInfoTransformer {

  val countryService: CountryService
  val thirdPartyDelegatedAuthorityConnector: ThirdPartyDelegatedAuthorityConnector

  val addLine = PartialFunction[Option[String], String](_.map(s => s"\n$s").getOrElse(""))

  def transform(desUserInfo: Option[DesUserInfo], nino: String)(implicit hc:HeaderCarrier): Future[UserInfo] = {
    def bearerToken(authorization: Authorization) = authorization.value.stripPrefix("Bearer ")

    hc.authorization match {
      case Some(authorization) =>  thirdPartyDelegatedAuthorityConnector.fetchScopes(bearerToken(authorization)) map { scopes =>
        constructUserInfo(desUserInfo, nino, scopes)
      }
      case None => Future.failed(new UnauthorizedException("Bearer token is required"))
    }
  }

  private def constructUserInfo(desUserInfo: Option[DesUserInfo], nino: String, scopes: Set[String]): UserInfo = {
    val userProfile = desUserInfo map (u => UserProfile(u.name.firstForenameOrInitial, u.name.surname, u.name.secondForenameOrInitial, u.dateOfBirth))
    val country = desUserInfo flatMap (u => u.address.countryCode flatMap countryService.getCountry)

    val profile = if (scopes.contains("profile")) userProfile else None
    val identifier = if (scopes.contains("openid:gov-uk-identifiers")) Some(nino) else None
    val address = if (scopes.contains("address")) desUserInfo map (u => Address(formattedAddress(u.address, country), u.address.postcode, country)) else None

    UserInfo(profile.map(_.firstName),
      profile.map(_.familyName),
      profile.flatMap(_.middleName),
      address,
      profile.flatMap(_.birthDate),
      identifier)
  }

  private def formattedAddress(desAddress: DesAddress, country: Option[String]) = {
    s"${desAddress.line1}\n${desAddress.line2}${addLine(desAddress.line3)}${addLine(desAddress.line4)}${addLine(desAddress.postcode)}${addLine(country)}"
  }

  private case class UserProfile(firstName: String, familyName: String, middleName: Option[String], birthDate: Option[LocalDate])
}

object UserInfoTransformer extends UserInfoTransformer {
  override val countryService = CountryService
  override val thirdPartyDelegatedAuthorityConnector = ThirdPartyDelegatedAuthorityConnector
}
