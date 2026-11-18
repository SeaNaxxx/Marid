package com.qodex.app.ui

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qodex.app.icons.QodexIcons
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.ServiceChannels
import com.qodex.app.rpc.asArrayOrNull
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
// Numeric inputs hold the Personal Overlay value only; inherited values are placeholders that the
// engine re-resolves at commit, exactly like the desktop draft state machine.
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
    // Sparse overlay state: leaves pinned as personal overrides (seeded from a prior save, extended
    // by in-session edits). Untouched leaves stay inherited — the engine resolves them at commit.
    val builtinConfig: JsonObject? = null,
    val personalConfig: JsonObject? = null,
    val overriddenFields: Set<String> = emptySet(),
)

// Leaf keys mirror React's overriddenFieldsValue naming so the commit rules stay comparable.
internal fun personalDraftFieldKeys2e(personal: JsonObject?): Set<String> {
    if (personal == null) return emptySet()
    val result = mutableSetOf<String>()
    val properties = personal["properties"].asObjectOrNull()
    val optionSpecs = personal["optionSpecs"].asObjectOrNull()
    for (key in listOf("supportsJsonSchemaOutput", "supportsNativeWebSearch", "supportsMidConversationSystem")) {
        if (properties?.get(key) != null) result.add("${key}Value")
    }
    if (optionSpecs?.get("reasoningLevel").asObjectOrNull()?.get("values") != null) {
        result.add("reasoningLevelValuesValue")
    }
    val inputFormat = properties?.get("inputFormat").asObjectOrNull()
    for (key in listOf("supportsImage", "supportsVideo", "supportsPdf")) {
        if (inputFormat?.get(key) != null) result.add("inputFormatValue.$key")
    }
    return result
}

