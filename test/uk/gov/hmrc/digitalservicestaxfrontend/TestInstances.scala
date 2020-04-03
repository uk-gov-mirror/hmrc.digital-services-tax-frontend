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

import java.time.LocalDate

import enumeratum.scalacheck._
import cats.implicits.{none, _}
import org.scalacheck.Arbitrary.{arbitrary, arbBigDecimal => _, _}
import org.scalacheck.cats.implicits._
import org.scalacheck.{Arbitrary, Gen, _}
import uk.gov.hmrc.digitalservicestax.data._
import wolfendale.scalacheck.regexp.RegexpGen
import uk.gov.hmrc.auth.core._

object TestInstances {

  implicit class RichRegexValidatedString[A <: RegexValidatedString](val in: A) {
    def gen = RegexpGen.from(in.regex).map{in.apply}
  }
  // these characters look very suspect to me
  implicit val arbString: Arbitrary[String] = Arbitrary(
    Gen.alphaNumStr.map{_.take(255)}
    //    RegexpGen.from("""^[0-9a-zA-Z{À-˿’}\\- &`'^._|]{1,255}$""")
  )
  // what range of values is acceptable? pennies? fractional pennies?
  implicit val arbMoney: Arbitrary[Money] = Arbitrary(
    Gen.choose(0L, Long.MaxValue).map{BigDecimal.apply}

  )

  val ibanList = List(
    "AD9179714843548170724658",
    "AE532299249995935421750",
    "AL36442788709271283994894168",
    "AT836670643070032585",
    "AZ73GIGY95609694664952978687",
    "BA590483797190961278",
    "BE83158799706920",
    "BG29EFYA04741506522965",
    "BH45BSND64749329633519",
    "BR2900994781520950149594104E9",
    "BY13NBRB3600900000002Z00AB00",
    "CH2669331784946419826",
    "CR05015202001026284066",
    "CY75371695368399798483913159",
    "CZ9667197451460125048740",
    "DE09355072686373974067",
    "DK2017303660295651",
    "DO72758351294497410197174992",
    "EE803801541625441184",
    "ES2837832292261368335005",
    "FI6346426364591804",
    "FO5778020271560713",
    "FR2531682128768051490609537",
    "GB42RHBR54612317078692",
    "GE20NB4430283848566197",
    "GI44UQCR057428239600992",
    "GL0502210301477863",
    "GR7642039964850941669463374",
    "GT46866930583377769763582334",
    "HR7971488917057910183",
    "HU91584374853132521152169392",
    "IE27CTJG26368089744633",
    "IL862354629185675963157",
    "IQ98NBIQ850123456789012",
    "IS925454646268060577432601",
    "IT82K7579579031831283849724",
    "JO94CBJO0010000000000131000302",
    "KW23YOUF8981762754308793114869",
    "KZ830907614112967961",
    "LB52372840451692007088329912",
    "LC55HEMM000100010012001200023015",
    "LI1935676368061346475",
    "LT192412904756279442",
    "LU951073196477746242",
    "LV88HAGO9615590045208",
    "MC3212096543766737867569392",
    "MD9881991481700015080266",
    "ME36752723963983139414",
    "MK31948483764450559",
    "MR5442207381102036325668909",
    "MT84AIWA00813109843252965695890",
    "MU65OSUR4348845783494200142ABC",
    "NL64VCYA2607250706",
    "NO2451742753161",
    "PK13LCJY3078553597017331",
    "PL64778828144458067930515057",
    "PS38LEKA813957891201287138525",
    "PT87847144161279936872891",
    "QA58DOHB00001234567890ABCDEFG",
    "RO17KRBO3212835705756123",
    "RS80246113647338221492",
    "SA0979413508515371577583",
    "SC18SSCB11010000000000001497USD",
    "SE0651297191201320278580",
    "SI75412441865827872",
    "SK7336568401664473733427",
    "SM67H0718392993037614346095",
    "ST32000200010192194210112",
    "SV62CENR00000000000000700025",
    "TL380080012345678910157",
    "TN6199977105796904072050",
    "TR330006100519786457841326",
    "TR888859050625760496700846",
    "UA213223130000026007233566001",
    "VA59001123000012345678",
    "BE71096123456769",
    "FR7630006000011234567890189",
    "DE91100000000123456789",
    "GR9608100010000001234567890",
    "RO09BCYP0000001234567890",
    "SA4420000001234567891234",
    "ES7921000813610123456789",
    "CH5604835012345678009",
    "GB98MIDL07009312345678",
    "VG14NDUM4605555206975725",
    "XK051212012345678906",
    "BE71 0961 2345 6769",
    "FR76 3000 6000 0112 3456 7890 189",
    "DE91 1000 0000 0123 4567 89",
    "GR96 0810 0010 0000 0123 4567 890",
    "RO09 BCYP 0000 0012 3456 7890",
    "SA44 2000 0001 2345 6789 1234",
    "ES79 2100 0813 6101 2345 6789",
    "CH56 0483 5012 3456 7800 9",
    "GB98 MIDL 0700 9312 3456 78"
  )

