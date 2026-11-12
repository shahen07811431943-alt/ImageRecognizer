package com.example.mycompose.imagerecognizer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InventoryScreen(vm: MainViewModel) {
    val animals by vm.animals.collectAsState()
    val inv by vm.inventory.collectAsState()
    val camera = rememberCameraLauncher { vm.scanInventory(listOf(it)) }
    val gallery = rememberGalleryLauncher(true) { vm.scanInventory(it) }

    val missing = animals.filter { it.id !in inv.seen }
    val seen = animals.filter { it.id in inv.seen }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الجرد", style = MaterialTheme.typography.headlineSmall)
                Text("تم رصد ${seen.size} من ${animals.size}   |   الناقص: ${missing.size}")
                Text(
                    "صوّر القطيع (الأرقام الكبيرة الواضحة تُقرأ تلقائياً)، أو اضغط على أي حيوان لتحديده يدوياً.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = camera) { Text("كاميرا") }
                    Button(onClick = gallery) { Text("معرض") }
                    OutlinedButton(onClick = { vm.resetInventory() }) { Text("جرد جديد") }
                }
                if (inv.busy) CircularProgressIndicator()
                Text("لم يُرصد بعد (${missing.size})", style = MaterialTheme.typography.titleMedium)
            }
        }
        items(missing, key = { "m" + it.id }) { a ->
            AnimalRow(vm, a, "اضغط لتحديده كموجود", Modifier.clickable { vm.toggleSeen(a.id) })
        }
        item { Text("تم رصده (${seen.size})", style = MaterialTheme.typography.titleMedium) }
        items(seen, key = { "s" + it.id }) { a ->
            AnimalRow(vm, a, "موجود، اضغط للإلغاء", Modifier.clickable { vm.toggleSeen(a.id) })
        }
    }
}