internal fun seedDraftFromModel2e(model: ModelView2d): ModelDraftValues2e {
    val effective = model.effectiveConfig
    val properties = effective?.get("properties").asObjectOrNull()
    val inputFormat = properties?.get("inputFormat").asObjectOrNull()
    val optionSpecs = effective?.get("optionSpecs").asObjectOrNull()
    val personal = model.personalConfig
    val personalProperties = personal?.get("properties").asObjectOrNull()
    val personalOptionSpecs = personal?.get("optionSpecs").asObjectOrNull()
    val reasoning = optionSpecs?.get("reasoningLevel").asObjectOrNull()
    // No hardcoded level fallback: the engine view (or a user-added level) is the only source,
    // matching the desktop editor which renders whatever the config resolution reports.
    val reasoningValues = (reasoning?.get("values") as? kotlinx.serialization.json.JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.content }
        ?: emptyList()
    return ModelDraftValues2e(
        idValue = model.modelId,
        contextWindowValue =
            personalProperties?.get("contextWindow").asStringOrNull() ?: "",
        maxOutputTokensValue =
            personalOptionSpecs?.get("maxOutputTokens").asObjectOrNull()
                ?.get("max").asStringOrNull() ?: "",
        supportsImage = inputFormat?.get("supportsImage").asBoolOrNull() ?: false,
        supportsVideo = inputFormat?.get("supportsVideo").asBoolOrNull() ?: false,
        supportsPdf = inputFormat?.get("supportsPdf").asBoolOrNull() ?: false,
        supportsJsonSchemaOutputValue = properties?.get("supportsJsonSchemaOutput").asBoolOrNull() ?: false,
        supportsNativeWebSearchValue = properties?.get("supportsNativeWebSearch").asBoolOrNull() ?: false,
        supportsMidConversationSystemValue =
            properties?.get("supportsMidConversationSystem").asBoolOrNull() ?: false,
        reasoningLevelValuesValue = reasoningValues,
        reasoningLevelMapValue =
            personalOptionSpecs?.get("reasoningLevel").asObjectOrNull()
                ?.get("map").asStringOrNull() ?: "",
        useRecommendedConfigValue = model.useRecommendedConfig,
        builtinConfig = model.builtinConfig,
        personalConfig = model.personalConfig,
        overriddenFields = personalDraftFieldKeys2e(personal),
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

private fun positiveOrNull2e(raw: String): Long? =
    raw.trim().toLongOrNull()?.takeIf { it > 0 }

// 修复：智能模式下 Context window / Max output tokens 留空表示继承推荐配置（React 兜底
// inherited?.properties?.contextWindow ?? null），此前「禁用 + 必填」导致 Add model 永远无法保存。
internal fun validateModelDraft2e(
    draft: ModelDraftValues2e,
    inherited: JsonObject?,
): ModelDraftValidationError2e? {
    if (draft.idValue.trim().isEmpty()) return ModelDraftValidationError2e.Id
    val contextText = draft.contextWindowValue.trim()
    if (contextText.isNotEmpty()) {
        if (positiveOrNull2e(contextText) == null) return ModelDraftValidationError2e.ContextWindow
    } else if (!draft.useRecommendedConfigValue) {
        // Manual mode owns the hard cap; smart mode may commit a sparse overlay
        // when no recommendation exists to inherit (unknown ids such as
        // openrouter free-agent model ids must still be saveable).
        return ModelDraftValidationError2e.ContextWindow
    }
    // Manual mode owns the hard cap; smart mode lets the engine resolve it from the recommended config.
    val maxText = draft.maxOutputTokensValue.trim()
    if (maxText.isNotEmpty()) {
        if (positiveOrNull2e(maxText) == null) return ModelDraftValidationError2e.MaxOutputTokens
    } else if (!draft.useRecommendedConfigValue) {
        return ModelDraftValidationError2e.MaxOutputTokens
    }
    val levels = draft.reasoningLevelValuesValue.map { it.trim() }
    val resolvedMapText = draft.reasoningLevelMapValue.trim().ifEmpty {
        if (draft.useRecommendedConfigValue) {
            inherited?.get("optionSpecs").asObjectOrNull()
                ?.get("reasoningLevel").asObjectOrNull()?.get("map").asStringOrNull().orEmpty()
        } else {
            ""
        }
    }
    if (draft.useRecommendedConfigValue && levels.isEmpty() && resolvedMapText.isEmpty()) {
        // Sparse overlay: no user-entered reasoning contract and no engine
        // recommendation — the engine applies its defaults instead of the
        // editor inventing levels for a model it knows nothing about.
        return null
    }
    if (levels.isEmpty() || levels.any { it.isEmpty() } || levels.toSet().size != levels.size) {
        return ModelDraftValidationError2e.ReasoningLevels
    }
    if (resolvedMapText.isEmpty()) return ModelDraftValidationError2e.ReasoningMap
    val map = parseReasoningMap2e(resolvedMapText)
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

// 对齐 React resolveProviderModelDraftCommit：智能模式只提交用户改动的叶子（稀疏 Overlay），
// 未触碰的叶子保持继承；手动模式物化产品开放的完整可编辑叶子（extractManualModelConfig 等价）。
internal fun buildPersonalModelConfig2e(
    draft: ModelDraftValues2e,
    inherited: JsonObject?,
): JsonObject {
    if (!draft.useRecommendedConfigValue) return buildManualModelConfig2e(draft)
    val personal = draft.personalConfig ?: JsonObject(emptyMap())
    val baseProperties = personal["properties"].asObjectOrNull() ?: JsonObject(emptyMap())
    val baseOptionSpecs = personal["optionSpecs"].asObjectOrNull() ?: JsonObject(emptyMap())
    val outProperties = LinkedHashMap(baseProperties)
    val outInputFormat =
        LinkedHashMap(baseProperties["inputFormat"].asObjectOrNull() ?: JsonObject(emptyMap()))
    val outOptionSpecs = LinkedHashMap(baseOptionSpecs)
    val outReasoning =
        LinkedHashMap(baseOptionSpecs["reasoningLevel"].asObjectOrNull() ?: JsonObject(emptyMap()))
    val outMaxSpec =
        LinkedHashMap(baseOptionSpecs["maxOutputTokens"].asObjectOrNull() ?: JsonObject(emptyMap()))

    val contextText = draft.contextWindowValue.trim()
    if (contextText.isNotEmpty()) {
        outProperties["contextWindow"] = JsonPrimitive(positiveOrNull2e(contextText))
    } else {
        // 数值清空即撤销覆盖，交还继承层（React: deleteMutable(personalProperties, "contextWindow")）。
        outProperties.remove("contextWindow")
    }

    for (key in listOf("supportsJsonSchemaOutput", "supportsNativeWebSearch", "supportsMidConversationSystem")) {
        val value = when (key) {
            "supportsJsonSchemaOutput" -> draft.supportsJsonSchemaOutputValue
            "supportsNativeWebSearch" -> draft.supportsNativeWebSearchValue
            else -> draft.supportsMidConversationSystemValue
        }
        if ("${key}Value" in draft.overriddenFields) {
            outProperties[key] = JsonPrimitive(value)
        } else {
            outProperties.remove(key)
        }
    }
    for (key in listOf("supportsImage", "supportsVideo", "supportsPdf")) {
        val value = when (key) {
            "supportsImage" -> draft.supportsImage
            "supportsVideo" -> draft.supportsVideo
            else -> draft.supportsPdf
        }
        if ("inputFormatValue.$key" in draft.overriddenFields) {
            outInputFormat[key] = JsonPrimitive(value)
        } else {
            outInputFormat.remove(key)
        }
    }
    if (outInputFormat.isEmpty()) {
        outProperties.remove("inputFormat")
    } else {
        outProperties["inputFormat"] = JsonObject(outInputFormat)
    }

    if ("reasoningLevelValuesValue" in draft.overriddenFields) {
        outReasoning["values"] = buildJsonArray {
            draft.reasoningLevelValuesValue.forEach { add(kotlinx.serialization.json.JsonPrimitive(it.trim())) }
        }
    } else {
        outReasoning.remove("values")
    }
    val mapText = draft.reasoningLevelMapValue.trim()
    if (mapText.isNotEmpty()) outReasoning["map"] = JsonPrimitive(mapText)
    if (outReasoning.isEmpty()) {
        outOptionSpecs.remove("reasoningLevel")
    } else {
        outOptionSpecs["reasoningLevel"] = JsonObject(outReasoning)
    }

    val maxText = draft.maxOutputTokensValue.trim()
    if (maxText.isNotEmpty()) {
        outMaxSpec["max"] = JsonPrimitive(positiveOrNull2e(maxText))
    } else {
        outMaxSpec.remove("max")
    }
    if (outMaxSpec.isEmpty()) {
        outOptionSpecs.remove("maxOutputTokens")
    } else {
        outOptionSpecs["maxOutputTokens"] = JsonObject(outMaxSpec)
    }

    val config = LinkedHashMap(personal)
    if (outProperties.isEmpty()) config.remove("properties") else config["properties"] = JsonObject(outProperties)
    if (outOptionSpecs.isEmpty()) config.remove("optionSpecs") else config["optionSpecs"] = JsonObject(outOptionSpecs)
    return JsonObject(config)
}

private fun buildManualModelConfig2e(draft: ModelDraftValues2e): JsonObject = buildJsonObject {
    put(
        "properties",
        buildJsonObject {
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
        },
    )
    put(
        "optionSpecs",
        buildJsonObject {
            put("maxOutputTokens", buildJsonObject { put("max", draft.maxOutputTokensValue.trim().toLong()) })
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
        },
    )
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

// Facade surface for these mutations is positional: (providerId, modelId, enabled) / (providerId, modelIds).
internal suspend fun setPersonalModelEnabled2e(
    rpc: QodexRpcClient,
    providerId: String,
    modelId: String,
    enabled: Boolean,
): Boolean = try {
    withTimeout(20_000) {
        rpc.callPositional(
            ServiceChannels.PROVIDER_SETTINGS,
            "setPersonalModelEnabled",
            listOf(JsonPrimitive(providerId), JsonPrimitive(modelId), JsonPrimitive(enabled)),
        )
    } != null
} catch (t: Throwable) {
    android.util.Log.w(TAG_MODEL_EDITOR, "setPersonalModelEnabled failed: ${t.message}")
    false
}

internal suspend fun reorderPersonalModels2e(
    rpc: QodexRpcClient,
    providerId: String,
    orderedModelIds: List<String>,
): Boolean = try {
    withTimeout(20_000) {
        rpc.callPositional(
            ServiceChannels.PROVIDER_SETTINGS,
            "reorderPersonalModels",
            listOf(
                JsonPrimitive(providerId),
                buildJsonArray { orderedModelIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } },
            ),
        )
    } != null
} catch (t: Throwable) {
    android.util.Log.w(TAG_MODEL_EDITOR, "reorderPersonalModels failed: ${t.message}")
    false
}

internal suspend fun testModelConnectivity2e(
    rpc: QodexRpcClient,
    workspacePath: String,
    providerId: String,
    modelId: String,
): Boolean = try {
    withTimeout(30_000) {
        rpc.testModelConnectivity(workspacePath, providerId, modelId)
    } != null
} catch (t: Throwable) {
    android.util.Log.w(TAG_MODEL_EDITOR, "testModelConnectivity failed: ${t.message}")
    false
}

// Smart-mode placeholder source: {inheritedConfig, effectiveConfig, issues} — only inheritedConfig is needed here.
internal suspend fun resolveModelConfig2e(
    rpc: QodexRpcClient,
    providerId: String,
    modelId: String,
): JsonObject? = try {
    withTimeout(15_000) {
        rpc.callPositional(
            ServiceChannels.PROVIDER_SETTINGS,
            "resolveModelConfig",
            listOf(
                buildJsonObject {
                    put("providerId", providerId)
                    put("modelId", modelId)
                },
            ),
        )
    }?.asObjectOrNull()?.get("inheritedConfig").asObjectOrNull()
} catch (t: Throwable) {
    android.util.Log.w(TAG_MODEL_EDITOR, "resolveModelConfig failed: ${t.message}")
    null
}

// ── Provider detail: model list ──────────────────────────────────────────────

@Composable
internal fun ModelsSection2d(
    provider: ProviderEntry2d,
    revision: Long,
    rpc: QodexRpcClient?,
    workspacePath: String,
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
            for ((index, model) in provider.models.withIndex()) {
                ModelRow2e(
                    model = model,
                    provider = provider,
                    modelIndex = index,
                    revision = revision,
                    rpc = rpc,
                    workspacePath = workspacePath,
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
    modelIndex: Int,
    revision: Long,
    rpc: QodexRpcClient?,
    workspacePath: String,
    onEdit: () -> Unit,
) {
    var busy by remember { mutableStateOf(false) }
    var rowFailure by remember { mutableStateOf<String?>(null) }
    var rowSuccess by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // 修复：行内每个动作（启停/重排/连通性测试/删除）失败时给出真实反馈，不再静默吞错。
    fun runAction(client: QodexRpcClient, label: String, successMessage: String, action: suspend (QodexRpcClient) -> Boolean) {
        scope.launch {
            busy = true
            rowFailure = null
            rowSuccess = null
            val ok = try {
                action(client)
            } catch (t: Throwable) {
                false
            }
            busy = false
            if (ok) rowSuccess = successMessage else rowFailure = "$label failed"
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
                .background(QodexTokens.Surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                icon = QodexIcons.Plug,
                size = 28.dp,
                contentSize = 14.dp,
                contentDescription = "Test model connectivity", // connectivityTest
                background = Color.Transparent,
                rounded = 6.dp,
                enabled = !busy && rpc != null,
                onClick = {
                    runAction(rpc!!, "Connectivity test", "Connectivity test passed") {
                        testModelConnectivity2e(it, workspacePath, provider.providerId, model.modelId)
                    }
                },
            )
            GhostIconButton2d(
                icon = QodexIcons.ArrowUp,
                size = 28.dp,
                contentSize = 14.dp,
                contentDescription = "Move model up", // reorder
                background = Color.Transparent,
                rounded = 6.dp,
                enabled = !busy && modelIndex > 0 && rpc != null,
                onClick = {
                    val ids = provider.models.map { it.modelId }.toMutableList()
                    val target = modelIndex - 1
                    ids[modelIndex] = ids[target].also { ids[target] = ids[modelIndex] }
                    runAction(rpc!!, "Reorder", "Model order saved") {
                        reorderPersonalModels2e(it, provider.providerId, ids)
                    }
                },
            )
            GhostIconButton2d(
                icon = QodexIcons.ChevronDown,
                size = 28.dp,
                contentSize = 14.dp,
                contentDescription = "Move model down", // reorder
                background = Color.Transparent,
                rounded = 6.dp,
                enabled = !busy && modelIndex < provider.models.size - 1 && rpc != null,
                onClick = {
                    val ids = provider.models.map { it.modelId }.toMutableList()
                    val target = modelIndex + 1
                    ids[modelIndex] = ids[target].also { ids[target] = ids[modelIndex] }
                    runAction(rpc!!, "Reorder", "Model order saved") {
                        reorderPersonalModels2e(it, provider.providerId, ids)
                    }
                },
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
                    enabled = !busy,
                    onClick = {
                        runAction(rpc!!, "Delete model", "Model deleted") {
                            deletePersonalModel2e(it, provider.providerId, model.modelId)
                        }
                    },
                )
            }
            Switch(
                checked = model.enabled,
                onCheckedChange = { next ->
                    runAction(rpc!!, "Enable change", if (next) "Model enabled" else "Model disabled") {
                        setPersonalModelEnabled2e(it, provider.providerId, model.modelId, next)
                    }
                },
                enabled = !busy && rpc != null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = QodexTokens.Primary,
                    uncheckedTrackColor = QodexTokens.Input,
                ),
            )
        }
        when {
            rowFailure != null -> FeedbackBanner2d(message = rowFailure!!, failure = true)
            rowSuccess != null -> FeedbackBanner2d(message = rowSuccess!!, failure = false)
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

    // Inherited (recommended) config: Edit seeds from the engine view; Add resolves on Model ID commit.
    var inheritedConfig by remember(target) {
        mutableStateOf(
            when (target) {
                is ModelEditorTarget2d.Add -> null
                is ModelEditorTarget2d.Edit -> target.model.builtinConfig ?: target.model.effectiveConfig
            },
        )
    }

    fun resolveInherited() {
        val client = rpc ?: return
        if (target !is ModelEditorTarget2d.Add) return
        val modelId = draft.idValue.trim()
        if (modelId.isEmpty()) return
        scope.launch {
            val resolved = resolveModelConfig2e(client, provider.providerId, modelId) ?: return@launch
            inheritedConfig = resolved
            // Fill only empty draft fields from the resolution; typed values stay user-owned
            // (React renders them as placeholders, the commit treats blank leaves as inherited).
            draft = draft.copy(
                reasoningLevelValuesValue =
                    if (draft.reasoningLevelValuesValue.isEmpty()) {
                        resolved["optionSpecs"].asObjectOrNull()
                            ?.get("reasoningLevel").asObjectOrNull()
                            ?.get("values").asArrayOrNull()
                            ?.mapNotNull { (it as? JsonPrimitive)?.content }
                            ?: emptyList()
                    } else {
                        draft.reasoningLevelValuesValue
                    },
            )
        }
    }

    fun commit() {
        val client = rpc
        if (client == null) {
            // A silent `rpc ?: return` used to make Save do literally nothing.
            saveFailedMessage = "Not connected to the engine — cannot save right now"
            return
        }
        if (saving) return
        saving = true
        saveFailedMessage = null
        error = null
        scope.launch {
            try {
                // Add + smart mode: resolve the engine recommendation at commit
                // time. Resolution used to run only on Model ID focus loss, and
                // Compose does not clear focus when a non-focusable Save row is
                // tapped — so a freshly typed model id always validated against
                // an empty inherited config and the first save silently failed.
                if (target is ModelEditorTarget2d.Add &&
                    draft.useRecommendedConfigValue &&
                    inheritedConfig == null &&
                    draft.idValue.trim().isNotEmpty()
                ) {
                    val resolved = resolveModelConfig2e(client, provider.providerId, draft.idValue.trim())
                    if (resolved != null) {
                        inheritedConfig = resolved
                        if (draft.reasoningLevelValuesValue.isEmpty()) {
                            draft = draft.copy(
                                reasoningLevelValuesValue = resolved["optionSpecs"]
                                    .asObjectOrNull()
                                    ?.get("reasoningLevel").asObjectOrNull()
                                    ?.get("values").asArrayOrNull()
                                    ?.mapNotNull { (it as? JsonPrimitive)?.content }
                                    ?: emptyList(),
                            )
                        }
                    }
                }
                val validation = validateModelDraft2e(draft, inheritedConfig)
                error = validation
                if (validation != null) return@launch
                val config = buildPersonalModelConfig2e(draft, inheritedConfig)
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
                if (ok) {
                    onSaved()
                } else {
                    saveFailedMessage = "Failed to save ${provider.name} / $modelId"
                }
            } catch (t: Throwable) {
                // Surface the engine's actual rejection (duplicate id, invalid
                // config, ...) instead of a generic failure line.
                saveFailedMessage = t.message?.take(200) ?: "Failed to save model"
            } finally {
                saving = false
            }
        }
    }

    val inheritedContext = inheritedConfig?.get("properties").asObjectOrNull()
        ?.get("contextWindow").asStringOrNull().orEmpty()
    val inheritedMax = inheritedConfig?.get("optionSpecs").asObjectOrNull()
        ?.get("maxOutputTokens").asObjectOrNull()
        ?.get("max").asStringOrNull().orEmpty()
    val inheritedMap = inheritedConfig?.get("optionSpecs").asObjectOrNull()
        ?.get("reasoningLevel").asObjectOrNull()
        ?.get("map").asStringOrNull().orEmpty()

    ModelEditorDialogShell2e(
        title = if (editing == null) "Add model" else "Edit model settings", // addModel | editModel
        smart = draft.useRecommendedConfigValue,
        // 从固定模式切回智能模式时清空已有 Overlay 基线（React clearPersonalConfigValue 语义）。
        onSmartChange = { next ->
            draft =
                if (next && !draft.useRecommendedConfigValue) {
                    draft.copy(
                        useRecommendedConfigValue = true,
                        personalConfig = null,
                        overriddenFields = emptySet(),
                        contextWindowValue = "",
                        maxOutputTokensValue = "",
                        reasoningLevelMapValue = "",
                    )
                } else {
                    draft.copy(useRecommendedConfigValue = next)
                }
        },
        saving = saving,
        errorLabel = when (error) {
            ModelDraftValidationError2e.Id -> "Model ID is required"
            ModelDraftValidationError2e.ContextWindow ->
                "Context window must be a positive integer (leave blank in smart mode to inherit)"
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
            inheritedConfig = when (target) {
                is ModelEditorTarget2d.Add -> null
                is ModelEditorTarget2d.Edit -> target.model.builtinConfig ?: target.model.effectiveConfig
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
                onFocusLost = ::resolveInherited,
            )
        }
        EditorField2e(label = "Context window") { // contextWindow
            EditorInput2e(
                value = draft.contextWindowValue,
                onValueChange = { draft = draft.copy(contextWindowValue = numericOnly2e(it)) },
                numeric = true,
                // 智能模式下同样可编辑：留空 = 继承推荐值（placeholder 展示引擎解析结果）。
                enabled = !saving,
                placeholder =
                    if (draft.useRecommendedConfigValue && inheritedContext.isNotEmpty()) inheritedContext
                    else "Context window",
            )
        }
        EditorField2e(label = "Max output tokens") { // maxOutputTokens
            EditorInput2e(
                value = draft.maxOutputTokensValue,
                onValueChange = { draft = draft.copy(maxOutputTokensValue = numericOnly2e(it)) },
                numeric = true,
                enabled = !saving,
                placeholder =
                    if (draft.useRecommendedConfigValue && inheritedMax.isNotEmpty()) inheritedMax
                    else "Max output tokens",
            )
        }
        EditorField2e(label = "Input types") { // inputModalities
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BooleanOptionChip2e(label = "Text", selected = true, enabled = false, onToggle = {})
                BooleanOptionChip2e(
                    label = "Image",
                    selected = draft.supportsImage,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(
                            supportsImage = !draft.supportsImage,
                            overriddenFields = draft.overriddenFields + "inputFormatValue.supportsImage",
                        )
                    },
                )
                BooleanOptionChip2e(
                    label = "Video",
                    selected = draft.supportsVideo,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(
                            supportsVideo = !draft.supportsVideo,
                            overriddenFields = draft.overriddenFields + "inputFormatValue.supportsVideo",
                        )
                    },
                )
                BooleanOptionChip2e(
                    label = "PDF",
                    selected = draft.supportsPdf,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(
                            supportsPdf = !draft.supportsPdf,
                            overriddenFields = draft.overriddenFields + "inputFormatValue.supportsPdf",
                        )
                    },
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
                        draft = draft.copy(
                            supportsJsonSchemaOutputValue = !draft.supportsJsonSchemaOutputValue,
                            overriddenFields = draft.overriddenFields + "supportsJsonSchemaOutputValue",
                        )
                    },
                )
                BooleanOptionChip2e(
                    label = "Native web search", // supportsNativeWebSearch
                    selected = draft.supportsNativeWebSearchValue,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(
                            supportsNativeWebSearchValue = !draft.supportsNativeWebSearchValue,
                            overriddenFields = draft.overriddenFields + "supportsNativeWebSearchValue",
                        )
                    },
                )
                BooleanOptionChip2e(
                    label = "Mid-conversation system messages", // supportsMidConversationSystem
                    selected = draft.supportsMidConversationSystemValue,
                    enabled = !saving,
                    onToggle = {
                        draft = draft.copy(
                            supportsMidConversationSystemValue = !draft.supportsMidConversationSystemValue,
                            overriddenFields = draft.overriddenFields + "supportsMidConversationSystemValue",
                        )
                    },
                )
            }
        }
        EditorField2e(label = "Reasoning levels (low to high)") { // reasoningLevelsOrdered
            ReasoningLevelChips2e(
                levels = draft.reasoningLevelValuesValue,
                enabled = !saving,
                onChange = {
                    draft = draft.copy(
                        reasoningLevelValuesValue = it,
                        overriddenFields = draft.overriddenFields + "reasoningLevelValuesValue",
                    )
                },
            )
        }
        EditorField2e(label = "Reasoning parameter mapping") { // reasoningLevelMapping
            EditorInput2e(
                value = draft.reasoningLevelMapValue,
                onValueChange = { draft = draft.copy(reasoningLevelMapValue = it) },
                monospace = true,
                enabled = !saving,
                placeholder =
                    if (draft.useRecommendedConfigValue && inheritedMap.isNotEmpty()) inheritedMap
                    else "{\"low\": \"reasoning_effort\"}",
                minLines = 3,
            )
        }
    }
}