  implicit val arbIban: Arbitrary[IBAN] = Arbitrary {
    Gen.oneOf(ibanList).map(IBAN.apply)
  }

  implicit val argRestrictedString: Arbitrary[RestrictiveString] = Arbitrary(
    RestrictiveString.gen
    //    RegexpGen.from("""^[0-9a-zA-Z{À-˿’}\\- &`'^._|]{1,255}$""")
  )


  implicit val arbPercent: Arbitrary[Percent] = Arbitrary {
    Gen.chooseNum(0, 100).map(b => Percent(b.toByte))
  }

  def nonEmptyString: Gen[NonEmptyString] =
    arbitrary[RestrictiveString].filter(_.nonEmpty).map { NonEmptyString.apply }

  def genActivityPercentMap: Gen[Map[Activity, Percent]] = Gen.mapOf(
    (
      arbitrary[Activity],
      Gen.choose(0,100).map{x => Percent.apply(x.asInstanceOf[Byte])}
      ).tupled
  )

  def genGroupCo: Gen[GroupCompany] = (
    nonEmptyString,
    Gen.option(UTR.gen)
    ).mapN(GroupCompany.apply)

  implicit def genEnrolmentIdentifier: Arbitrary[EnrolmentIdentifier] = Arbitrary {
    (
      nonEmptyString,
      nonEmptyString
    ).mapN(EnrolmentIdentifier)
  }

  implicit def enrolmentArbitrary: Arbitrary[Enrolment] = Arbitrary {
    for {
      key <- arbitrary[NonEmptyString]
      sized <- Gen.chooseNum(1, 10)
      enrolments <- Gen.listOfN(sized, genEnrolmentIdentifier.arbitrary)
      state <- nonEmptyString
      delegate <- Gen.const(none[String])
    } yield Enrolment(key, enrolments, state, delegate)
  }

  implicit def enrolmentsArbitrary: Arbitrary[Enrolments] = Arbitrary {
    for {
      num <- Gen.chooseNum(1, 5)
      list <- Gen.listOfN(num, enrolmentArbitrary.arbitrary)
    } yield Enrolments(list.toSet)
  }

  def gencomap: Gen[Map[GroupCompany, Money]] = Gen.mapOf(
    (
      genGroupCo,
      arbitrary[Money]
      ).tupled
  )

  def genBankAccount: Gen[BankAccount] = {

    val genDomestic: Gen[DomesticBankAccount] = (
      SortCode.gen,
      AccountNumber.gen,
      arbitrary[String]
      ).mapN(DomesticBankAccount.apply)

    val genForeign: Gen[ForeignBankAccount] =
      arbitrary[IBAN].map { ForeignBankAccount.apply }
    Gen.oneOf(genDomestic, genForeign)
  }

  def genRepayment: Gen[RepaymentDetails] =
    (
      nonEmptyString,
      genBankAccount
      ).mapN(RepaymentDetails.apply)

  def date(start: LocalDate, end: LocalDate): Gen[LocalDate] =
    Gen.choose(start.toEpochDay, end.toEpochDay).map(LocalDate.ofEpochDay)

  implicit def returnGen: Arbitrary[Return] = Arbitrary((
    genActivityPercentMap,
    arbitrary[Money],
    gencomap,
    arbitrary[Money],
    arbitrary[Money],
    Gen.option(genRepayment)
    ).mapN(Return.apply))

  implicit def arbDate: Arbitrary[LocalDate] = Arbitrary(
    date(LocalDate.of(2010, 1, 1), LocalDate.of(2020, 1, 1))
  )

