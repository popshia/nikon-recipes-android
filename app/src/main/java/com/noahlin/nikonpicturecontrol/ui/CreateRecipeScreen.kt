package com.noahlin.nikonpicturecontrol.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.Recipe
import com.noahlin.nikonpicturecontrol.RecipeStore
import com.noahlin.nikonpicturecontrol.imageModel
import com.noahlin.nikonpicturecontrol.nilIfBlank
import com.noahlin.nikonpicturecontrol.recipeAssetsDir
import java.io.File
import java.util.UUID

/** A sample image being edited: an existing stored path, or a freshly picked Uri. */
private sealed interface ImageSource {
    data class Existing(val name: String) : ImageSource
    data class Picked(val uri: Uri) : ImageSource

    fun bytes(ctx: Context): ByteArray? = when (this) {
        is Picked -> ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        is Existing -> {
            val custom = File(ctx.recipeAssetsDir(), name)
            if (custom.exists()) custom.readBytes()
            else runCatching { ctx.assets.open("Bundled/$name").use { it.readBytes() } }.getOrNull()
        }
    }

    fun model(ctx: Context): Any? = when (this) {
        is Picked -> uri
        is Existing -> ctx.imageModel(name)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(editId: String?, store: RecipeStore, nav: NavController) {
    val ctx = LocalContext.current
    val existing = editId?.let { store.recipe(it) }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var author by remember { mutableStateOf(existing?.author ?: "") }
    var category by remember { mutableStateOf(existing?.category) }
    val selectedTags = remember { mutableStateListOf<String>().apply { existing?.let { addAll(it.tags) } } }
    var about by remember { mutableStateOf(existing?.description ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var whiteBalance by remember { mutableStateOf(existing?.whiteBalance ?: "") }
    var whiteBalanceShift by remember { mutableStateOf(existing?.whiteBalanceShift ?: "") }
    var activeDLighting by remember { mutableStateOf(existing?.activeDLighting ?: "") }
    var evComp by remember { mutableStateOf(existing?.evComp ?: "") }
    var grain by remember { mutableStateOf(existing?.grain ?: "") }

    val images = remember {
        mutableStateListOf<ImageSource>().apply {
            existing?.images?.forEach { add(ImageSource.Existing(it)) }
        }
    }
    var np3Uri by remember { mutableStateOf<Uri?>(null) }
    var np3Name by remember {
        mutableStateOf(existing?.np3?.let { n -> n.substringAfterLast('/').removePrefix("${existing.id}-") })
    }
    var showTagPicker by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris ->
        images.clear()
        uris.forEach { images.add(ImageSource.Picked(it)) }
    }
    val np3Picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { np3Uri = uri; np3Name = ctx.displayName(uri) }
    }

    fun save() {
        val id = existing?.id ?: UUID.randomUUID().toString()
        val dir = ctx.recipeAssetsDir()
        // Clear this recipe's old flat files (no-op for bundled asset paths).
        existing?.let { e ->
            ((e.images ?: emptyList()) + listOfNotNull(e.np3)).forEach { File(dir, it).delete() }
        }
        val imageNames = images.mapIndexedNotNull { i, src ->
            src.bytes(ctx)?.let { bytes ->
                val fname = "$id-${i + 1}.jpg"
                File(dir, fname).writeBytes(bytes); fname
            }
        }
        val savedNp3 = when {
            np3Uri != null -> {
                val fname = "$id-${np3Name ?: "recipe.NP3"}"
                ctx.contentResolver.openInputStream(np3Uri!!)?.use { input ->
                    File(dir, fname).outputStream().use { input.copyTo(it) }
                }
                fname
            }
            existing?.np3 != null && np3Name != null -> existing.np3
            else -> null
        }
        val recipe = Recipe(
            id = id,
            name = name.trim(),
            slug = existing?.slug ?: id,
            category = category,
            tags = selectedTags.sorted(),
            description = about.nilIfBlank(),
            author = author.nilIfBlank(),
            authorLink = existing?.authorLink,
            whiteBalance = whiteBalance.nilIfBlank(),
            whiteBalanceShift = whiteBalanceShift.nilIfBlank(),
            activeDLighting = activeDLighting.nilIfBlank(),
            evComp = evComp.nilIfBlank(),
            grain = grain.nilIfBlank(),
            note = note.nilIfBlank(),
            np3 = savedNp3,
            images = imageNames.takeIf { it.isNotEmpty() },
            recipeLink = existing?.recipeLink,
        )
        if (existing != null) store.update(recipe) else store.addCustom(recipe)
        nav.popBackStack()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Recipe" else "Edit Recipe") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { save() }, enabled = name.trim().isNotEmpty()) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormHeader("Sample Images")
            if (images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(images) { src ->
                        SampleImage(src.model(ctx), Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)))
                    }
                }
            }
            OutlinedButton(onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (images.isEmpty()) "Add Images" else "Change Images (${images.size})")
            }

            FormHeader("NP3 File")
            OutlinedButton(onClick = { np3Picker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(np3Name ?: "Choose .NP3 File")
            }

            FormHeader("Details")
            OutlinedTextField(name, { name = it }, label = { Text("Name") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(author, { author = it }, label = { Text("Author") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            LabeledDropdown("Category", "None", store.categories, category) { category = it }
            Row(
                Modifier.fillMaxWidth().clickable { showTagPicker = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tags", Modifier.weight(1f))
                Text(if (selectedTags.isEmpty()) "None" else selectedTags.sorted().joinToString(", "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            FormHeader("About")
            OutlinedTextField(about, { about = it }, label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp))

            FormHeader("Recommended Settings")
            OutlinedTextField(whiteBalance, { whiteBalance = it }, label = { Text("White Balance") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(whiteBalanceShift, { whiteBalanceShift = it }, label = { Text("WB Shift") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(activeDLighting, { activeDLighting = it }, label = { Text("Active D-Lighting") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(evComp, { evComp = it }, label = { Text("Exposure Comp.") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(grain, { grain = it }, label = { Text("Grain") },
                singleLine = true, modifier = Modifier.fillMaxWidth())

            FormHeader("Notes")
            OutlinedTextField(note, { note = it }, label = { Text("Add a note…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp))
        }
    }

    if (showTagPicker) {
        AlertDialog(
            onDismissRequest = { showTagPicker = false },
            title = { Text("Tags") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    store.tags.forEach { tag ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (tag in selectedTags) selectedTags.remove(tag) else selectedTags.add(tag)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = tag in selectedTags, onCheckedChange = {
                                if (tag in selectedTags) selectedTags.remove(tag) else selectedTags.add(tag)
                            })
                            Text(tag)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTagPicker = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun FormHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary)
}

private fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "recipe.NP3"
}
