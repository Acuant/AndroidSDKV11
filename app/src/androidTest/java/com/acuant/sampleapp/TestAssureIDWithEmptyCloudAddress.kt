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

class TestAssureIDWithEmptyCloudAddress {
    @get:Rule
    public var mActivityRule = ActivityTestRule(
            TestMainActivity::class.java)

    /**
     * Test With a valid license key and null url to expect return true
     * as the cloud address is by default set to cssnwebservices.com
     */
    @Test
    fun test() {
        try {
            val signal = CountDownLatch(1)
            val subscriptionCredential = SubscriptionCredential()
            subscriptionCredential.username = "tbehera@acuantcorp.com"
            subscriptionCredential.password = "3CQgc6cwhAe4ugkh"
            subscriptionCredential.subscription = "30A24E86-9A18-423F-9939-533AF439CA4F"
            subscriptionCredential.endpoint = ""
            Controller.init(subscriptionCredential) { error ->
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