  implicit def periodArb: Arbitrary[Period] = Arbitrary((
    arbitrary[LocalDate],
    arbitrary[LocalDate],
    arbitrary[LocalDate],
    arbitrary[NonEmptyString].map{_.take(4)}.map{Period.Key(_)}
    ).mapN(Period.apply))

  def neString(maxLen: Int = 255) = (
    (
      Gen.alphaNumChar,
      arbitrary[String]
      ).mapN(_ + _).
      map{_.take(maxLen)}.map{NonEmptyString.apply}
    )

  implicit def arbNEString: Arbitrary[NonEmptyString] = Arbitrary { neString() }
  implicit def arbPostcode: Arbitrary[Postcode] = Arbitrary(Postcode.gen)
  implicit def arbDSTNumber: Arbitrary[DSTRegNumber] = Arbitrary(DSTRegNumber.gen)
  implicit def arbFormBundleNumber: Arbitrary[FormBundleNumber] = Arbitrary(FormBundleNumber.gen)
  implicit def arbCountryCode: Arbitrary[CountryCode] = Arbitrary(CountryCode.gen)
  implicit def arbPhone: Arbitrary[PhoneNumber] = Arbitrary(PhoneNumber.gen)
  implicit def arbUTR: Arbitrary[UTR] = Arbitrary(UTR.gen)
  implicit val arbInternalId: Arbitrary[InternalId] = Arbitrary(InternalId.gen)

  // note this does NOT check all RFC-compliant email addresses (e.g. '"luke tebbs"@company.co.uk')
  implicit def arbEmail: Arbitrary[Email] = Arbitrary{
    (
      neString(20),
      neString(20)
      ).mapN((a,b) => Email(s"${a}@${b}.co.uk"))
  }

  implicit def arbForeignAddress: Arbitrary[ForeignAddress] = Arbitrary {
    (
      neString(40),
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[CountryCode]
    ).mapN(ForeignAddress)
  }

  implicit def arbUkAddress: Arbitrary[UkAddress] = Arbitrary {
    (
      neString(40),
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[Postcode]
    ).mapN(UkAddress.apply)
  }


  implicit def arbAddr: Arbitrary[Address] = Arbitrary {

    val ukGen: Gen[Address] = (
      neString(40),
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[Postcode]
      ).mapN(UkAddress.apply)

    val foreignGen: Gen[Address] = (
      neString(40),
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[String].map{_.take(40)},
      arbitrary[CountryCode]
      ).mapN(ForeignAddress)

    Gen.oneOf(ukGen, foreignGen)

  }

  implicit def arbCo: Arbitrary[Company] = Arbitrary(
    (
      arbitrary[NonEmptyString],
      arbitrary[Address]
      ).mapN(Company.apply)
  )

  implicit def arbCoRegWrap: Arbitrary[CompanyRegWrapper] = Arbitrary(
    (
      arbitrary[Company],
      Gen.const(none[UTR]),
      Gen.const(none[SafeId]),
      Gen.const(false)
      ).mapN(CompanyRegWrapper.apply)
  )

  implicit def arbContact: Arbitrary[ContactDetails] = Arbitrary {
    (
      arbitrary[RestrictiveString],
      arbitrary[RestrictiveString],
      arbitrary[PhoneNumber],
      arbitrary[Email]
      ).mapN(ContactDetails.apply)
  }

  implicit def subGen: Arbitrary[Registration] = Arbitrary (
    {
      (
        arbitrary[CompanyRegWrapper],
        Gen.option(arbitrary[Address]),
        arbitrary[Option[Company]],
        arbitrary[ContactDetails],
        date(LocalDate.of(2039,1,1), LocalDate.of(2040,1,1)),
        arbitrary[LocalDate],
        Gen.some(arbitrary[DSTRegNumber])
        ).mapN(Registration.apply)
    }
  )

  implicit def arbCredRole: Arbitrary[CredentialRole] = Arbitrary {
    Gen.oneOf(List(User, Admin, Assistant))
  }

  implicit def arbAffinityGroup: Arbitrary[AffinityGroup] = Arbitrary {
    Gen.oneOf(List(AffinityGroup.Agent, AffinityGroup.Individual, AffinityGroup.Organisation))
  }

}