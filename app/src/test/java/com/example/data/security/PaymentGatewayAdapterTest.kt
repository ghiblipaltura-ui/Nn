package com.example.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentGatewayAdapterTest {

  @Test
  fun authorizeTestPayment_positiveAmount_returnsTestModeReceipt() {
    val receipt = PaymentGatewayAdapter.authorizeTestPayment("GIG", "ref123", 1000.0)
    assertTrue(receipt.isTestMode)
    assertTrue(receipt.verified)
    assertTrue(receipt.paymentReference.startsWith("TEST_GIG_"))
    assertTrue(receipt.idempotencyKey.startsWith("GIG_ref123_"))
  }

  @Test(expected = IllegalArgumentException::class)
  fun authorizeTestPayment_nonPositiveAmount_throws() {
    PaymentGatewayAdapter.authorizeTestPayment("GIG", "ref123", 0.0)
  }

  @Test
  fun maskUpiOrAccount_masksSensitivePrefixes() {
    assertEquals("nit***@okicici", PaymentGatewayAdapter.maskUpiOrAccount("nitin.dash@okicici"))
    assertEquals("XXXX-7890", PaymentGatewayAdapter.maskUpiOrAccount("1234567890"))
  }
}
