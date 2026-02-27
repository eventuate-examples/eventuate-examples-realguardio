class JavaQualityExtension {
    String checkstyleToolVersion = '10.12.5'
    String pmdToolVersion = '6.55.0'
    String jacocoToolVersion = '0.8.12'

    File checkstyleConfigFile
    File pmdRulesetFile

    BigDecimal minLineCoverage = 0.0
    BigDecimal minBranchCoverage = null // set to e.g. 0.70 to enable
}
