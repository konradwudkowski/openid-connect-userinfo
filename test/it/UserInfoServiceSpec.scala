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

package it

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.openidconnect.userinfo.domain._
import uk.gov.hmrc.play.auth.microservice.connectors.ConfidenceLevel

import scalaj.http.Http

class UserInfoServiceSpec extends BaseFeatureSpec {

  val authBearerToken = "AUTH_BEARER_TOKEN"
  val nino = "AB123456A"
  val ukCountryCode = 1
  val desUserInfo = DesUserInfo(DesUserName("John", Some("A"), "Smith"), Some(LocalDate.parse("1980-01-01")),
    DesAddress("1 Station Road", "Town Centre", Some("London"), Some("England"), Some("NW1 6XE"), Some(ukCountryCode)))
  val userInfo = UserInfo(
    Some("John"),
    Some("Smith"),
    Some("A"),
    Some(Address("1 Station Road\nTown Centre\nLondon\nEngland\nNW1 6XE\nGREAT BRITAIN", Some("NW1 6XE"), Some("GREAT BRITAIN"))),
    Some(LocalDate.parse("1980-01-01")),
    Some("AB123456A"))

  feature("fetch user information") {

    scenario("fetch user profile") {

      Given("A Auth token with 'openid', 'profile', 'address' and 'openid:gov-uk-identifiers' scopes")
      thirdPartyDelegatedAuthorityStub.willReturnScopesForAuthBearerToken(authBearerToken,
        Set("openid", "profile", "address", "openid:gov-uk-identifiers"))

      And("The Auth token has a confidence level above 200 and a NINO")
      authStub.willReturnAuthorityWith(ConfidenceLevel.L200, Nino(nino))

      And("DES contains user information for the NINO")
      desStub.willReturnUserInformation(desUserInfo, nino)

      When("We request the user information")
      val result = Http(s"$serviceUrl")
        .headers(Seq("Authorization" -> s"Bearer $authBearerToken", "Accept" -> "application/vnd.hmrc.1.0+json"))
        .asString

      Then("The user information is returned")
      result.code shouldBe 200
      Json.parse(result.body) shouldBe Json.toJson(userInfo)
    }
  }
}
