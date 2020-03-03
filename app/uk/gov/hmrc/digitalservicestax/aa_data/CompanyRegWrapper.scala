package uk.gov.hmrc.digitalservicestax.data

case class CompanyRegWrapper(
  company: Company,
  utr: Option[UTR] = None,
  safeId: Option[SafeId] = None,
  useSafeId: Boolean = false
)
