package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.noahlin.nikonpicturecontrol.RecipeStore
import com.noahlin.nikonpicturecontrol.nilIfBlank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item { SectionHeader("Library") }
            item { SettingRow(Icons.Default.GridView, "Edit Categories") { nav.navigate("terms/category") } }
            item { SettingRow(Icons.Default.Sell, "Edit Tags") { nav.navigate("terms/tag") } }
            item { Divider(Modifier.padding(vertical = 8.dp)) }
            item {
                SettingRow(Icons.Default.HelpOutline, "How to add NP3 to Nikon cameras",
                    tint = MaterialTheme.colorScheme.primary) { nav.navigate("guide") }
            }
            item {
                Text("Made by Noah Lin with 💛",
                    Modifier.fillMaxWidth().padding(top = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp))
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTermsScreen(kind: String, store: RecipeStore, nav: NavController) {
    val isCategory = kind == "category"
    val title = if (isCategory) "Categories" else "Tags"
    val singular = if (isCategory) "Category" else "Tag"
    val terms = if (isCategory) store.categories else store.tags

    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var addText by remember { mutableStateOf("") }

    fun rename(old: String, new: String) =
        if (isCategory) store.renameCategory(old, new) else store.renameTag(old, new)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { addText = ""; showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add $singular")
                    }
                },
            )
        },
    ) { pad ->
        if (terms.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No $title", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(pad)) {
                items(terms, key = { it }) { term ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { renameTarget = term; renameText = term }
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(term, Modifier.weight(1f).padding(vertical = 16.dp))
                        IconButton(onClick = { deleteTarget = term }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Divider()
                }
            }
        }
    }

    if (showAdd) {
        TextPromptDialog("New $singular", addText, { addText = it }, onConfirm = {
            if (isCategory) store.addCategory(addText) else store.addTag(addText)
            showAdd = false
        }, onDismiss = { showAdd = false })
    }
    renameTarget?.let { target ->
        TextPromptDialog("Rename", renameText, { renameText = it },
            confirmEnabled = renameText.nilIfBlank() != null,
            onConfirm = { rename(target, renameText); renameTarget = null },
            onDismiss = { renameTarget = null })
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"$target\"?") },
            text = { Text("This removes it from every recipe that uses it.") },
            confirmButton = {
                TextButton(onClick = { rename(target, ""); deleteTarget = null }) {
                    Text("Delete $singular", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TextPromptDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, onValueChange, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Np3GuideScreen(nav: NavController) {
    val steps = listOf(
        "Download the `.NP3` file (use Save/Share on any recipe).",
        "Insert your SD card, create a folder named `/NIKON`, then a folder named `/CUSTOMPC` inside it, and copy the `.NP3` file into `/CUSTOMPC.`",
        "Insert the memory card into the camera.",
        "On the camera, go to MANAGE PICTURE CONTROL → LOAD/SAVE → COPY TO CAMERA.",
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Install .NP3 Files") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(steps) { step ->
                val index = steps.indexOf(step) + 1
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$index", color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(monospacedBackticks(step), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Render `` `backtick` `` spans in a monospaced font, mirroring the iOS guide. */
private fun monospacedBackticks(text: String) = buildAnnotatedString {
    text.split("`").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(part) }
        else append(part)
    }
}
