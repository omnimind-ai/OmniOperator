package cn.com.omnimind.omnibot.api

// A generic, reusable result wrapper for all operations.
data class BaseOperatorResult<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
)

// --- Type Aliases for specific operation results ---

// Node and Coordinate Actions
typealias ClickNodeResult = BaseOperatorResult<Unit>
typealias LongClickNodeResult = BaseOperatorResult<Unit>
typealias ScrollNodeResult = BaseOperatorResult<Unit>
typealias InputTextResult = BaseOperatorResult<Unit>
typealias InputTextToFocusedNodeResult = BaseOperatorResult<Unit>
typealias CopyToClipboardResult = BaseOperatorResult<Unit>
typealias InjectTextByIMEResult = BaseOperatorResult<Unit>
typealias ClickCoordinateResult = BaseOperatorResult<Unit>
typealias LongClickCoordinateResult = BaseOperatorResult<Unit>
typealias ScrollCoordinateResult = BaseOperatorResult<Unit>

// User Interaction
typealias RequireUserConfirmationResult = BaseOperatorResult<String?>
typealias RequireUserChoiceResult = BaseOperatorResult<String?>

// Global Actions
typealias LaunchApplicationResult = BaseOperatorResult<Unit>
typealias GoHomeResult = BaseOperatorResult<Unit>
typealias GoBackResult = BaseOperatorResult<Unit>
typealias ShowMessageResult = BaseOperatorResult<Unit>
typealias PushMessageToBotResult = BaseOperatorResult<Unit>

// Screenshot Data Payloads
data class CaptureScreenshotImageData(
    val imageBase64: String?,
)
typealias CaptureScreenshotImageResult = BaseOperatorResult<CaptureScreenshotImageData>

data class CaptureScreenshotXmlData(
    val xml: String?,
)
typealias CaptureScreenshotXmlResult = BaseOperatorResult<CaptureScreenshotXmlData>

data class GetMetadataData(
    val packageName: String?,
    val activityName: String?,
)
typealias GetMetadataResult = BaseOperatorResult<GetMetadataData>

// Application List Data Payload
data class ListInstalledApplicationsData(
    val packageNames: List<String> = emptyList(),
    val applicationNames: List<String> = emptyList(),
)
typealias ListInstalledApplicationsResult = BaseOperatorResult<ListInstalledApplicationsData>

// --- Enums ---

enum class NodeScrollDirection {
    FORWARD,
    BACKWARD,
}

enum class CoordinateScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}
