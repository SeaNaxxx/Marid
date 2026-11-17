package com.qodex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qodex.app.icons.QodexIcons
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.ServiceChannels
import com.qodex.app.rpc.asBoolOrNull
import com.qodex.app.rpc.asObjectOrNull
import com.qodex.app.rpc.asStringOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG_MODEL_EDITOR = "QodexModelEditor"

// Field set and validation mirror packages/ui/src/settings/model-provider-section/ProviderModelMetadata.ts.
internal data class ModelDraftValues2e(
    val idValue: String,
    val contextWindowValue: String,
    val maxOutputTokensValue: String,
    val supportsImage: Boolean,
    val supportsVideo: Boolean,
    val supportsPdf: Boolean,
    val supportsJsonSchemaOutputValue: Boolean,
    val supportsNativeWebSearchValue: Boolean,
    val supportsMidConversationSystemValue: Boolean,
    val reasoningLevelValuesValue: List<String>,
    val reasoningLevelMapValue: String,
    val useRecommendedConfigValue: Boolean,
)

internal fun seedDraftFromModel2e(model: ModelView2d): ModelDraftValues2e {
    val config = model.effectiveConfig
    val properties = config?.get("properties").asObjectOrNull()
    val inputFormat = properties?.get("inputFormat").asObjectOrNull()
    val optionSpecs = config?.get("optionSpecs").asObjectOrNull()
    val maxOutput = optionSpecs?.get("maxOutputTokens").asObjectOrNull()
    val reasoning = optionSpecs?.get("reasoningLevel").asObjectOrNull()
    val reasoningValues = (reasoning?.get("values") as? kotlinx.serialization.json.JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.content }
        ?: listOf("low", "medium", "high")
    val contextWindow = properties?.get("contextWindow")?.let { (it as? JsonPrimitive)?.content }
    val maxTokens = maxOutput?.get("max")?.let { (it as? JsonPrimitive)?.content }
    return ModelDraftValues2e(
        idValue = model.modelId,
        contextWindowValue = contextWindow ?: "",
        maxOutputTokensValue = maxTokens ?: "",
        supportsImage = inputFormat?.get("supportsImage").asBoolOrNull() ?: false,
        supportsVideo = inputFormat?.get("supportsVideo").asBoolOrNull() ?: false,
        supportsPdf = inputFormat?.get("supportsPdf").asBoolOrNull() ?: false,
        supportsJsonSchemaOutputValue = properties?.get("supportsJsonSchemaOutput").asBoolOrNull() ?: false,
        supportsNativeWebSearchValue = properties?.get("supportsNativeWebSearch").asBoolOrNull() ?: false,
        supportsMidConversationSystemValue =
            properties?.get("supportsMidConversationSystem").asBoolOrNull() ?: false,
        reasoningLevelValuesValue = reasoningValues,
        reasoningLevelMapValue = reasoning?.get("map")?.let { (it as? JsonPrimitive)?.content } ?: "",
        useRecommendedConfigValue = model.useRecommendedConfig,
    )
}

internal fun newBlankDraft2e(): ModelDraftValues2e = ModelDraftValues2e(
    idValue = "",
    contextWindowValue = "",
    maxOutputTokensValue = "",
    supportsImage = false,
    supportsVideo = false,
    supportsPdf = false,
    supportsJsonSchemaOutputValue = false,
    supportsNativeWebSearchValue = false,
    supportsMidConversationSystemValue = false,
    reasoningLevelValuesValue = listOf(),
    reasoningLevelMapValue = "",
    useRecommendedConfigValue = true,
)

internal sealed interface ModelDraftValidationError2e {
    data object Id : ModelDraftValidationError2e
    data object ContextWindow : ModelDraftValidationError2e
    data object MaxOutputTokens : ModelDraftValidationError2e
    data object ReasoningLevels : ModelDraftValidationError2e
    data object ReasoningMap : ModelDraftValidationError2e
}

