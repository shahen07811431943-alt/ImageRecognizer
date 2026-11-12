package com.example.mycompose.imagerecognizer.data

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** حفظ القطيع محلياً: ملف JSON + مجلد صور داخل تخزين التطبيق. */
class AnimalRepository(context: Context) {

    private val file = File(context.filesDir, "animals.json")
    private val photosDir = File(context.filesDir, "photos").apply { mkdirs() }

    fun photoFile(name: String) = File(photosDir, name)

    fun load(): List<Animal> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONObject(file.readText()).getJSONArray("animals")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val photos = o.getJSONArray("photos")
                val feats = o.getJSONArray("features")
                Animal(
                    id = o.getString("id"),
                    number = o.getString("number"),
                    name = o.optString("name", ""),
                    type = o.optString("type", "نعجة"),
                    photos = (0 until photos.length()).map { photos.getString(it) },
                    features = (0 until feats.length()).map { j ->
                        val f = feats.getJSONArray(j)
                        FloatArray(f.length()) { k -> f.getDouble(k).toFloat() }
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(list: List<Animal>) {
        val arr = JSONArray()
        list.forEach { a ->
            val feats = JSONArray()
            a.features.forEach { f ->
                val ja = JSONArray()
                f.forEach { v -> ja.put(v.toDouble()) }
                feats.put(ja)
            }
            val photos = JSONArray()
            a.photos.forEach { photos.put(it) }
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("number", a.number)
                    .put("name", a.name)
                    .put("type", a.type)
                    .put("photos", photos)
                    .put("features", feats)
            )
        }
        file.writeText(JSONObject().put("animals", arr).toString())
    }

    fun savePhoto(bitmap: Bitmap): String {
        val name = UUID.randomUUID().toString() + ".jpg"
        FileOutputStream(File(photosDir, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return name
    }

    fun deletePhoto(name: String) {
        File(photosDir, name).delete()
    }
}
