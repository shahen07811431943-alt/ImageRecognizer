package com.example.mycompose.imagerecognizer.ui

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mycompose.imagerecognizer.data.ANIMAL_TYPES
import com.example.mycompose.imagerecognizer.data.Animal

@Composable
fun HerdScreen(vm: MainViewModel) {
    val animals by vm.animals.collectAsState()
    var dialogOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Animal?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("القطيع (${animals.size})", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { editing = null; dialogOpen = true }) { Text("إضافة حيوان") }
            }
        }
        items(animals, key = { it.id }) { a ->
            AnimalRow(
                vm = vm,
                animal = a,
                subtitle = "${a.photos.size} صورة",
                modifier = Modifier.clickable { editing = a; dialogOpen = true }
            )
        }
    }

    if (dialogOpen) {
        AnimalDialog(vm, editing) { dialogOpen = false }
    }
}

@Composable
fun AnimalDialog(vm: MainViewModel, animal: Animal?, onDismiss: () -> Unit) {
    var number by remember { mutableStateOf(animal?.number ?: "") }
    var name by remember { mutableStateOf(animal?.name ?: "") }
    var type by remember { mutableStateOf(animal?.type ?: ANIMAL_TYPES.first()) }
    val newUris = remember { mutableStateListOf<Uri>() }
    val camera = rememberCameraLauncher { newUris.add(it) }
    val gallery = rememberGalleryLauncher(true) { newUris.addAll(it) }
    val existing = animal?.photos ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (animal == null) "إضافة حيوان" else "تعديل الحيوان") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = number, onValueChange = { number = it },
                    label = { Text("الرقم أو العلامة") }, singleLine = true
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("الاسم (اختياري، يمكن لاحقاً)") }, singleLine = true
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ANIMAL_TYPES.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = camera) { Text("كاميرا") }
                    OutlinedButton(onClick = gallery) { Text("معرض") }
                }
                Text("الصور: ${existing.size + newUris.size}  (الأفضل 3 إلى 5 من زوايا مختلفة)")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    existing.forEach {
                        AsyncImage(vm.photoFile(it), null, Modifier.size(64.dp), contentScale = ContentScale.Crop)
                    }
                    newUris.forEach {
                        AsyncImage(it, null, Modifier.size(64.dp), contentScale = ContentScale.Crop)
                    }
                }
                if (animal != null) {
                    TextButton(
                        onClick = { vm.deleteAnimal(animal.id); onDismiss() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("حذف الحيوان") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = number.isNotBlank(),
                onClick = {
                    vm.saveAnimal(animal?.id, number, name, type, newUris.toList())
                    onDismiss()
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
