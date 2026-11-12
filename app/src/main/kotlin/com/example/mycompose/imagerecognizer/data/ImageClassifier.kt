package com.example.mycompose.imagerecognizer.data

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

/** غلاف بسيط حول ML Kit Image Labeling. لاستبداله بنموذج TFLite مخصص عدّل هذا الملف فقط. */
class ImageClassifier {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
    )

    fun classify(
        image: InputImage,
        onSuccess: (List<Prediction>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        labeler.process(image)
            .addOnSuccessListener { labels ->
                onSuccess(
                    labels.map { Prediction(it.text, it.confidence) }
                        .sortedByDescending { it.confidence }
                )
            }
            .addOnFailureListener { onError(it) }
    }

    fun close() = labeler.close()
}
