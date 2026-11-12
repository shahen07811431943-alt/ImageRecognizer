package com.example.mycompose.imagerecognizer.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycompose.imagerecognizer.data.Analyzer
import com.example.mycompose.imagerecognizer.data.Animal
import com.example.mycompose.imagerecognizer.data.AnimalRepository
import com.example.mycompose.imagerecognizer.data.Candidate
import com.example.mycompose.imagerecognizer.data.ImageUtils
import com.example.mycompose.imagerecognizer.data.Matcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class IdentifyState(
    val preview: Bitmap? = null,
    val candidates: List<Candidate> = emptyList(),
    val features: FloatArray? = null,
    val busy: Boolean = false
)

class InventoryState(
    val seen: Set<String> = emptySet(),
    val photos: Int = 0,
    val busy: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val repo = AnimalRepository(ctx)
    private val analyzer = Analyzer()

    private val _animals = MutableStateFlow(repo.load())
    val animals: StateFlow<List<Animal>> = _animals.asStateFlow()

    private val _identify = MutableStateFlow(IdentifyState())
    val identifyState: StateFlow<IdentifyState> = _identify.asStateFlow()

    private val _inventory = MutableStateFlow(InventoryState())
    val inventory: StateFlow<InventoryState> = _inventory.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun photoFile(name: String) = repo.photoFile(name)
    fun clearMessage() { _message.value = null }

    private suspend fun load(uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) { runCatching { ImageUtils.loadBitmap(ctx, uri) }.getOrNull() }

    private suspend fun analyze(bmp: Bitmap): Analyzer.Result =
        withContext(Dispatchers.Default) { analyzer.analyze(bmp) }

    private suspend fun updateAnimals(block: (List<Animal>) -> List<Animal>) {
        _animals.update(block)
        withContext(Dispatchers.IO) { repo.save(_animals.value) }
    }

    // ---------- التعرف ----------
    fun identify(uri: Uri) {
        viewModelScope.launch {
            _identify.value = IdentifyState(busy = true)
            val bmp = load(uri)
            if (bmp == null) {
                _identify.value = IdentifyState()
                _message.value = "تعذّر فتح الصورة"
                return@launch
            }
            val r = analyze(bmp)
            val cands = Matcher.rank(_animals.value, r)
            _identify.value = IdentifyState(preview = bmp, candidates = cands, features = r.features)
            if (_animals.value.isEmpty()) _message.value = "سجّل القطيع أولاً من تبويب القطيع"
        }
    }

    /** تأكيد أن الصورة لهذا الحيوان: تُضاف لصوره (فيتحسن التعرف) ويُسجَّل في الجرد. */
    fun confirm(animalId: String) {
        val st = _identify.value
        val bmp = st.preview ?: return
        val f = st.features ?: return
        viewModelScope.launch {
            val photo = withContext(Dispatchers.IO) { repo.savePhoto(bmp) }
            updateAnimals { list ->
                list.map {
                    if (it.id == animalId) it.copy(photos = it.photos + photo, features = it.features + listOf(f)) else it
                }
            }
            _inventory.update { InventoryState(it.seen + animalId, it.photos, false) }
            _identify.value = IdentifyState(preview = bmp)
            _message.value = "تمت إضافة الصورة وتسجيله في الجرد"
        }
    }

    // ---------- القطيع ----------
    fun saveAnimal(existingId: String?, number: String, name: String, type: String, uris: List<Uri>) {
        viewModelScope.launch {
            val num = number.trim()
            if (_animals.value.any { it.id != existingId && it.number.trim().equals(num, ignoreCase = true) }) {
                _message.value = "هذا الرقم مسجّل لحيوان آخر"
                return@launch
            }
            val newPhotos = mutableListOf<String>()
            val newFeatures = mutableListOf<FloatArray>()
            for (u in uris) {
                val bmp = load(u) ?: continue
                newFeatures.add(analyze(bmp).features)
                newPhotos.add(withContext(Dispatchers.IO) { repo.savePhoto(bmp) })
            }
            if (existingId == null) {
                val a = Animal(UUID.randomUUID().toString(), num, name.trim(), type, newPhotos, newFeatures)
                updateAnimals { it + a }
            } else {
                updateAnimals { list ->
                    list.map {
                        if (it.id == existingId) it.copy(
                            number = num, name = name.trim(), type = type,
                            photos = it.photos + newPhotos, features = it.features + newFeatures
                        ) else it
                    }
                }
            }
            _message.value = "تم الحفظ"
        }
    }

    fun deleteAnimal(id: String) {
        viewModelScope.launch {
            val a = _animals.value.firstOrNull { it.id == id } ?: return@launch
            withContext(Dispatchers.IO) { a.photos.forEach { repo.deletePhoto(it) } }
            updateAnimals { list -> list.filter { it.id != id } }
            _inventory.update { InventoryState(it.seen - id, it.photos, false) }
        }
    }

    // ---------- الجرد ----------
    fun scanInventory(uris: List<Uri>) {
        viewModelScope.launch {
            _inventory.update { InventoryState(it.seen, it.photos, true) }
            var found = 0
            var seen = _inventory.value.seen
            for (u in uris) {
                val bmp = load(u) ?: continue
                val r = analyze(bmp)
                val matched = _animals.value.filter { it.number.trim().uppercase() in r.tokens }
                found += matched.count { it.id !in seen }
                seen = seen + matched.map { it.id }
            }
            _inventory.value = InventoryState(seen, _inventory.value.photos + uris.size, false)
            _message.value = if (found > 0) "تم رصد $found حيوان من الأرقام" else "لم أقرأ أي رقم مسجّل في الصور"
        }
    }

    fun toggleSeen(id: String) {
        _inventory.update {
            InventoryState(if (id in it.seen) it.seen - id else it.seen + id, it.photos, it.busy)
        }
    }

    fun resetInventory() {
        _inventory.value = InventoryState()
    }
}
