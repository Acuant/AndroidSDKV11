package com.acuant.sampleapp

import android.support.test.rule.ActivityTestRule
import com.acuant.acuantmobilesdk.Controller
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch

/**
 * Created by tapasbehera on 4/27/18.
 */
class TestAcufillWithoutEndpoint {
    @get:Rule
    public var mActivityRule = ActivityTestRule(
            TestMainActivity::class.java)

    /**
     * Test With a valid license key and no endpoint
     * as the cloud address is by default set to https://cssnwebservices.com
     */
    @Test
    fun test() {
        try {
            val signal = CountDownLatch(1)
            val licenseKeyCredentials = LicenseKeyCredential()
            licenseKeyCredentials.licenseKey = "71F86FD1E789"
            Controller.init(licenseKeyCredentials) { error ->
                Assert.assertNull(error)
                val sdk = Controller.getInstance()
                sdk?.cleanup()
                signal.countDown()

            }
            signal.await()
            mActivityRule.finishActivity()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }
}
