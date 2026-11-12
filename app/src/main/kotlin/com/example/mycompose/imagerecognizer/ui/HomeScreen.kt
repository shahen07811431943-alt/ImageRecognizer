package com.example.mycompose.imagerecognizer.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun HomeScreen(modifier: Modifier = Modifier, vm: RecognitionViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { vm.onImagePicked(it) }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { vm.onPhotoTaken(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("التعرف على الصور", style = MaterialTheme.typography.headlineMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("من المعرض") }
            Button(onClick = { camera.launch(null) }) { Text("الكاميرا") }
        }

        state.preview?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
            )
        }

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text("خطأ: ${state.error}", color = MaterialTheme.colorScheme.error)
            state.preview != null && state.predictions.isEmpty() -> Text("لم يتم التعرف على شيء")
            else -> state.predictions.forEach { p ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(p.label, style = MaterialTheme.typography.titleMedium)
                        Text("${(p.confidence * 100).toInt()}%")
                    }
                }
            }
        }
    }
}
