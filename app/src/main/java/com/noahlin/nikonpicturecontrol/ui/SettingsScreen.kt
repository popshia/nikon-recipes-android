package com.noahlin.nikonpicturecontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
fun SettingsScreen(store: RecipeStore, nav: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // Large collapsing title — the signature Android 12+ Settings header.
            LargeTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SettingsGroup("Library") {
                    SettingListItem(Icons.Default.GridView, "Edit categories",
                        "Rename or remove recipe categories") { nav.navigate("terms/category") }
                    SettingDivider()
                    SettingListItem(Icons.Default.Sell, "Edit tags",
                        "Rename or remove tags") { nav.navigate("terms/tag") }
                }
            }
            item {
                SettingsGroup("Library sync") {
                    ListItem(
                        headlineContent = { Text("Fetch Latest Recipes") },
                        supportingContent = { Text("Download new recipes from the cloud") },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(enabled = !store.isFetching) { store.fetchLatest() },
                    )
                    if (store.isFetching) {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    store.fetchError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
                    }
                }
            }
            item {
                SettingsGroup("Help") {
                    SettingListItem(Icons.AutoMirrored.Filled.HelpOutline,
                        "How to add NP3 to Nikon cameras",
                        "Step-by-step guide for your camera") { nav.navigate("guide") }
                }
            }
            if (supportsDynamicColor) {
                item {
                    SettingsGroup("Appearance") {
                        SwitchListItem(
                            title = "Use wallpaper colors",
                            supporting = "Color the app from your wallpaper · Android 12+",
                            checked = store.dynamicColorEnabled,
                            onCheckedChange = store::setDynamicColor,
                        )
                    }
                }
            }
            item { AppFooter() }
        }
    }
}

/** A titled group of settings wrapped in a rounded surface card (grouped-preferences style). */
@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
            content = { Column(content = content) },
        )
    }
}

@Composable
private fun SettingListItem(icon: ImageVector, title: String, supporting: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/** Full-row-tappable switch item — tapping anywhere toggles it. */
@Composable
private fun SwitchListItem(title: String, supporting: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supporting) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
}

/** Text-aligned divider between items inside a group card. */
@Composable
private fun SettingDivider() =
    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun AppFooter() {
    val ctx = LocalContext.current
    val version = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull()
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        version?.let {
            Text("Nikon Recipes $it", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Made by Noah Lin with 💛", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                scrollBehavior = scrollBehavior,
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
                    HorizontalDivider()
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Install .NP3 Files") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
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
