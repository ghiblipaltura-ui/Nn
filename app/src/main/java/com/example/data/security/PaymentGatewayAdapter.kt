package com.example.data.security

import java.util.UUID

data class TestPaymentReceipt(
  val paymentReference: String,
  val idempotencyKey: String,
  val isTestMode: Boolean = true,
  val verified: Boolean = true,
)

object PaymentGatewayAdapter {
  /**
   * TEST MODE payment adapter. Never stores UPI PINs, bank passwords, or card CVVs.
   * Future live integration will verify Razorpay/Cashfree/PayU server webhook signatures
   * before marking any transaction as settled.
   */
  fun authorizeTestPayment(orderType: String, referenceId: String, amountInr: Double): TestPaymentReceipt {
    require(amountInr > 0.0) { "Payment amount must be positive" }
    val shortId = UUID.randomUUID().toString().take(8).uppercase()
    return TestPaymentReceipt(
      paymentReference = "TEST_${orderType}_$shortId",
      idempotencyKey = "${orderType}_${referenceId}_$shortId",
      isTestMode = true,
      verified = true,
    )
  }

  fun maskUpiOrAccount(rawInput: String): String {
    val clean = rawInput.trim()
    return if (clean.contains("@")) {
      val parts = clean.split("@", limit = 2)
      val prefix = parts[0].take(3) + "***"
      "$prefix@${parts[1]}"
    } else if (clean.length > 4) {
      "XXXX-" + clean.takeLast(4)
    } else {
      clean
    }
  }
}
