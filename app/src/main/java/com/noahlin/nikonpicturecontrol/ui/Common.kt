package com.noahlin.nikonpicturecontrol.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.imageModel
import com.noahlin.nikonpicturecontrol.recipeAssetsDir
import java.io.File

/** Sample image loaded off-thread by Coil, with a gradient placeholder fallback. */
@Composable
fun SampleImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val ctx = LocalContext.current
    if (model == null) {
        Placeholder(modifier)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(ctx).data(model).crossfade(true).build(),
        contentDescription = null,
        contentScale = contentScale,
        loading = { Placeholder(Modifier.fillMaxSize()) },
        error = { Placeholder(Modifier.fillMaxSize()) },
        modifier = modifier,
    )
}

@Composable
private fun Placeholder(modifier: Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Camera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

/** Category pill: known bundled categories get stable colors; others a deterministic fallback. */
@Composable
fun CategoryChip(category: String) {
    val fg = categoryColor(category, isSystemInDarkTheme())
    Text(
        text = category,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .border(0.75.dp, fg.copy(alpha = 0.24f), RoundedCornerShape(50))
            .background(fg.copy(alpha = 0.13f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private val categoryPalette = mapOf(
    "third party creator" to Triple(0.09, 0.35, 0.70),
    "nikon creators" to Triple(0.04, 0.42, 0.31),
    "nikon color grading" to Triple(0.04, 0.43, 0.58),
    "color grading" to Triple(0.04, 0.43, 0.58),
)

private val categoryFallback = listOf(
    Triple(0.65, 0.18, 0.30),
    Triple(0.31, 0.37, 0.09),
    Triple(0.12, 0.42, 0.50),
    Triple(0.53, 0.26, 0.12),
    Triple(0.38, 0.29, 0.62),
)

private fun categoryColor(category: String, dark: Boolean): Color {
    val key = category.trim().lowercase()
    val rgb = categoryPalette[key] ?: categoryFallback[key.sumOf { it.code } % categoryFallback.size]
    // Base RGB is tuned for light mode; brighten on dark backgrounds so text stays readable.
    val lift = if (dark) 0.42 else 0.0
    return Color(
        (rgb.first + lift).coerceAtMost(1.0).toFloat(),
        (rgb.second + lift).coerceAtMost(1.0).toFloat(),
        (rgb.third + lift).coerceAtMost(1.0).toFloat(),
    )
}

/** Read-only dropdown selector with a "none" entry that maps to null. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LabeledDropdown(
    label: String,
    noneLabel: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.ExposedDropdownMenuBox(expanded, { expanded = it }) {
        androidx.compose.material3.OutlinedTextField(
            value = selected ?: noneLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(noneLabel) },
                onClick = { onSelect(null); expanded = false },
            )
            options.forEach { opt ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}

/**
 * Share a recipe's `.NP3` via the system share sheet. Custom files are shared straight from the
 * assets dir; bundled ones are staged into cache first (assets have no shareable file path).
 */
fun shareNp3(context: Context, recipe: Recipe) {
    val name = recipe.np3 ?: return
    val file: File = File(context.recipeAssetsDir(), name).takeIf { it.exists() } ?: run {
        val dest = File(context.cacheDir, "shared").apply { mkdirs() }
            .let { File(it, name.substringAfterLast('/')) }
        context.assets.open("Bundled/$name").use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        dest
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Save / Share .NP3 File"))
}
