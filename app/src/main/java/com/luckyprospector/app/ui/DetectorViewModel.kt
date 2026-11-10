package com.luckyprospector.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luckyprospector.app.inference.Detector
import com.luckyprospector.app.inference.FakeDetector
import com.luckyprospector.app.inference.ModelBundleLoader
import java.io.Closeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetectorState(val detector: Detector, val modelUnavailable: Boolean)

/**
 * Owns the on-device detector.
 *
 * Loading verifies the model hash and builds the interpreter, which is too slow for
 * the main thread. Keeping it here also means rotation or a language switch reuses
 * the loaded model instead of building it again.
 */
class DetectorViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<DetectorState?>(null)

    /** Null while the model is loading. */
    val state: StateFlow<DetectorState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) {
                runCatching { ModelBundleLoader(application).load() }
                    .fold(
                        onSuccess = { DetectorState(it, modelUnavailable = false) },
                        onFailure = { DetectorState(FakeDetector(), modelUnavailable = true) },
                    )
            }
        }
    }

    override fun onCleared() {
        (_state.value?.detector as? Closeable)?.close()
    }
}