internal fun validateModelDraft2e(draft: ModelDraftValues2e): ModelDraftValidationError2e? {
    if (draft.idValue.trim().isEmpty()) return ModelDraftValidationError2e.Id
    if (draft.contextWindowValue.trim().toLongOrNull()?.let { it > 0 } != true) {
        return ModelDraftValidationError2e.ContextWindow
    }
    // Manual mode owns the hard cap; smart mode lets the engine resolve it from the recommended config.
    if (!draft.useRecommendedConfigValue && draft.maxOutputTokensValue.trim().toLongOrNull()?.let { it > 0 } != true) {
        return ModelDraftValidationError2e.MaxOutputTokens
    }
    val levels = draft.reasoningLevelValuesValue.map { it.trim() }
    if (levels.isEmpty() || levels.any { it.isEmpty() } || levels.toSet().size != levels.size) {
        return ModelDraftValidationError2e.ReasoningLevels
    }
    val map = parseReasoningMap2e(draft.reasoningLevelMapValue)
    if (map == null || !map.keys.containsAll(levels)) return ModelDraftValidationError2e.ReasoningMap
    return null
}

private fun parseReasoningMap2e(raw: String): Map<String, String>? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return try {
        val obj = kotlinx.serialization.json.Json.parseToJsonElement(trimmed).asObjectOrNull()
            ?: return null
        obj.entries.associate { Pair(it.key, (it.value as? JsonPrimitive)?.content ?: "") }
    } catch (t: Throwable) {
        null
    }
}

internal fun buildPersonalModelConfig2e(draft: ModelDraftValues2e): JsonObject {
    val properties = buildJsonObject {
        put("contextWindow", draft.contextWindowValue.trim().toLong())
        put(
            "inputFormat",
            buildJsonObject {
                put("supportsText", true)
                put("supportsImage", draft.supportsImage)
                put("supportsVideo", draft.supportsVideo)
                put("supportsPdf", draft.supportsPdf)
            },
        )
        put("supportsJsonSchemaOutput", draft.supportsJsonSchemaOutputValue)
        put("supportsNativeWebSearch", draft.supportsNativeWebSearchValue)
        put("supportsMidConversationSystem", draft.supportsMidConversationSystemValue)
    }
    val optionSpecs = buildJsonObject {
        val maxOutput = draft.maxOutputTokensValue.trim().toLongOrNull()
        if (maxOutput != null) {
            put("maxOutputTokens", buildJsonObject { put("max", maxOutput) })
        }
        put(
            "reasoningLevel",
            buildJsonObject {
                put(
                    "values",
                    buildJsonArray {
                        draft.reasoningLevelValuesValue.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.trim())) }
                    },
                )
                put("map", draft.reasoningLevelMapValue.trim())
            },
        )
    }
    return buildJsonObject {
        put("properties", properties)
        put("optionSpecs", optionSpecs)
    }
}

// Engine commands match IProviderSettingsService; add is positional, draft save takes a single input object.
internal suspend fun addPersonalModel2e(
    rpc: QodexRpcClient,
    providerId: String,
    modelId: String,
    config: JsonObject,
    useRecommendedConfig: Boolean,
): Boolean = try {
    withTimeout(20_000) {
        rpc.callPositional(
            ServiceChannels.PROVIDER_SETTINGS,
            "addPersonalModel",
            listOf(
                JsonPrimitive(providerId),
                JsonPrimitive(modelId),
                config,
                JsonPrimitive(useRecommendedConfig),
            ),
        )
    } != null
} catch (t: Throwable) {
    android.util.Log.w(TAG_MODEL_EDITOR, "addPersonalModel failed: ${t.message}")
    false
}

