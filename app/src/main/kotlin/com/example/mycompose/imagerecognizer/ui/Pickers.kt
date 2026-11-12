package com.example.mycompose.imagerecognizer.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** يعيد دالة تفتح الكاميرا وتلتقط صورة كاملة الدقة. */
@Composable
fun rememberCameraLauncher(onPhoto: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pending
        if (ok && u != null) onPhoto(u)
    }
    return {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pending = uri
        launcher.launch(uri)
    }
}

/** يعيد دالة تفتح المعرض (صورة واحدة أو عدة صور). */
@Composable
fun rememberGalleryLauncher(multiple: Boolean, onPick: (List<Uri>) -> Unit): () -> Unit {
    val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { u ->
        if (u != null) onPick(listOf(u))
    }
    val multi = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { list ->
        if (list.isNotEmpty()) onPick(list)
    }
    return {
        val req = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (multiple) multi.launch(req) else single.launch(req)
    }
}
