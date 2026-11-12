package com.example.mycompose.imagerecognizer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun IdentifyScreen(vm: MainViewModel) {
    val st by vm.identifyState.collectAsState()
    val animals by vm.animals.collectAsState()
    val camera = rememberCameraLauncher { vm.identify(it) }
    val gallery = rememberGalleryLauncher(false) { vm.identify(it.first()) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تعرّف على الحيوان", style = MaterialTheme.typography.headlineSmall)
        Text(
            "صوّر حيواناً واحداً قريباً، ويفضّل أن يظهر الرقم أو العلامة.",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = camera) { Text("الكاميرا") }
            Button(onClick = gallery) { Text("من المعرض") }
        }
        if (animals.isEmpty()) Text("القطيع فارغ. سجّل الحيوانات من تبويب القطيع.")

        st.preview?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
            )
        }
        if (st.busy) CircularProgressIndicator()

        st.candidates.forEach { c ->
            AnimalRow(
                vm = vm,
                animal = c.animal,
                subtitle = if (c.byNumber) "مطابق بقراءة الرقم" else "تشابه تقريبي ${(c.score * 100).toInt()}%",
                trailing = { Button(onClick = { vm.confirm(c.animal.id) }) { Text("هذا هو") } }
            )
        }
        if (st.preview != null && !st.busy && st.candidates.isEmpty() && animals.isNotEmpty()) {
            Text("لا يوجد تطابق")
        }
    }
}
