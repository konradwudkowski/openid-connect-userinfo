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

package it.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import it.{MockHost, Stub}
import uk.gov.hmrc.openidconnect.userinfo.domain.DesUserInfo

object DesStub extends Stub {
  override val stub: MockHost = new MockHost(22222)

  def willReturnUserInformation(desUserInfo: DesUserInfo, nino: String) = {
    val ninoWithoutSuffix = nino.take(8)
    stub.mock.register(get(urlPathEqualTo(s"/pay-as-you-earn/individuals/$ninoWithoutSuffix"))
      .withHeader("Authorization", equalTo("Bearer local"))
      .withHeader("Environment", equalTo("local"))
      .willReturn(aResponse().withBody(
        s"""
          |{
          |  "names": {
          |    "1": {
          |      "firstForenameOrInitial": "${desUserInfo.name.firstForenameOrInitial}",
          |      "secondForenameOrInitial": "${desUserInfo.name.secondForenameOrInitial.get}",
          |      "surname": "${desUserInfo.name.surname}"
          |      }
          |   },
          |  "dateOfBirth": "${desUserInfo.dateOfBirth.get}",
          |  "addresses": {
          |    "1": {
          |      "line1": "${desUserInfo.address.line1}",
          |      "line2": "${desUserInfo.address.line2}",
          |      "line3": "${desUserInfo.address.line3.get}",
          |      "line4": "${desUserInfo.address.line4.get}",
          |      "postcode": "${desUserInfo.address.postcode.get}",
          |      "countryCode": ${desUserInfo.address.countryCode.get}
          |    }
          |  }
          |}
        """.stripMargin)))
  }
}