internal suspend fun savePersonalModelDraft2e(
    rpc: QodexRpcClient,
    providerId: String,
    originalModelId: String,
    nextModelId: String,
    config: JsonObject,
    useRecommendedConfig: Boolean,
    basedOnRevision: Long,
): Boolean {
    val input = buildJsonObject {
        put("providerId", providerId)
        put("originalModelId", originalModelId)
        put("nextModelId", nextModelId)
        put("personalConfig", config)
        put("useRecommendedConfig", useRecommendedConfig)
        put("basedOnRevision", basedOnRevision)
    }
    return try {
        withTimeout(20_000) {
            rpc.callPositional(
                ServiceChannels.PROVIDER_SETTINGS,
                "savePersonalModelDraft",
                listOf(input),
            )
        } != null
    } catch (t: Throwable) {
        android.util.Log.w(TAG_MODEL_EDITOR, "savePersonalModelDraft failed: ${t.message}")
        false
    }
}

internal suspend fun deletePersonalModel2e(
    rpc: QodexRpcClient,
    providerId: String,
    modelId: String,
): Boolean = try {
    withTimeout(20_000) {
        rpc.callPositional(
            ServiceChannels.PROVIDER_SETTINGS,
            "deletePersonalModel",
            listOf(JsonPrimitive(providerId), JsonPrimitive(modelId)),
        )
    } != null
} catch (t: Throwable) {
    android.util.Log.w(TAG_MODEL_EDITOR, "deletePersonalModel failed: ${t.message}")
    false
}

// ── Provider detail: model list ──────────────────────────────────────────────

@Composable
internal fun ModelsSection2d(
    provider: ProviderEntry2d,
    revision: Long,
    rpc: QodexRpcClient?,
    onEditModel: (ModelView2d) -> Unit,
    onAddModel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Model list", // settings.modelProvider.models
                color = QodexTokens.ForegroundSubtlest,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton2d(label = "Add model", icon = QodexIcons.Plus, onClick = onAddModel)
        }
        if (provider.models.isEmpty()) {
            Text(
                "No models are configured. Add a model to use it in chat.", // modelsEmpty
                color = QodexTokens.ForegroundSubtlest,
                fontSize = 12.sp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (model in provider.models) {
                ModelRow2e(
                    model = model,
                    provider = provider,
                    revision = revision,
                    rpc = rpc,
                    onEdit = { onEditModel(model) },
                )
            }
        }
    }
}

@Composable
private fun ModelRow2e(
    model: ModelView2d,
    provider: ProviderEntry2d,
    revision: Long,
    rpc: QodexRpcClient?,
    onEdit: () -> Unit,
) {
    var deleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
            .background(QodexTokens.Surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model.modelId,
            color = QodexTokens.Foreground,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    when {
                        !model.enabled -> QodexTokens.ForegroundSubtlest
                        model.executable -> QodexTokens.Success
                        else -> QodexTokens.Warning
                    },
                    RoundedCornerShape(50),
                ),
        )
        GhostIconButton2d(
            icon = QodexIcons2d.Pencil,
            size = 28.dp,
            contentSize = 14.dp,
            contentDescription = "Edit model settings", // editModel
            background = Color.Transparent,
            rounded = 6.dp,
            onClick = onEdit,
        )
        if (!model.builtin) {
            GhostIconButton2d(
                icon = QodexIcons2d.Trash,
                size = 28.dp,
                contentSize = 14.dp,
                contentDescription = "Delete model",
                background = Color.Transparent,
                rounded = 6.dp,
                tint = QodexTokens.Destructive,
                enabled = !deleting,
                onClick = {
                    val client = rpc ?: return@GhostIconButton2d
                    scope.launch {
                        deleting = true
                        deletePersonalModel2e(client, provider.providerId, model.modelId)
                        deleting = false
                    }
                },
            )
        }
    }
}

// ── Add / Edit model dialog ──────────────────────────────────────────────────

