package cn.com.omnimind.omnibot

import android.inputmethodservice.InputMethodService

class OmniIME : InputMethodService() {
    companion object {
        private const val TAG = "OmniIME"
        var instance: OmniIME? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    public fun inputText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }
}
