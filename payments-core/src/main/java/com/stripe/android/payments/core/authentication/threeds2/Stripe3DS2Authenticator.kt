package com.stripe.android.payments.core.authentication.threeds2

import com.stripe.android.PaymentAuthConfig
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.PaymentAuthenticatorRegistryId
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * [PaymentAuthenticator] authenticating through Stripe's 3ds2 SDK.
 */
@Singleton
internal class Stripe3DS2Authenticator @Inject constructor(
    private val config: PaymentAuthConfig,
    @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    @PaymentAuthenticatorRegistryId private val paymentAuthenticatorRegistryId: Int,
) : PaymentAuthenticator<StripeIntent> {

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        host.startActivityForResult(
            Stripe3ds2TransactionActivity::class.java,
            Stripe3ds2TransactionContract.Args(
                SdkTransactionId.create(),
                config.stripe3ds2Config,
                authenticatable,
                authenticatable.nextActionData as StripeIntent.NextActionData.SdkData.Use3DS2,
                requestOptions,
                enableLogging = enableLogging,
                host.statusBarColor,
                paymentAuthenticatorRegistryId

            ).toBundle(),
            StripePaymentController.getRequestCode(authenticatable)
        )
    }
}