@Composable
internal fun ModelEditorDialog2d(
    provider: ProviderEntry2d,
    target: ModelEditorTarget2d,
    revision: Long,
    rpc: QodexRpcClient?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val editing = target as? ModelEditorTarget2d.Edit
    var draft by remember(target) {
        mutableStateOf(
            when (target) {
                is ModelEditorTarget2d.Add -> newBlankDraft2e()
                is ModelEditorTarget2d.Edit -> seedDraftFromModel2e(target.model)
            },
        )
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<ModelDraftValidationError2e?>(null) }
    var saveFailedMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun commit() {
        val client = rpc ?: return
        val validation = validateModelDraft2e(draft)
        error = validation
        if (validation != null) return
        scope.launch {
            saving = true
            saveFailedMessage = null
            val config = buildPersonalModelConfig2e(draft)
            val modelId = draft.idValue.trim()
            val ok = when (target) {
                is ModelEditorTarget2d.Add -> addPersonalModel2e(
                    client,
                    provider.providerId,
                    modelId,
                    config,
                    draft.useRecommendedConfigValue,
                )
                is ModelEditorTarget2d.Edit -> savePersonalModelDraft2e(
                    client,
                    provider.providerId,
                    target.model.modelId,
                    modelId,
                    config,
                    draft.useRecommendedConfigValue,
                    revision,
                )
            }
            saving = false
            if (ok) onSaved()
            else saveFailedMessage = "Failed to save ${provider.name} / $modelId"
        }
    }

    ModelEditorDialogShell2e(
        title = if (editing == null) "Add model" else "Edit model settings", // addModel | editModel
        smart = draft.useRecommendedConfigValue,
        onSmartChange = { draft = draft.copy(useRecommendedConfigValue = it) },
        saving = saving,
        errorLabel = when (error) {
            ModelDraftValidationError2e.Id -> "Model ID is required"
            ModelDraftValidationError2e.ContextWindow -> "Context window must be a positive integer"
            ModelDraftValidationError2e.MaxOutputTokens -> "Max output tokens must be a positive integer"
            ModelDraftValidationError2e.ReasoningLevels -> "Reasoning levels cannot be empty or duplicated"
            ModelDraftValidationError2e.ReasoningMap -> "Reasoning parameter mapping is invalid"
            null -> saveFailedMessage
        },
        onDismiss = onDismiss,
        onReset = {
            draft = when (target) {
                is ModelEditorTarget2d.Add -> newBlankDraft2e()
                is ModelEditorTarget2d.Edit -> seedDraftFromModel2e(target.model)
            }
            error = null
            saveFailedMessage = null
        },
        onSave = { commit() },
    ) {
        EditorField2e(label = "Model ID") { // modelId
            EditorInput2e(
                value = draft.idValue,
                onValueChange = { draft = draft.copy(idValue = it) },
                monospace = true,
                enabled = !saving,
                placeholder = "Model ID",
            )
        }
        EditorField2e(label = "Context window") { // contextWindow
            EditorInput2e(
                value = draft.contextWindowValue,
                onValueChange = { draft = draft.copy(contextWindowValue = numericOnly2e(it)) },
                numeric = true,
                // Smart mode resolves the cap from the engine's recommended config; manual mode owns it.
                enabled = !saving && !draft.useRecommendedConfigValue,
                placeholder = "Context window",
            )
        }
        EditorField2e(label = "Max output tokens") { // maxOutputTokens
            EditorInput2e(
                value = draft.maxOutputTokensValue,
                onValueChange = { draft = draft.copy(maxOutputTokensValue = numericOnly2e(it)) },
                numeric = true,
                enabled = !saving && !draft.useRecommendedConfigValue,
                placeholder = "Max output tokens",
            )
        }
        EditorField2e(label = "Input types") { // inputModalities
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BooleanOptionChip2e(label = "Text", selected = true, enabled = false, onToggle = {})
                BooleanOptionChip2e(
                    label = "Image",
                    selected = draft.supportsImage,
                    enabled = !saving,
                    onToggle = { draft = draft.copy(supportsImage = !draft.supportsImage) },
                )
                BooleanOptionChip2e(
                    label = "Video",
                    selected = draft.supportsVideo,
                    enabled = !saving,
                    onToggle = { draft = draft.copy(supportsVideo = !draft.supportsVideo) },
                )
                BooleanOptionChip2e(
                    label = "PDF",
                    selected = draft.supportsPdf,
                    enabled = !saving,
                    onToggle = { draft = draft.copy(supportsPdf = !draft.supportsPdf) },
                )
            }
        }
        EditorField2e(label = "Model capabilities") { // capabilities
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BooleanOptionChip2e(
                    label = "Structured output", // supportsJsonSchemaOutput
                    selected = draft.supportsJsonSchemaOutputValue,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(supportsJsonSchemaOutputValue = !draft.supportsJsonSchemaOutputValue)
                    },
                )
                BooleanOptionChip2e(
                    label = "Native web search", // supportsNativeWebSearch
                    selected = draft.supportsNativeWebSearchValue,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(supportsNativeWebSearchValue = !draft.supportsNativeWebSearchValue)
                    },
                )
                BooleanOptionChip2e(
                    label = "Mid-conversation system messages", // supportsMidConversationSystem
                    selected = draft.supportsMidConversationSystemValue,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(
                            supportsMidConversationSystemValue = !draft.supportsMidConversationSystemValue,
                        )
                    },
                )
            }
        }
        EditorField2e(label = "Reasoning levels (low to high)") { // reasoningLevelsOrdered
            ReasoningLevelChips2e(
                levels = draft.reasoningLevelValuesValue,
                enabled = !saving,
                onChange = { draft = draft.copy(reasoningLevelValuesValue = it) },
            )
        }
        EditorField2e(label = "Reasoning parameter mapping") { // reasoningLevelMapping
            EditorInput2e(
                value = draft.reasoningLevelMapValue,
                onValueChange = { draft = draft.copy(reasoningLevelMapValue = it) },
                monospace = true,
                enabled = !saving,
                placeholder = "{\"low\": \"reasoning_effort\"}",
                minLines = 3,
            )
        }
    }
}

