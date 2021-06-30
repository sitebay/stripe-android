package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Logger
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class PaymentMethodsApiRepository(
    private val stripeRepository: StripeRepository,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    enableLogging: Boolean = false,
    private val workContext: CoroutineContext
) : PaymentMethodsRepository {
    /**
     * A [PaymentMethodsRepository] that uses the Stripe API.
     */
    private val logger = Logger.getInstance(enableLogging)

    /**
     * Retrieve a Customer's payment methods of the given types. Silently handle failures by
     * returning an empty list.
     */
    override suspend fun get(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>
    ): List<PaymentMethod> = withContext(workContext) {
        runCatching {
            types.map { type ->
                async {
                    stripeRepository.getPaymentMethods(
                        ListPaymentMethodsParams(
                            customerId = customerConfig.id,
                            paymentMethodType = type
                        ),
                        publishableKey,
                        PRODUCT_USAGE,
                        ApiRequest.Options(
                            customerConfig.ephemeralKeySecret,
                            stripeAccountId
                        )
                    )
                }
            }.awaitAll().flatten()
        }.onFailure {
            logger.error("Failed to retrieve ${customerConfig.id}'s payment methods.", it)
        }.getOrDefault(emptyList())
    }

    private companion object {
        private val PRODUCT_USAGE = setOf("PaymentSheet")
    }
}
