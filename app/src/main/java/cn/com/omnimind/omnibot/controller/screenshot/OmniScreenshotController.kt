package cn.com.omnimind.omnibot.controller.screenshot

import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Xml
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import cn.com.omnimind.omnibot.OmniOperatorService
import cn.com.omnimind.omnibot.api.CaptureScreenshotImageData
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.util.concurrent.Executor
import kotlin.collections.iterator
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OmniScreenshotController(
    private val service: OmniOperatorService,
) {
    companion object {
        private const val TAG = "OmniScreenshotController"
        private val screenshotMutex = Mutex()
    }

    private var currentPackageName: String = "unknown"
    private var currentActivityName: String = "unknown"

    fun getCurrentPackageName(): String {
        return currentPackageName
    }

    fun getCurrentActivityName(): String {
        return currentActivityName
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentPackageName = event.packageName?.toString() ?: "unknown"
            currentActivityName = event.className?.toString() ?: "unknown"
        }
    }

    private val mainThreadExecutor: Executor =
        Executor { command ->
            Handler(Looper.getMainLooper()).post(command)
        }

    // TODO: Maybe support lower SDK versions? When takeScreenshot is not available, fallback to other methods?
    suspend fun captureScreenshotImage(): CaptureScreenshotImageData {
        screenshotMutex.lock()
        try {
            val result =
                suspendCancellableCoroutine { cont ->
                    service.takeScreenshot(
                        Display.DEFAULT_DISPLAY,
                        mainThreadExecutor,
                        object : TakeScreenshotCallback {
                            override fun onSuccess(screenshot: ScreenshotResult) {
                                screenshot.hardwareBuffer.use { hardwareBuffer ->
                                    try {
                                        val bitmap =
                                            Bitmap.wrapHardwareBuffer(
                                                hardwareBuffer,
                                                screenshot.colorSpace,
                                            )
                                                ?: throw RuntimeException("Failed to wrap hardware buffer into Bitmap")

                                        val base64 = ImageUtils.bitmapToJpegBase64(bitmap)

                                        cont.resume(
                                            CaptureScreenshotImageData(
                                                imageBase64 = "data:image/jpeg;base64,$base64",
                                            ),
                                        )
                                    } catch (e: Exception) {
                                        cont.resumeWithException(e)
                                    }
                                }
                            }

                            override fun onFailure(errorCode: Int) {
                                cont.resumeWithException(RuntimeException("Screenshot failed with error code: $errorCode"))
                            }
                        },
                    )
                }
            // Throttle: prevent rapid-fire screenshot requests
            delay(300)
            return result
        } finally {
            screenshotMutex.unlock()
        }
    }

    fun captureScreenshotXml(): String? {
        val rootNode = service.rootInActiveWindow ?: return null
        val xmlTree = XmlTreeUtils.buildXmlTree(rootNode) ?: return null
        return XmlTreeUtils.serializeXml(xmlTree)
    }

    fun getNodeMap(): Map<String, AccessibilityNode>? {
        val rootNode = service.rootInActiveWindow ?: return null
        val xmlTree = XmlTreeUtils.buildXmlTree(rootNode) ?: return null
        return XmlTreeUtils.extractNodeMap(xmlTree)
    }
}

data class AccessibilityNode(
    val info: AccessibilityNodeInfo,
    val bounds: Rect,
    val show: Boolean,
    val interactive: Boolean,
)

data class XmlTreeNode(
    val id: String,
    val node: AccessibilityNode,
    val children: List<XmlTreeNode>,
)

object XmlTreeUtils {
    fun buildXmlTree(root: AccessibilityNodeInfo?): XmlTreeNode? = buildRecursive(root, 0).first

    // TODO: nodeId allocation algorithm is not optimal
    private fun buildRecursive(
        node: AccessibilityNodeInfo?,
        currentId: Int,
    ): Pair<XmlTreeNode?, Int> {
        if (node == null || (!node.isVisibleToUser && currentId != 0)) {
            return null to currentId
        }

        val nodeId = currentId.toString()
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val hasText = !node.text.isNullOrEmpty()
        val interactive =
            node.isClickable ||
                node.isLongClickable ||
                node.isFocusable ||
                node.isFocused ||
                node.isScrollable ||
                node.isPassword ||
                node.isSelected ||
                node.isEditable
        val show =
            hasText ||
                interactive ||
                currentId == 0 // Always show root node

        var nextId = currentId + 1
        val children = mutableListOf<XmlTreeNode>()
        for (i in 0 until node.childCount) {
            val (childTree, newId) = buildRecursive(node.getChild(i), nextId)
            if (childTree != null) {
                children.add(childTree)
                nextId = newId
            }
        }

        return XmlTreeNode(
            id = nodeId,
            node =
                AccessibilityNode(
                    info = node,
                    bounds = bounds,
                    show = show,
                    interactive = interactive,
                ),
            children = children,
        ) to nextId
    }

    fun extractNodeMap(tree: XmlTreeNode): Map<String, AccessibilityNode> {
        val map = mutableMapOf<String, AccessibilityNode>()

        fun dfs(node: XmlTreeNode) {
            map[node.id] = node.node
            node.children.forEach(::dfs)
        }
        dfs(tree)
        return map
    }

    private fun sanitizeXmlString(text: String?): String? {
        if (text == null) return null
        // This regex matches any character that is NOT a valid XML 1.0 character.
        val illegalXmlCharRegex = "[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]"
        return text.replace(Regex(illegalXmlCharRegex), "")
    }

    fun serializeXml(tree: XmlTreeNode): String {
        val writer = StringWriter()
        val namespace = "http://schemas.android.com/apk/res/android"
        val serializer =
            Xml.newSerializer().apply {
                setOutput(writer)
                startDocument("UTF-8", true)
                setPrefix("", namespace)
                startTag(namespace, "hierarchy")
            }

        fun addAttr(
            name: String,
            value: String?,
        ) {
            if (!value.isNullOrEmpty() && value != "false") {
                serializer.attribute(null, name, value)
            }
        }

        fun serializeNode(node: XmlTreeNode) {
            if (node.node.show) {
                val n = node.node.info
                val bounds = node.node.bounds
                serializer.startTag(null, "node")
                serializer.attribute(null, "id", node.id)
                // Sanitize text-based attributes before adding them
                addAttr("text", sanitizeXmlString(n.text?.toString()))
                addAttr("content-desc", sanitizeXmlString(n.contentDescription?.toString()))
                addAttr("clickable", n.isClickable.toString())
                addAttr("long-clickable", n.isLongClickable.toString())
                addAttr("focusable", n.isFocusable.toString())
                addAttr("focused", n.isFocused.toString())
                addAttr("scrollable", n.isScrollable.toString())
                addAttr("password", n.isPassword.toString())
                addAttr("selected", n.isSelected.toString())
                addAttr("editable", n.isEditable.toString())
                serializer.attribute(
                    null,
                    "bounds",
                    "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]",
                )
                node.children.forEach { serializeNode(it) }
                serializer.endTag(null, "node")
            } else {
                node.children.forEach { serializeNode(it) }
            }
        }

        serializeNode(tree)

        serializer.endTag(namespace, "hierarchy")
        serializer.endDocument()
        return writer.toString()
    }
}

object ImageUtils {
    @Volatile
    private var jpegQuality: Int = 50

    fun setJpegQuality(quality: Int) {
        jpegQuality = quality.coerceIn(1, 100)
    }

    fun bitmapToJpegBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
