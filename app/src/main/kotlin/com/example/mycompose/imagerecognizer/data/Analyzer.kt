package com.example.mycompose.imagerecognizer.data

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

/**
 * يستخرج من الصورة:
 *  1) الأرقام/الرموز المكتوبة (من الحلقة أو الوسم) عبر قراءة النصوص.
 *  2) بصمة شكلية تقريبية (تصنيفات ML Kit + ألوان الوسط) لمقارنة الصور.
 */
class Analyzer {

    class Result(val tokens: Set<String>, val features: FloatArray)

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0f).build()
    )
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyze(bitmap: Bitmap): Result {
        val image = InputImage.fromBitmap(bitmap, 0)

        val features = FloatArray(DIM)
        runCatching { labeler.process(image).await() }.getOrNull()?.forEach { l ->
            if (l.index in 0 until LABEL_DIM) features[l.index] = l.confidence
        }
        normalize(features, 0, LABEL_DIM)
        colorHistogram(bitmap, features)

        val text = runCatching { recognizer.process(image).await().text }.getOrDefault("")
        val tokens = Regex("[A-Za-z0-9]+")
            .findAll(normalizeDigits(text))
            .map { it.value.uppercase() }
            .toSet()
        return Result(tokens, features)
    }

    private fun colorHistogram(src: Bitmap, out: FloatArray) {
        val w = src.width
        val h = src.height
        val cw = (w * 0.6f).toInt().coerceAtLeast(1)
        val ch = (h * 0.6f).toInt().coerceAtLeast(1)
        val crop = Bitmap.createBitmap(src, (w * 0.2f).toInt(), (h * 0.2f).toInt(), cw, ch)
        val small = Bitmap.createScaledBitmap(crop, 64, 64, true)
        val px = IntArray(64 * 64)
        small.getPixels(px, 0, 64, 0, 0, 64, 64)
        for (p in px) {
            val r = ((p shr 16) and 0xFF) shr 6
            val g = ((p shr 8) and 0xFF) shr 6
            val b = (p and 0xFF) shr 6
            out[LABEL_DIM + r * 16 + g * 4 + b] += 1f
        }
        normalize(out, LABEL_DIM, DIM)
    }

    companion object {
        const val LABEL_DIM = 512
        const val COLOR_DIM = 64
        const val DIM = LABEL_DIM + COLOR_DIM

        private fun normalize(a: FloatArray, from: Int, to: Int) {
            var s = 0f
            for (i in from until to) s += a[i] * a[i]
            val n = sqrt(s)
            if (n > 0f) for (i in from until to) a[i] /= n
        }

        private fun cosine(a: FloatArray, b: FloatArray, from: Int, to: Int): Float {
            var dot = 0f
            var na = 0f
            var nb = 0f
            for (i in from until to) {
                dot += a[i] * b[i]
                na += a[i] * a[i]
                nb += b[i] * b[i]
            }
            return if (na == 0f || nb == 0f) 0f else dot / (sqrt(na) * sqrt(nb))
        }

        fun similarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != DIM || b.size != DIM) return 0f
            return 0.6f * cosine(a, b, 0, LABEL_DIM) + 0.4f * cosine(a, b, LABEL_DIM, DIM)
        }

        fun normalizeDigits(s: String): String = buildString {
            s.forEach { c ->
                append(
                    when (c) {
                        in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                        in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                        else -> c
                    }
                )
            }
        }
    }
}
