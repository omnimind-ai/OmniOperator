package cn.com.omnimind.omnibot.controller.overlay.dynamicisland.model

/**
 * Represents a clickable action in the Dynamic Island dialogue.
 * @param id A unique identifier for the action, returned when the user clicks it.
 * @param text The text to display for the action link.
 */
data class DialogueAction(val id: String, val text: String)