private fun numericOnly2e(raw: String): String = raw.filter { it.isDigit() }

@Composable
private fun ModelEditorDialogShell2e(
    title: String,
    smart: Boolean,
    onSmartChange: (Boolean) -> Unit,
    saving: Boolean,
    errorLabel: String?,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(12.dp))
            .background(QodexTokens.Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = QodexTokens.Foreground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Smart configuration", // followRecommendedConfig
                    color = QodexTokens.Foreground,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = smart,
                    onCheckedChange = onSmartChange,
                    enabled = !saving,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = QodexTokens.Primary,
                        uncheckedTrackColor = QodexTokens.Input,
                    ),
                )
            }
            content()
            errorLabel?.let {
                Text(
                    text = it,
                    color = QodexTokens.Destructive,
                    fontSize = 12.sp, // text-ui-sm
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlineButton2d(label = "Reset form", onClick = onReset, enabled = !saving) // resetForm
                Spacer(Modifier.weight(1f))
                OutlineButton2d(label = "Cancel", onClick = onDismiss, enabled = !saving) // common.cancel
                PrimaryButton2d(
                    label = if (saving) "Saving..." else "Save", // common.saving | common.save
                    icon = null,
                    enabled = !saving,
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
private fun EditorField2e(label: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            color = QodexTokens.ForegroundSubtle,
            fontSize = 14.sp, // text-ui-base
            modifier = Modifier.padding(bottom = 4.dp), // mb-1
        )
        content()
    }
}

@Composable
private fun EditorInput2e(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
    monospace: Boolean = false,
    numeric: Boolean = false,
    minLines: Int = 1,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = minLines == 1,
        enabled = enabled,
        maxLines = if (minLines == 1) 1 else 8,
        textStyle = TextStyle(
            color = QodexTokens.Foreground,
            fontSize = 14.sp,
            fontFamily = if (monospace || numeric) FontFamily.Monospace else FontFamily.Default,
        ),
        cursorBrush = SolidColor(QodexTokens.Foreground),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp) // h-9
                    .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
                    .background(QodexTokens.Input) // bg-input
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = if (minLines == 1) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = QodexTokens.ForegroundSubtlest,
                        fontSize = 14.sp,
                    )
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BooleanOptionChip2e(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .border(
                1.dp,
                if (selected) QodexTokens.Primary else QodexTokens.Border,
                RoundedCornerShape(8.dp),
            )
            .background(if (selected) QodexTokens.SurfaceHover else QodexTokens.Input)
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModelOptionCheckbox2e(selected = selected)
        Text(
            text = label,
            color = QodexTokens.Foreground,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ModelOptionCheckbox2e(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp) // size-4
            .border(
                1.dp,
                if (selected) QodexTokens.Primary else QodexTokens.Border,
                RoundedCornerShape(3.dp),
            )
            .background(if (selected) QodexTokens.Primary else QodexTokens.Input),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = QodexIcons2d.Check,
                contentDescription = null,
                tint = QodexTokens.PrimaryForeground,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun ReasoningLevelChips2e(
    levels: List<String>,
    enabled: Boolean,
    onChange: (List<String>) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var addValue by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((index, level) in levels.withIndex()) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
                        .background(QodexTokens.Input)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = level,
                        color = QodexTokens.Foreground,
                        fontSize = 13.sp,
                    )
                    if (enabled && levels.size > 1) {
                        Icon(
                            imageVector = QodexIcons2d.Close,
                            contentDescription = "Delete reasoning level: $level",
                            tint = QodexTokens.ForegroundSubtle,
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onChange(levels.filterIndexed { i, _ -> i != index }) },
                        )
                    }
                }
            }
            if (enabled && !adding) {
                GhostIconButton2d(
                    icon = QodexIcons.Plus,
                    size = 32.dp,
                    contentSize = 14.dp,
                    contentDescription = "Add reasoning level",
                    background = Color.Transparent,
                    border = QodexTokens.Border,
                    rounded = 8.dp,
                    onClick = {
                        addValue = ""
                        adding = true
                    },
                )
            }
        }
        if (adding) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    EditorInput2e(
                        value = addValue,
                        onValueChange = { addValue = it },
                        enabled = true,
                        placeholder = "e.g. high",
                    )
                }
                PrimaryButton2d(
                    label = "Add",
                    icon = null,
                    onClick = {
                        val trimmed = addValue.trim()
                        if (trimmed.isNotEmpty() && !levels.contains(trimmed)) {
                            onChange(levels + trimmed)
                        }
                        adding = false
                    },
                )
            }
        }
    }
}

