package cn.com.omnimind.omnibot.controller.screenshot

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class XmlTreeUtilsTest {
    @Test
    fun `sanitizeXmlString should remove illegal XML chars`() {
        val method =
            XmlTreeUtils::class.java.getDeclaredMethod(
                "sanitizeXmlString",
                String::class.java,
            )
        method.isAccessible = true

        val input = "ab\u0001cd\u000Bef\u0000"
        val sanitized = method.invoke(XmlTreeUtils, input) as String

        assertEquals("abcdef", sanitized)
    }

    @Test
    fun `serializeXml should include key node attributes`() {
        val info =
            AccessibilityNodeInfo
                .obtain()
                .apply {
                    text = "hello"
                    contentDescription = "desc"
                    isClickable = true
                    isFocusable = true
                    setBoundsInScreen(Rect(1, 2, 30, 40))
                }
        val node =
            XmlTreeNode(
                id = "0",
                node =
                    AccessibilityNode(
                        info = info,
                        bounds = Rect(1, 2, 30, 40),
                        show = true,
                        interactive = true,
                    ),
                children = emptyList(),
            )

        val xml = XmlTreeUtils.serializeXml(node)

        assertTrue(xml.contains("<node"))
        assertTrue(xml.contains("id=\"0\""))
        assertTrue(xml.contains("text=\"hello\""))
        assertTrue(xml.contains("content-desc=\"desc\""))
        assertTrue(xml.contains("clickable=\"true\""))
        assertTrue(xml.contains("focusable=\"true\""))
        assertTrue(xml.contains("bounds=\"[1,2][30,40]\""))
        assertFalse(xml.contains("long-clickable="))
    }
}