private fun numericOnly2e(raw: String): String = raw.filter { it.isDigit() }

// Real modal panel: scrim + tap-outside dismiss + system-back dismiss (Compose
// Dialog routes back to onDismissRequest). The inline shells used to append
// below the scroll flow, which read as "tapped Add and nothing happened".
@Composable
internal fun ModalPanel2e(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = null, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .clickable(interactionSource = null, indication = null) { /* swallow taps */ },
            ) {
                content()
            }
        }
    }
}

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
    ModalPanel2e(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QodexTokens.Border, RoundedCornerShape(12.dp))
                .background(QodexTokens.Surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
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
    onFocusLost: (() -> Unit)? = null,
) {
    var wasFocused by remember { mutableStateOf(false) }
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
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                // Commit-time engine resolution hook (React onBlur flush of model config resolution).
                val now = state.isFocused
                if (wasFocused && !now) onFocusLost?.invoke()
                wasFocused = now
            },
    )
}

@Composable
internal fun BooleanOptionChip2e(
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
    // Tap-to-edit + explicit reordering mirror the desktop chip editor (drag/click-to-edit/add/delete).
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editValue by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((index, level) in levels.withIndex()) {
                if (editingIndex == index) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .border(1.dp, QodexTokens.Primary, RoundedCornerShape(8.dp))
                            .background(QodexTokens.Input)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.width(120.dp)) {
                            EditorInput2e(
                                value = editValue,
                                onValueChange = { editValue = it },
                                enabled = true,
                                placeholder = "e.g. high",
                            )
                        }
                        GhostIconButton2d(
                            icon = QodexIcons2d.Check,
                            size = 28.dp,
                            contentSize = 14.dp,
                            contentDescription = "Confirm reasoning level edit",
                            background = Color.Transparent,
                            rounded = 6.dp,
                            onClick = {
                                val trimmed = editValue.trim()
                                if (trimmed.isNotEmpty() && !levels.filterIndexed { i, _ -> i != index }.contains(trimmed)) {
                                    onChange(levels.mapIndexed { i, value -> if (i == index) trimmed else value })
                                }
                                editingIndex = null
                            },
                        )
                        GhostIconButton2d(
                            icon = QodexIcons2d.Close,
                            size = 28.dp,
                            contentSize = 14.dp,
                            contentDescription = "Cancel reasoning level edit",
                            background = Color.Transparent,
                            rounded = 6.dp,
                            onClick = { editingIndex = null },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
                            .background(QodexTokens.Input)
                            .clickable(enabled = enabled) {
                                editingIndex = index
                                editValue = level
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = level,
                            color = QodexTokens.Foreground,
                            fontSize = 13.sp,
                        )
                        if (enabled) {
                            GhostIconButton2d(
                                icon = QodexIcons.ArrowUp,
                                size = 20.dp,
                                contentSize = 11.dp,
                                contentDescription = "Move reasoning level up: $level",
                                background = Color.Transparent,
                                rounded = 4.dp,
                                enabled = index > 0,
                                onClick = {
                                    val next = levels.toMutableList()
                                    next[index] = next[index - 1].also { next[index - 1] = next[index] }
                                    onChange(next)
                                },
                            )
                            GhostIconButton2d(
                                icon = QodexIcons.ChevronDown,
                                size = 20.dp,
                                contentSize = 11.dp,
                                contentDescription = "Move reasoning level down: $level",
                                background = Color.Transparent,
                                rounded = 4.dp,
                                enabled = index < levels.size - 1,
                                onClick = {
                                    val next = levels.toMutableList()
                                    next[index] = next[index + 1].also { next[index + 1] = next[index] }
                                    onChange(next)
                                },
                            )
                            if (levels.size > 1) {
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
    ModalPanel2e(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QodexTokens.Border, RoundedCornerShape(12.dp))
                .background(QodexTokens.Surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 480.dp)
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
