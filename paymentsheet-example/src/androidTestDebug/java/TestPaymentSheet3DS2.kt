import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.base.Predicates.equalTo
import com.stripe.android.paymentsheet.example.MainActivity
import com.stripe.android.paymentsheet.example.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher

@RunWith(AndroidJUnit4::class)
@LargeTest
class TestPaymentSheet3DS2 {
    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<MainActivity> = activityScenarioRule()

    @Before
    fun setup() {

    }

    @Test
    fun launchPaymentSheet() {
        // launch PaymentSheet complete
        onView(ViewMatchers.withId(R.id.launch_custom_button))
            .perform(ViewActions.click())

        TimeUnit.SECONDS.sleep(5)

        onView(ViewMatchers.withId(R.id.payment_method))
            .perform(ViewActions.click())

        TimeUnit.SECONDS.sleep(5)
        onData(allOf("****3220")).performClick()
    }
}
