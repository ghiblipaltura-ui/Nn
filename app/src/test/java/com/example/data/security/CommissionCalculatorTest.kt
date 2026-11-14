package com.example.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class CommissionCalculatorTest {

  @Test
  fun calculate_freelanceThousandInr_matchesTransparentBreakdown() {
    val result =
      CommissionCalculator.calculate(
        grossAmountInr = 1000.0,
        commissionPercent = 10.0,
        gatewayFeePercent = 2.0,
        operatingCostPercent = 1.5,
      )

    assertEquals(1000.0, result.grossAmountInr, 0.001)
    assertEquals(100.0, result.platformCommissionInr, 0.001)
    assertEquals(900.0, result.providerNetInr, 0.001)
    assertEquals(20.0, result.gatewayFeeInr, 0.001)
    assertEquals(15.0, result.operatingCostInr, 0.001)
    assertEquals(65.0, result.estimatedPlatformNetProfitInr, 0.001)
  }

  @Test
  fun calculate_microtaskHundredInr_matchesFifteenPercentFee() {
    val result =
      CommissionCalculator.calculate(
        grossAmountInr = 100.0,
        commissionPercent = 15.0,
        gatewayFeePercent = 0.0,
        operatingCostPercent = 2.0,
      )

    assertEquals(15.0, result.platformCommissionInr, 0.001)
    assertEquals(85.0, result.providerNetInr, 0.001)
    assertEquals(13.0, result.estimatedPlatformNetProfitInr, 0.001)
  }
}
