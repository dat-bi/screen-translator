package com.vamsi3.android.screentranslator.core.data.model

enum class TranslatorDismissAction {
    NOTHING,
    GO_BACK,
    GO_HOME,
    KILL_APP;

    companion object {
        val default = NOTHING
    }
}
