package com.acuant.sampleapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.support.test.rule.ActivityTestRule
import com.acuant.acuantmobilesdk.Controller
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.ArrayList
import java.util.concurrent.CountDownLatch

/**
 * Created by tapasbehera on 5/4/18.
 */
class TestImageGlare {
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

                val SD_CARD_PATH = Environment.getExternalStorageDirectory().toString() + "/GlareTestImages/"
                val files = getListFiles(File(SD_CARD_PATH))

                for (i in files.indices) {
                    val imageName = files.get(i).getName()
                    if (!imageName.contains("glare_")) {
                        continue
                    }
                    val photoPath = SD_CARD_PATH + imageName
                    val options = BitmapFactory.Options()
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    val bitmap = BitmapFactory.decodeFile(photoPath, options)

                    val croppingOptions = CroppingOptions()
                    croppingOptions.imageMetricsRequired = true
                    croppingOptions.isHealthInsuranceCard = false

                    val croppingData = CroppingData()
                    croppingData.image = bitmap

                    val image : Image = sdk.crop(croppingOptions,croppingData)
                    val glare : Double = image.glareGrade.toDouble()
                    val actualGlare = java.lang.Float.parseFloat(imageName.split("_".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[2].replace(".jpg", ""))
                    TestCase.assertEquals(glare, actualGlare.toDouble(), 0.001)
                }

                sdk?.cleanup()
                signal.countDown()

            }
            signal.await()
            mActivityRule.finishActivity()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    private fun getListFiles(parentDir: File): List<File> {
        val inFiles = ArrayList<File>()
        val files = parentDir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                inFiles.addAll(getListFiles(file))
            } else {
                if (file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png")) {
                    inFiles.add(file)
                }
            }
        }
        return inFiles
    }
}