// ── Template picker (Add provider entry point) ───────────────────────────────

@Composable
internal fun TemplatePickerDialog2d(
    templates: List<ProviderTemplateEntry2d>,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onPick: (templateId: String?, providerName: String?) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(12.dp))
            .background(QodexTokens.Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Add provider", // templatePickerTitle
                    color = QodexTokens.Foreground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                GhostIconButton2d(
                    icon = QodexIcons2d.Close,
                    size = 28.dp,
                    contentSize = 14.dp,
                    contentDescription = "Close",
                    background = Color.Transparent,
                    rounded = 6.dp,
                    onClick = onDismiss,
                )
            }
            Text(
                "Zhipu", // templateGroup.zhipu
                color = QodexTokens.ForegroundSubtle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            for (template in templates.filter { it.category == "zhipu" }) {
                TemplatePickRow2e(template.name, enabled) { onPick(template.templateId, null) }
            }
            TemplatePickRow2e("Create custom provider", enabled) { onPick(null, "New provider") } // newProviderName
            Text(
                "Other", // templateGroup.other
                color = QodexTokens.ForegroundSubtle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            for (template in templates.filter { it.category == "other" }) {
                TemplatePickRow2e(template.name, enabled) { onPick(template.templateId, null) }
            }
        }
    }
}

@Composable
private fun TemplatePickRow2e(label: String, enabled: Boolean, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
            .background(QodexTokens.Surface)
            .clickable(enabled = enabled, onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = QodexIcons.Package,
            contentDescription = null,
            tint = QodexTokens.Foreground,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            color = QodexTokens.Foreground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = QodexIcons.ChevronRight,
            contentDescription = null,
            tint = QodexTokens.ForegroundSubtlest,
            modifier = Modifier.size(16.dp),
        )
    }
}
