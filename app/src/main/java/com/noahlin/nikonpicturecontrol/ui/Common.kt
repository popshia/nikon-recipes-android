package com.noahlin.nikonpicturecontrol.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.thumbModel
import com.noahlin.nikonpicturecontrol.imageModel
import com.noahlin.nikonpicturecontrol.np3Model
import com.noahlin.nikonpicturecontrol.recipeAssetsDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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

/**
 * Category pill, colored from the theme's own accent roles (primary/secondary/tertiary/error)
 * instead of hand-picked RGB — stays correct and contrast-safe under any seed color, dynamic
 * color included, in both light and dark. Known bundled categories get a stable assigned role;
 * others fall back to a deterministic hash so the same name always gets the same color.
 */
@Composable
fun CategoryChip(category: String) {
    val fg = categoryColor(category, MaterialTheme.colorScheme)
    Text(
        text = category,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .border(0.75.dp, fg.copy(alpha = 0.24f), RoundedCornerShape(50))
            .background(fg.copy(alpha = 0.13f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

private val categoryRoles: Map<String, ColorScheme.() -> Color> = mapOf(
    "third party creator" to { secondary },
    "nikon creators" to { tertiary },
    "nikon color grading" to { primary },
    "color grading" to { primary },
)

private val categoryRoleFallback: List<ColorScheme.() -> Color> = listOf(
    { primary }, { secondary }, { tertiary }, { error },
)

private fun categoryColor(category: String, scheme: ColorScheme): Color {
    val key = category.trim().lowercase()
    val role = categoryRoles[key] ?: categoryRoleFallback[key.sumOf { it.code } % categoryRoleFallback.size]
    return scheme.role()
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

/** Full-width recipe card: hero image + name/category + author/tags. Shared by Library and Random. */
@Composable
fun RecipeCard(recipe: Recipe, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        SampleImage(
            recipe.thumbModel(ctx),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                recipe.category?.takeIf { it.isNotEmpty() }?.let {
                    Spacer(Modifier.size(8.dp)); CategoryChip(it)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                recipe.author?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                } ?: Spacer(Modifier.weight(1f))
                if (recipe.tags.isNotEmpty()) {
                    Text(recipe.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Compact recipe row: small thumbnail + name/author. Shared by Library, Search, Browse. */
@Composable
fun RecipeRow(recipe: Recipe, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SampleImage(
            recipe.thumbModel(ctx),
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column {
            Text(recipe.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            recipe.author?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Small image card for the Search browse rails: thumbnail + name + author. */
@Composable
fun RailCard(recipe: Recipe, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.width(160.dp).clickable(onClick = onClick)) {
        SampleImage(
            recipe.thumbModel(ctx),
            modifier = Modifier.width(160.dp).height(115.dp).clip(RoundedCornerShape(12.dp)),
        )
        Text(recipe.name, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp))
        recipe.author?.takeIf { it.isNotEmpty() }?.let {
            Text(it, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Share a recipe's `.NP3` via the system share sheet. A custom recipe's file is shared straight
 * from internal storage; a cloud recipe's file is downloaded from R2 into cache once (keeping its
 * real filename so the share sheet names it right), then shared. Suspends for the download.
 */
suspend fun shareNp3(context: Context, recipe: Recipe) {
    val name = recipe.np3 ?: return
    val custom = File(context.recipeAssetsDir(), name).takeIf { it.exists() }
    val file: File = custom ?: withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "NP3/${recipe.id}").apply { mkdirs() }
        val dest = File(dir, name.substringAfterLast('/'))
        if (!dest.exists()) {
            val url = recipe.np3Model(context) as? String ?: return@withContext null
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 20_000
            }
            try {
                if (conn.responseCode >= 400) return@withContext null
                conn.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
            } catch (e: Exception) {
                return@withContext null
            } finally {
                conn.disconnect()
            }
        }
        dest
    } ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Save / Share .NP3 File"))
}
