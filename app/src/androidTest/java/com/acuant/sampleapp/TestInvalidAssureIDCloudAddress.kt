package com.acuant.sampleapp

import android.support.test.rule.ActivityTestRule

import com.acuant.acuantmobilesdk.Controller

import org.junit.Assert
import org.junit.Rule
import org.junit.Test

import java.util.concurrent.CountDownLatch

/**
 * Created by tapasbehera on 4/19/18.
 */

class TestInvalidAssureIDCloudAddress {
    @get:Rule
    public var mActivityRule = ActivityTestRule(
            TestMainActivity::class.java)

    /**
     * Test With a valid license key and null apiEndpoint to expect return true
     * as the cloud address is by default set to cssnwebservices.com
     */
    @Test
    fun test() {
        try {
            val signal = CountDownLatch(1)
            val subscriptionCredentials = SubscriptionCredential()
            subscriptionCredentials.username = "tbehera@acuantcorp.com"
            subscriptionCredentials.password = "3CQgc6cwhAe4ugkh"
            subscriptionCredentials.subscription = "30A24E86-9A18-423F-9939-533AF439CA4F"
            subscriptionCredentials.endpoint = "services.assureid.com"
            Controller.init(subscriptionCredentials) { error ->
                Assert.assertNotNull(error)
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
