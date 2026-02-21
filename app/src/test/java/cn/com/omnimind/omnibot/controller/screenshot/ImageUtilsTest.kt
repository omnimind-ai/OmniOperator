package cn.com.omnimind.omnibot.controller.screenshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageUtilsTest {
    @Test
    fun `setJpegQuality should clamp to valid range`() {
        ImageUtils.setJpegQuality(-1)
        assertEquals(1, getPrivateInt("jpegQuality"))

        ImageUtils.setJpegQuality(101)
        assertEquals(100, getPrivateInt("jpegQuality"))
    }

    @Test
    fun `bitmapToJpegBase64 should apply resize config`() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        ImageUtils.setJpegQuality(80)
        ImageUtils.setResizeConfig(enabled = true, scalePercent = 50)

        val base64 = ImageUtils.bitmapToJpegBase64(source)
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        assertNotNull(decoded)
        assertEquals(5, decoded.width)
        assertEquals(5, decoded.height)
    }

    private fun getPrivateInt(name: String): Int {
        val field = ImageUtils::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.getInt(null)
    }
}

