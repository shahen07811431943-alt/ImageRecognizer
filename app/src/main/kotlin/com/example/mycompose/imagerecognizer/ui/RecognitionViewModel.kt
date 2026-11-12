package com.example.mycompose.imagerecognizer.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.mycompose.imagerecognizer.data.ImageClassifier
import com.example.mycompose.imagerecognizer.data.Prediction
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecognitionState(
    val preview: Any? = null,          // Uri أو Bitmap
    val predictions: List<Prediction> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class RecognitionViewModel(app: Application) : AndroidViewModel(app) {

    private val classifier = ImageClassifier()
    private val _state = MutableStateFlow(RecognitionState())
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    fun onImagePicked(uri: Uri?) {
        uri ?: return
        runCatching { InputImage.fromFilePath(getApplication(), uri) }
            .onSuccess { run(uri, it) }
            .onFailure { _state.value = RecognitionState(error = it.message) }
    }

    fun onPhotoTaken(bitmap: Bitmap?) {
        bitmap ?: return
        run(bitmap, InputImage.fromBitmap(bitmap, 0))
    }

    private fun run(preview: Any, image: InputImage) {
        _state.value = RecognitionState(preview = preview, loading = true)
        classifier.classify(
            image,
            onSuccess = { _state.value = RecognitionState(preview = preview, predictions = it) },
            onError = { _state.value = RecognitionState(preview = preview, error = it.message) }
        )
    }

    override fun onCleared() {
        classifier.close()
    }
}
