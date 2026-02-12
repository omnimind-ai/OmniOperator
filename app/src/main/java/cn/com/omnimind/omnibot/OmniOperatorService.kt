@file:Suppress("ktlint")
package cn.com.omnimind.omnibot

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import cn.com.omnimind.omnibot.api.*
import cn.com.omnimind.omnibot.controller.action.OmniActionController
import cn.com.omnimind.omnibot.controller.overlay.OmniOverlayController
import cn.com.omnimind.omnibot.controller.overlay.dynamicisland.model.DialogueAction
import cn.com.omnimind.omnibot.controller.screenshot.OmniScreenshotController
import cn.com.omnimind.omnibot.util.OmniLog
import cn.com.omnimind.omnibot.util.SocketHandler
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout


class OmniOperatorService : AccessibilityService() {
    private val screenshotController = OmniScreenshotController(this)
    private val actionController = OmniActionController(this)
    private val overlayController = OmniOverlayController(this)
    companion object {
        private const val TAG = "OmniOperatorService"
        private const val SERVICE_NOT_RUNNING = "Accessibility service is not running."
        private var instance: OmniOperatorService? = null

        private fun requireInstance(): OmniOperatorService =
            instance ?: throw IllegalStateException(
                "OmniOperatorService is not running. Please enable accessibility service."
            )

        private suspend fun <T> requireService(
            block: suspend OmniOperatorService.() -> BaseOperatorResult<T>,
        ): BaseOperatorResult<T> {
            val service = try {
                requireInstance()
            } catch (_: IllegalStateException) {
                return BaseOperatorResult(success = false, message = SERVICE_NOT_RUNNING)
            }
            return service.block()
        }

        suspend fun clickNode(nodeId: String) = requireService { clickNode(nodeId) }
        suspend fun longClickNode(nodeId: String) = requireService { longClickNode(nodeId) }
        suspend fun scrollNode(nodeId: String, direction: String) = requireService { scrollNode(nodeId, direction) }
        suspend fun inputText(nodeId: String, text: String) = requireService { inputText(nodeId, text) }
        suspend fun inputTextToFocusedNode(text: String) = requireService { inputTextToFocusedNode(text) }
        suspend fun copyToClipboard(text: String) = requireService { copyToClipboard(text) }
        suspend fun injectTextByIME(text: String) = requireService { injectTextByIME(text) }
        suspend fun clickCoordinate(x: Float, y: Float) = requireService { clickCoordinate(x, y) }
        suspend fun longClickCoordinate(x: Float, y: Float) = requireService { longClickCoordinate(x, y) }
        suspend fun scrollCoordinate(x: Float, y: Float, direction: String, distance: Float) = requireService { scrollCoordinate(x, y, direction, distance) }
        suspend fun goHome() = requireService { goHome() }
        suspend fun goBack() = requireService { goBack() }
        suspend fun captureScreenshotImage() = requireService { captureScreenshotImage() }
        suspend fun captureScreenshotXml() = requireService { captureScreenshotXml() }
        suspend fun getMetadata() = requireService { getMetadata() }
        suspend fun launchApplication(packageName: String) = requireService { launchApplication(packageName) }
        suspend fun requireUserConfirmation(prompt: String) = requireService { requireUserConfirmation(prompt) }
        suspend fun requireUserChoice(prompt: String, options: List<String>) = requireService { requireUserChoice(prompt, options) }
        suspend fun listInstalledApplications() = requireService { listInstalledApplications() }
        suspend fun showMessage(title: String, content: String) = requireService { showMessage(title, content) }
        suspend fun pushMessageToBot(message: String, suggestionTitle: String?, suggestions: List<String>?) = requireService { pushMessageToBot(message, suggestionTitle, suggestions) }
        fun isAccessibilityServiceEnabled() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlayController.showDynamicIslandMessage(
            title = getString(R.string.omni_service_title),
            content = getString(R.string.omni_service_content),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        screenshotController.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {}

    private suspend fun <T> executeOperation(
        operationName: String,
        block: suspend () -> T,
    ): BaseOperatorResult<T> {
        return try {
            overlayController.dismissDynamicIsland()
            val data = block()
            val successMsg = "$operationName executed successfully."
            OmniLog.d(TAG, successMsg)
            BaseOperatorResult(success = true, message = successMsg, data = data)
        } catch (e: Exception) {
            val errorMsg = "Error during $operationName: ${e.message}"
            OmniLog.e(TAG, errorMsg, e)
            overlayController.showDynamicIslandMessage(
                title = getString(R.string.omni_op_failed_title),
                content = e.localizedMessage ?: getString(R.string.omni_op_failed_content_generic),
            )
            BaseOperatorResult(success = false, message = errorMsg)
        }
    }

    private suspend fun captureScreenshotImage(): CaptureScreenshotImageResult =
        executeOperation("Capture Screenshot Image") {
            withTimeout(1000) { screenshotController.captureScreenshotImage() }
        }

    private suspend fun captureScreenshotXml(): CaptureScreenshotXmlResult =
        executeOperation("Capture Screenshot XML") {
            val xml = screenshotController.captureScreenshotXml()
            CaptureScreenshotXmlData(xml)
        }

    private suspend fun getMetadata(): GetMetadataResult =
        executeOperation("Get Package Name and Activity Name") {
            val packageName = screenshotController.getCurrentPackageName()
            val activityName = screenshotController.getCurrentActivityName()
            GetMetadataData(
                packageName = packageName,
                activityName = activityName
            )
        }

    private suspend fun requireUserConfirmation(prompt: String): RequireUserConfirmationResult =
        executeOperation("Require User Confirmation") {
            val confirmed =
                overlayController.showDynamicIslandDialogue(
                    title = getString(R.string.omni_title_user_confirmation),
                    content = prompt,
                    actions = listOf(
                        DialogueAction(id = "confirm", text = getString(R.string.omni_action_confirm)),
                        DialogueAction(id = "refuse", text = getString(R.string.omni_action_refuse))
                    ),
                ).await()

            val message = if (confirmed?.id == "confirm") {
                getString(R.string.omni_content_user_confirmed, prompt)
            } else {
                getString(R.string.omni_content_user_refused, prompt)
            }
            overlayController.showDynamicIslandMessage(getString(R.string.omni_title_user_confirmation), message)
            confirmed?.id ?: "cancelled"
        }

    private suspend fun requireUserChoice(prompt: String, options: List<String>): RequireUserChoiceResult =
        executeOperation("Require User Choice") {
            val choice =
                overlayController.showDynamicIslandDialogue(
                    title = getString(R.string.omni_title_user_choice),
                    content = prompt,
                    actions = options.map { DialogueAction(id = it, text = it) },
                ).await()

            val message = if (choice != null) {
                getString(R.string.omni_content_user_chose, choice.text)
            } else {
                getString(R.string.omni_content_user_cancelled_choice)
            }
            overlayController.showDynamicIslandMessage(getString(R.string.omni_title_user_choice), message)
            choice?.id ?: "cancelled"
        }

    private suspend fun clickNode(nodeId: String): ClickNodeResult =
        executeOperation("Click Node '$nodeId'") {
            val node = screenshotController.getNodeMap()?.get(nodeId) ?: throw IllegalArgumentException("Node with ID '$nodeId' not found.")
            overlayController.showClickIndicator(node.bounds.centerX().toFloat(), node.bounds.centerY().toFloat())
            actionController.clickNode(node.info)
        }

    private suspend fun longClickNode(nodeId: String): LongClickNodeResult =
        executeOperation("Long Click Node '$nodeId'") {
            val node = screenshotController.getNodeMap()?.get(nodeId) ?: throw IllegalArgumentException("Node with ID '$nodeId' not found.")
            overlayController.showClickIndicator(node.bounds.centerX().toFloat(), node.bounds.centerY().toFloat())
            actionController.longClickNode(node.info)
        }

    private suspend fun scrollNode(nodeId: String, direction: String): ScrollNodeResult =
        executeOperation("Scroll Node '$nodeId'") {
            val node = screenshotController.getNodeMap()?.get(nodeId)?.info ?: throw IllegalArgumentException("Node with ID '$nodeId' not found.")
            val scrollDirection = when (direction.lowercase()) {
                "forward" -> NodeScrollDirection.FORWARD
                "backward" -> NodeScrollDirection.BACKWARD
                else -> throw IllegalArgumentException("Invalid direction: $direction. Use 'forward' or 'backward'.")
            }
            actionController.scrollNode(node, scrollDirection)
        }

    private suspend fun inputText(nodeId: String, text: String): InputTextResult =
        executeOperation("Input Text in Node '$nodeId'") {
            val node = screenshotController.getNodeMap()?.get(nodeId)?.info ?: throw IllegalArgumentException("Node with ID '$nodeId' not found.")
            actionController.inputText(node, text)
        }

    private suspend fun inputTextToFocusedNode(text: String): InputTextToFocusedNodeResult =
        executeOperation("Input Text to Focused Node") {
            val focusedNode = screenshotController.getNodeMap()?.values?.firstOrNull { it.info.isFocused }?.info
                ?: throw IllegalStateException("No focused node found on the screen.")
            actionController.inputText(focusedNode, text)
        }

    private suspend fun copyToClipboard(text: String): CopyToClipboardResult =
        executeOperation("Copy Text to Clipboard") {
            actionController.copyToClipboard(this, text)
        }

    private suspend fun injectTextByIME(text: String): InjectTextByIMEResult =
        executeOperation("Inject Text by Omni IME") {
            if (OmniIME.instance == null) {
                OmniLog.e(TAG, "Omni IME instance is null")
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                throw IllegalStateException("Please use Omni IME.")
            } else {
                actionController.injectTextByIME(text)
            }
        }

    private suspend fun clickCoordinate(x: Float, y: Float): ClickCoordinateResult =
        executeOperation("Click Coordinate ($x, $y)") {
            overlayController.showClickIndicator(x, y)
            withTimeout(1000) { actionController.clickCoordinate(x, y).await() }
        }

    private suspend fun longClickCoordinate(x: Float, y: Float): LongClickCoordinateResult =
        executeOperation("Long Click Coordinate ($x, $y)") {
            overlayController.showClickIndicator(x, y)
            withTimeout(2000) { actionController.longClickCoordinate(x, y).await() }
        }

    private suspend fun scrollCoordinate(x: Float, y: Float, direction: String, distance: Float): ScrollCoordinateResult =
        executeOperation("Scroll Coordinate ($x, $y) $direction") {
            val scrollDirection = when (direction.lowercase()) {
                "up" -> CoordinateScrollDirection.UP
                "down" -> CoordinateScrollDirection.DOWN
                "left" -> CoordinateScrollDirection.LEFT
                "right" -> CoordinateScrollDirection.RIGHT
                else -> throw IllegalArgumentException("Invalid direction: $direction. Use up/down/left/right.")
            }
            overlayController.showScrollIndicator(x, y, scrollDirection, distance)
            withTimeout(1000) { actionController.scrollCoordinate(x, y, scrollDirection, distance).await() }
        }

    private suspend fun goHome(): GoHomeResult =
        executeOperation("Go Home") {
            actionController.goHome()
        }

    private suspend fun goBack(): GoBackResult =
        executeOperation("Go Back") {
            actionController.goBack()
        }

    private suspend fun launchApplication(packageName: String): LaunchApplicationResult =
        executeOperation("Launch Application '$packageName'") {
            actionController.launchApplication(packageName)
        }

    private suspend fun listInstalledApplications(): ListInstalledApplicationsResult =
        executeOperation("List Installed Applications") {
            val (packageNames, applicationNames) = actionController.listInstalledApplications()
            ListInstalledApplicationsData(packageNames, applicationNames)
        }

    private suspend fun showMessage(title: String, content: String): ShowMessageResult =
        executeOperation("Show Message") {
            overlayController.showDynamicIslandMessage(title, content)
        }
        
    private suspend fun pushMessageToBot(message: String, suggestionTitle: String?, suggestions: List<String>?): PushMessageToBotResult =
        executeOperation("Push Message to Bot") {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                MainActivity.botMessageEventSink?.success(
                    mapOf(
                        "message" to message,
                        "suggestionTitle" to (suggestionTitle ?: "No suggestions"),
                        "suggestions" to (suggestions ?: emptyList()),
                    ),
                )
                OmniLog.i(TAG, "Pushed message to Flutter: $message")
            }
        }
}
