package com.acuant.sampleapp

import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import com.acuant.acuantmobilesdk.Controller

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@LargeTest
class TestValidLicenseKey {

    @get:Rule
    public var mActivityRule = ActivityTestRule(
            TestMainActivity::class.java)


    /**
     * Test With a valid license key and url to expect return true
     */
    @Test
    fun test() {
        try {
            val signal = CountDownLatch(1)
            val licenseKeyCredentials = LicenseKeyCredential()
            licenseKeyCredentials.licenseKey = "71F86FD1E789"
            licenseKeyCredentials.endpoint = "https://cssnwebservices.com"
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
