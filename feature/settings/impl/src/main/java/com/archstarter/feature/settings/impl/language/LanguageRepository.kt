package com.archstarter.feature.settings.impl.language

import com.archstarter.feature.settings.api.supportedLanguages
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Singleton
class LanguageRepository @Inject constructor() {
    suspend fun loadLanguages(): List<String> = withContext(Dispatchers.Default) {
        // Simulate asynchronous work so heavy filtering does not block the UI thread.
        delay(50)
        supportedLanguages
    }
}
