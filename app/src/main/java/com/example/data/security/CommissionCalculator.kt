package com.example.data.security

import kotlin.math.roundToLong

data class CommissionBreakdown(
  val grossAmountInr: Double,
  val commissionPercent: Double,
  val platformCommissionInr: Double,
  val providerNetInr: Double,
  val gatewayFeeInr: Double,
  val operatingCostInr: Double,
  val estimatedPlatformNetProfitInr: Double,
)

object CommissionCalculator {
  fun calculate(
    grossAmountInr: Double,
    commissionPercent: Double,
    gatewayFeePercent: Double = 2.0,
    operatingCostPercent: Double = 1.5,
  ): CommissionBreakdown {
    require(grossAmountInr >= 0.0) { "Gross amount cannot be negative" }
    require(commissionPercent in 0.0..50.0) { "Commission percent must be between 0 and 50" }

    val platformCommission = roundInr(grossAmountInr * (commissionPercent / 100.0))
    val providerNet = roundInr(grossAmountInr - platformCommission)
    val gatewayFee = roundInr(grossAmountInr * (gatewayFeePercent / 100.0))
    val operatingCost = roundInr(grossAmountInr * (operatingCostPercent / 100.0))
    val netProfit = roundInr(platformCommission - gatewayFee - operatingCost)

    return CommissionBreakdown(
      grossAmountInr = roundInr(grossAmountInr),
      commissionPercent = commissionPercent,
      platformCommissionInr = platformCommission,
      providerNetInr = providerNet,
      gatewayFeeInr = gatewayFee,
      operatingCostInr = operatingCost,
      estimatedPlatformNetProfitInr = netProfit,
    )
  }

  private fun roundInr(value: Double): Double = (value * 100.0).roundToLong() / 100.0
}
