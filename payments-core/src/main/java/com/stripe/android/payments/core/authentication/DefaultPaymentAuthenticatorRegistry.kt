package com.stripe.android.payments.core.authentication

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.ViewModel
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.Source
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.injection.AuthenticationComponent
import com.stripe.android.payments.core.injection.DaggerAuthenticationComponent
import com.stripe.android.payments.core.injection.IntentAuthenticatorMap
import com.stripe.android.payments.core.injection.PaymentAuthenticatorRegistryId
import com.stripe.android.view.AuthActivityStarterHost
import java.util.Collections
import java.util.WeakHashMap
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Default registry to provide look ups for [PaymentAuthenticator].
 * Should be only accessed through [DefaultPaymentAuthenticatorRegistry.createInstance].
 */
internal class DefaultPaymentAuthenticatorRegistry @Inject internal constructor(
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator,
    private val sourceAuthenticator: SourceAuthenticator,
    @IntentAuthenticatorMap
    private val paymentAuthenticatorMap:
        Map<Class<out StripeIntent.NextActionData>,
            @JvmSuppressWildcards PaymentAuthenticator<StripeIntent>>,
    @PaymentAuthenticatorRegistryId
    private val paymentAuthenticatorRegistryId: Int
) : PaymentAuthenticatorRegistry {

    /**
     * [AuthenticationComponent] instance is hold to inject into [Activity]s and [ViewModel]s
     * started by the [PaymentAuthenticator]s.
     */
    lateinit var authenticationComponent: AuthenticationComponent

    @Suppress("UNCHECKED_CAST")
    override fun <Authenticatable> getAuthenticator(
        authenticatable: Authenticatable
    ): PaymentAuthenticator<Authenticatable> {
        return when (authenticatable) {
            is StripeIntent -> {
                if (!authenticatable.requiresAction()) {
                    return noOpIntentAuthenticator as PaymentAuthenticator<Authenticatable>
                }
                return (
                    authenticatable.nextActionData?.let {
                        paymentAuthenticatorMap
                            .getOrElse(it::class.java) { noOpIntentAuthenticator }
                    } ?: run {
                        noOpIntentAuthenticator
                    }
                    ) as PaymentAuthenticator<Authenticatable>
            }
            is Source -> {
                sourceAuthenticator as PaymentAuthenticator<Authenticatable>
            }
            else -> {
                error("No suitable PaymentAuthenticator for $authenticatable")
            }
        }
    }

    override fun onNewActivityResultCaller(
        activityResultCaller: ActivityResultCaller,
        activityResultCallback: ActivityResultCallback<PaymentFlowResult.Unvalidated>
    ) {
        paymentAuthenticatorMap.values.forEach {
            it.onNewActivityResultCaller(activityResultCaller, activityResultCallback)
        }
    }

    override fun onLauncherInvalidated() {
        paymentAuthenticatorMap.values.forEach {
            it.onLauncherInvalidated()
        }
    }

    companion object {
        /**
         * Host weak reference of all [DefaultPaymentAuthenticatorRegistry] instances created,
         * the [Activity]s and [ViewModel]s started by the [PaymentAuthenticator]s hosted by the
         * registry will need to access this instance to inject their dependencies.
         * The reference will be garbage collected once it's no longer held by any client.
         */
        private val staticCache: MutableSet<DefaultPaymentAuthenticatorRegistry> =
            Collections.newSetFromMap(WeakHashMap())

        /**
         * A monotonically increasing ID to identify all instances created.
         */
        private var CURRENT_ID = 1

        /**
         * Retrieve an instance by its id.
         */
        fun retrieveInstance(id: Int): DefaultPaymentAuthenticatorRegistry? {
            staticCache.forEach {
                if (it.paymentAuthenticatorRegistryId == id) {
                    return it
                }
            }
            return null
        }

        /**
         * Create an instance of [PaymentAuthenticatorRegistry] with dagger and register it in the
         * static cache.
         *
         * [Synchronized] because it modifies [CURRENT_ID] for each new instance created.
         */
        @Synchronized
        fun createInstance(
            context: Context,
            stripeRepository: StripeRepository,
            paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter,
            paymentBrowserAuthStarterFactory: (AuthActivityStarterHost) -> PaymentBrowserAuthStarter,
            analyticsRequestExecutor: AnalyticsRequestExecutor,
            analyticsRequestFactory: AnalyticsRequestFactory,
            enableLogging: Boolean,
            workContext: CoroutineContext,
            uiContext: CoroutineContext,
            threeDs1IntentReturnUrlMap: MutableMap<String, String>
        ): PaymentAuthenticatorRegistry {
            val component = DaggerAuthenticationComponent.builder()
                .context(context)
                .stripeRepository(stripeRepository)
                .paymentRelayStarterFactory(paymentRelayStarterFactory)
                .paymentBrowserAuthStarterFactory(paymentBrowserAuthStarterFactory)
                .analyticsRequestExecutor(analyticsRequestExecutor)
                .analyticsRequestFactory(analyticsRequestFactory)
                .enableLogging(enableLogging)
                .workContext(workContext)
                .uiContext(uiContext)
                .threeDs1IntentReturnUrlMap(threeDs1IntentReturnUrlMap)
                .paymentAuthenticatorRegistryId(CURRENT_ID++)
                .build()
            val registry = component.registry
            registry.authenticationComponent = component
            staticCache.add(registry)
            return registry
        }
    }
}
