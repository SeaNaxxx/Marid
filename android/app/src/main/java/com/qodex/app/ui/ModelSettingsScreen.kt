package com.qodex.app.ui

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qodex.app.icons.QodexIcons
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.asBoolOrNull
import com.qodex.app.rpc.asObjectOrNull
import com.qodex.app.rpc.asStringOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG_2D = "QodexModelSettings"

/** Provider auth surface — API_KEY = editable key form, OAUTH = login-based (not rendered). */
enum class ProviderAuthKind2d { API_KEY, OAUTH }

// Model rows come from the engine's ProviderSettingsProviderView.models; effectiveConfig feeds the editor defaults.
// builtinConfig/personalConfig replicate desktop inherited-vs-personal resolution for the sparse overlay.
data class ModelView2d(
    val modelId: String,
    val builtin: Boolean,
    val enabled: Boolean,
    val executable: Boolean,
    val useRecommendedConfig: Boolean,
    val effectiveConfig: JsonObject?,
    val builtinConfig: JsonObject?,
    val personalConfig: JsonObject?,
)

data class ProviderEntry2d(
    val providerId: String,
    val name: String,
    val category: String,
    val authKind: ProviderAuthKind2d,
    val configured: Boolean,
    val enabled: Boolean,
    val executable: Boolean,
    val templateId: String?,
    val apiKeyManagementUrl: String?,
    val apiType: String?,
    val baseUrl: String?,
    val models: List<ModelView2d>,
    val personalConfig: JsonObject?,
)

data class ProviderTemplateEntry2d(
    val templateId: String,
    val name: String,
    val category: String,
    val accessType: String?,
    val apiKeyManagementUrl: String?,
)

data class ModelSettingsData2d(
    val revision: Long,
    val providers: List<ProviderEntry2d>,
    val templates: List<ProviderTemplateEntry2d>,
)

private val ZHIPU_TEMPLATE_IDS_2D =
    listOf("bigmodel-api", "zai-api", "bigmodel-standard-api", "zai-standard-api")

// zai-api is login-based and intentionally absent; this is the only hardcoded template filter.
private fun isZaiCodingPlan2d(templateId: String?, name: String?): Boolean =
    templateId == "zai-api" || name == "Z.ai Coding Plan"

private fun isApiKeyAccessType2d(type: String?): Boolean =
    type == "api-key" || type == "zhipu-coding-plan-api-key"

private fun templateLocale2d(): String =
    if (java.util.Locale.getDefault().language == "zh") "zh-CN" else "en-US"

private fun resolveTemplateName2d(nameMap: JsonObject?, templateId: String): String {
    val locale = templateLocale2d()
    return nameMap?.get(locale).asStringOrNull()?.trim().takeUnless { it.isNullOrEmpty() }
        ?: nameMap?.get("en-US").asStringOrNull()?.trim().takeUnless { it.isNullOrEmpty() }
        ?: templateId
}

// Provider/catalog data comes from the engine view; no hardcoded catalog fallback.
suspend fun fetchProviders(rpc: QodexRpcClient?): ModelSettingsData2d {
    if (rpc == null) {
        Log.w(TAG_2D, "RPC client unavailable; provider list not fetched")
        return ModelSettingsData2d(-1, emptyList(), emptyList())
    }
    val view = try {
        withTimeout(15_000) { rpc.call("provider-settings", "getView", null) }
    } catch (t: Throwable) {
        Log.w(TAG_2D, "provider-settings getView failed: ${t.message}")
        throw t
    }
    val obj = view.asObjectOrNull()
    if (obj == null) {
        Log.w(TAG_2D, "provider-settings getView returned unexpected shape: $view")
        return ModelSettingsData2d(-1, emptyList(), emptyList())
    }

    val locale = templateLocale2d()

    val templates = mutableListOf<ProviderTemplateEntry2d>()
    val templateArray = (obj["providerTemplates"] as? JsonArray)?.toList() ?: emptyList()
    for (element in templateArray) {
        val t = element.asObjectOrNull() ?: continue
        val templateId = t["templateId"].asStringOrNull() ?: continue
        val nameMap = t["templateNameMap"].asObjectOrNull()
        val name = resolveTemplateName2d(nameMap, templateId)
        if (isZaiCodingPlan2d(templateId, name)) continue // hard rule: login-based removal
        val config = t["config"].asObjectOrNull()
        val access = config?.get("access").asObjectOrNull()
        val category = if (templateId in ZHIPU_TEMPLATE_IDS_2D) "zhipu" else "other"
        templates.add(
            ProviderTemplateEntry2d(
                templateId = templateId,
                name = name,
                category = category,
                accessType = access?.get("type").asStringOrNull(),
                apiKeyManagementUrl = access?.get("apiKeyManagementUrl").asStringOrNull(),
            ),
        )
    }

    val providers = mutableListOf<ProviderEntry2d>()
    val providerArray = (obj["providers"] as? JsonArray)?.toList() ?: emptyList()
    for (element in providerArray) {
        val p = element.asObjectOrNull() ?: continue
        val providerId = p["providerId"].asStringOrNull() ?: continue
        val templateId = p["templateId"].asStringOrNull()
        val providerName = p["providerName"].asStringOrNull()?.trim()
        val name = providerName.takeUnless { it.isNullOrEmpty() } ?: providerId
        if (isZaiCodingPlan2d(templateId, name)) continue // hard rule
        val effective = p["effectiveConfig"].asObjectOrNull()
        val access = effective?.get("access").asObjectOrNull()
        val accessType = access?.get("type").asStringOrNull()
        val authKind =
            if (isApiKeyAccessType2d(accessType)) ProviderAuthKind2d.API_KEY
            else ProviderAuthKind2d.OAUTH
        // OAuth providers render through login UIs on desktop; this screen covers the API-key flow only.
        if (authKind != ProviderAuthKind2d.API_KEY) continue
        val api = effective?.get("api").asObjectOrNull()
        val apiKey = access?.get("apiKey").asStringOrNull()
        providers.add(
            ProviderEntry2d(
                providerId = providerId,
                name = name,
                category = "custom",
                authKind = authKind,
                configured = !apiKey.isNullOrBlank(),
                enabled = p["enabled"].asBoolOrNull() ?: true,
                executable = p["executable"].asBoolOrNull() ?: false,
                templateId = templateId,
                apiKeyManagementUrl = access?.get("apiKeyManagementUrl").asStringOrNull(),
                apiType = api?.get("type").asStringOrNull(),
                baseUrl = api?.get("baseUrl").asStringOrNull(),
                models = parseModels2d(p["models"]),
                personalConfig = p["personalConfig"].asObjectOrNull(),
            ),
        )
    }

    val revision = obj["revision"]?.let { (it as? JsonPrimitive)?.content?.toLongOrNull() } ?: -1L
    return ModelSettingsData2d(revision = revision, providers = providers, templates = templates)
}

// Provider detail save covers name / api format / base URL / API key in one overlay commit, mirroring
// the desktop provider card fields. A blank API key input never overwrites the stored credential
//（React 逐字段 idle-save，仅在用户输入后提交该字段）。
suspend fun saveProviderDetails2e(
    rpc: QodexRpcClient,
    providerId: String,
    apiKeyInput: String,
    baseUrlInput: String,
    apiType: String,
    providerName: String?,
    nameChanged: Boolean,
): Boolean {
    val view = try {
        withTimeout(15_000) { rpc.call("provider-settings", "getView", null) }
    } catch (t: Throwable) {
        Log.w(TAG_2D, "saveProviderDetails getView failed: ${t.message}")
        return false
    }
    val obj = view.asObjectOrNull()
    if (obj == null) {
        Log.w(TAG_2D, "saveProviderDetails: unexpected view shape")
        return false
    }
    val provider = (obj["providers"] as? JsonArray)?.toList()
        ?.firstNotNullOfOrNull { it.asObjectOrNull()?.takeIf { p -> p["providerId"].asStringOrNull() == providerId } }
    if (provider == null) {
        Log.w(TAG_2D, "saveProviderDetails: provider $providerId not in view")
        return false
    }
    val effective = provider["effectiveConfig"].asObjectOrNull()
    val effectiveAccess = effective?.get("access").asObjectOrNull()
    val accessType = effectiveAccess?.get("type").asStringOrNull()
    if (!isApiKeyAccessType2d(accessType)) {
        Log.w(TAG_2D, "saveProviderDetails: provider $providerId is not an API-key provider")
        return false
    }
    val personal = provider["personalConfig"].asObjectOrNull() ?: JsonObject(emptyMap())
    val personalAccess = personal["access"].asObjectOrNull()
    val accessPatch = LinkedHashMap<String, JsonElement>()
    personalAccess?.forEach { (key, value) ->
        if (key != "type" && key != "apiKey") accessPatch[key] = value
    }
    // Only a typed (non-blank) key is written; clearing the field does not wipe the credential.
    if (apiKeyInput.isNotBlank()) {
        accessPatch["apiKey"] = JsonPrimitive(apiKeyInput)
    }
    accessPatch["type"] = JsonPrimitive(accessType!!)
    val personalApi = personal["api"].asObjectOrNull()
    val apiPatch = LinkedHashMap<String, JsonElement>()
    personalApi?.forEach { (key, value) ->
        if (key != "type" && key != "baseUrl") apiPatch[key] = value
    }
    apiPatch["type"] = JsonPrimitive(apiType)
    val baseUrl = baseUrlInput.trim()
    if (baseUrl.isNotEmpty()) apiPatch["baseUrl"] = JsonPrimitive(baseUrl)
    val config = LinkedHashMap<String, JsonElement>(personal)
    config["access"] = JsonObject(accessPatch)
    config["api"] = JsonObject(apiPatch)
    val args = mutableListOf<JsonElement>(JsonPrimitive(providerId), JsonObject(config))
    // savePersonalProviderOverlay 的第三个可选参数是 metadata：providerName 只在改名时提交。
    if (nameChanged && !providerName.isNullOrBlank()) {
        args.add(buildJsonObject { put("providerName", providerName.trim()) })
    }
    return try {
        withTimeout(20_000) {
            callPositional2d(rpc, "provider-settings", "savePersonalProviderOverlay", args)
        } != null
    } catch (t: Throwable) {
        Log.w(TAG_2D, "savePersonalProviderOverlay failed: ${t.message}")
        false
    }
}

suspend fun deletePersonalProvider2e(rpc: QodexRpcClient, providerId: String): Boolean = try {
    withTimeout(20_000) {
        callPositional2d(
            rpc,
            "provider-settings",
            "deletePersonalProvider",
            listOf(JsonPrimitive(providerId)),
        )
    } != null
} catch (t: Throwable) {
    Log.w(TAG_2D, "deletePersonalProvider failed: ${t.message}")
    false
}

suspend fun createProviderFromTemplate2d(
    rpc: QodexRpcClient,
    templateId: String?,
    providerName: String?,
): String? {
    val input = buildJsonObject {
        templateId?.let { put("templateId", it) }
        providerName?.let { put("providerName", it) }
        put("locale", templateLocale2d())
    }
    val response = try {
        withTimeout(20_000) {
            callPositional2d(
                rpc,
                "provider-settings",
                "createPersonalProvider",
                listOf(input),
            )
        }
    } catch (t: Throwable) {
        Log.w(TAG_2D, "createPersonalProvider failed: ${t.message}")
        return null
    }
    return response.asObjectOrNull()?.get("providerId").asStringOrNull()
}

// Wire arg for positional service methods is a JSON array — the engine's ProxyChannel.fromService spreads it across the method signature.
private suspend fun callPositional2d(
    rpc: QodexRpcClient,
    channel: String,
    command: String,
    args: List<JsonElement>,
): JsonElement? = rpc.callPositional(channel, command, args)

private fun parseModels2d(raw: JsonElement?): List<ModelView2d> {
    val arr = (raw as? JsonArray)?.toList() ?: return emptyList()
    val models = mutableListOf<ModelView2d>()
    for (element in arr) {
        val m = element.asObjectOrNull() ?: continue
        val id = m["modelId"].asStringOrNull() ?: continue
        models.add(
            ModelView2d(
                modelId = id,
                builtin = m["builtin"].asBoolOrNull() ?: false,
                enabled = m["enabled"].asBoolOrNull() ?: true,
                executable = m["executable"].asBoolOrNull() ?: false,
                useRecommendedConfig = m["useRecommendedConfig"].asBoolOrNull() ?: true,
                effectiveConfig = m["effectiveConfig"].asObjectOrNull(),
                builtinConfig = m["effectiveBuiltinConfig"].asObjectOrNull(),
                personalConfig = m["personalExactConfig"].asObjectOrNull(),
            ),
        )
    }
    return models
}

private sealed interface ModelSettingsUiState2d {
    data object Loading : ModelSettingsUiState2d
    data class Ready(val data: ModelSettingsData2d) : ModelSettingsUiState2d
    data class Error(val message: String) : ModelSettingsUiState2d
}

@Composable
fun ModelSettingsScreen(
    rpc: com.qodex.app.rpc.QodexRpcClient?,
    workspacePath: String,
    onBack: () -> Unit,
) {
    var uiState by remember { mutableStateOf<ModelSettingsUiState2d>(ModelSettingsUiState2d.Loading) }
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var loadedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // System back mirrors the visible affordance: close the provider detail
    // first, otherwise leave this screen. Default activity back (app exit)
    // from a nested settings screen read as "no back button".
    BackHandler {
        if (selectedProviderId != null) {
            selectedProviderId = null
        } else {
            onBack()
        }
    }

    fun reload(reset: Boolean = false) {
        scope.launch {
            // In-place refresh by default: the full-screen Loading flash after a
            // save kicked the user back to the provider list mid-edit.
            if (reset || !loadedOnce) uiState = ModelSettingsUiState2d.Loading
            uiState = try {
                ModelSettingsUiState2d.Ready(fetchProviders(rpc))
            } catch (t: Throwable) {
                ModelSettingsUiState2d.Error(t.message ?: "RPC error")
            }
            loadedOnce = true
        }
    }

    fun launchCreate(templateId: String?, providerName: String?) {
        scope.launch {
            creating = true
            val id = rpc?.let { createProviderFromTemplate2d(it, templateId, providerName) }
            creating = false
            if (id != null) {
                uiState = try {
                    ModelSettingsUiState2d.Ready(fetchProviders(rpc))
                } catch (t: Throwable) {
                    ModelSettingsUiState2d.Error(t.message ?: "RPC error")
                }
                selectedProviderId = id
            }
        }
    }

    LaunchedEffect(rpc) { reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QodexTokens.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                .widthInMax4xl(),
            verticalArrangement = Arrangement.spacedBy(32.dp), // flex flex-col gap-8
        ) {
            val state = uiState
            val selectedProvider = (state as? ModelSettingsUiState2d.Ready)?.data?.providers
                ?.firstOrNull { it.providerId == selectedProviderId }
            if (selectedProvider != null) {
                ApiKeyFormScreen2d(
                    provider = selectedProvider,
                    savingLocked = creating,
                    revision = (state as ModelSettingsUiState2d.Ready).data.revision,
                    workspacePath = workspacePath,
                    onBackToList = { selectedProviderId = null },
                    onSaved = { reload() },
                    onModelChanged = { reload() },
                    rpc = rpc,
                )
            } else {
                ListHeader2d(
                    state = state,
                    creating = creating,
                    onBack = onBack,
                    onRetry = { reload() },
                    onOpenPicker = { pickerOpen = true },
                    onAddTemplate = ::launchCreate,
                    onSelectProvider = { selectedProviderId = it.providerId },
                )
            }
        }
    }

    if (pickerOpen && uiState is ModelSettingsUiState2d.Ready) {
        TemplatePickerDialog2d(
            templates = (uiState as ModelSettingsUiState2d.Ready).data.templates,
            enabled = !creating,
            onDismiss = { pickerOpen = false },
            onPick = { templateId, providerName ->
                pickerOpen = false
                launchCreate(templateId, providerName)
            },
        )
    }
}

private fun Modifier.widthInMax4xl(): Modifier = this.then(
    Modifier.fillMaxWidth(),
)

@Composable
private fun ListHeader2d(
    state: ModelSettingsUiState2d,
    creating: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenPicker: () -> Unit,
    onAddTemplate: (templateId: String?, providerName: String?) -> Unit,
    onSelectProvider: (ProviderEntry2d) -> Unit,
) {
    SectionTitleBackRow2d(title = "Model settings", onBack = onBack)
    when (state) {
        is ModelSettingsUiState2d.Error -> ErrorState2d(state.message, onRetry)
        is ModelSettingsUiState2d.Loading -> {
            DescriptionRow2d(onRefresh = onRetry, onAddProvider = onOpenPicker)
            LoadingCard2d("Loading...")
        }
        is ModelSettingsUiState2d.Ready -> {
            DescriptionRow2d(onRefresh = onRetry, onAddProvider = onOpenPicker)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProvidersSection2d(state.data.providers, onSelectProvider)
                AddProviderSection2d(state.data.templates, creating, onAddTemplate)
            }
        }
    }
}

@Composable
private fun SectionTitleBackRow2d(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GhostIconButton2d(
            icon = QodexIcons2d.ArrowLeft,
            size = 40.dp, // max-lg:size-10 back button
            contentSize = 16.dp, // ArrowLeft size-4
            contentDescription = "Back to workspace",
            background = QodexTokens.Surface, // bg-surface
            rounded = 12.dp, // rounded-xl
            onClick = onBack,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = QodexTokens.Foreground,
            fontSize = 24.sp, // text-ui-settings-title
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp, // tracking-tight (-0.025em @ 24sp)
        )
    }
}

@Composable
private fun DescriptionRow2d(onRefresh: () -> Unit, onAddProvider: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { // gap-3
        Text(
            text = "Manage custom model providers. Once configured, they can be selected during chat.",
            color = QodexTokens.ForegroundSubtle,
            fontSize = 14.sp, // text-ui-base
            lineHeight = 24.sp, // leading-6
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp), // actions gap-2
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostIconButton2d(
                icon = QodexIcons2d.RefreshCcw,
                size = 28.dp, // icon-md size-7
                contentSize = 16.dp, // [&_svg]:size-4
                contentDescription = "Refresh provider list from engine",
                background = Color.Transparent,
                border = QodexTokens.Border, // outline variant: border-border
                rounded = 8.dp, // icon-md rounded-lg
                onClick = onRefresh,
            )
            PrimaryButton2d(label = "Add provider", icon = QodexIcons.Plus, onClick = onAddProvider)
        }
    }
}

@Composable
private fun LoadingCard2d(label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(12.dp)) // rounded-xl border-border
            .background(QodexTokens.Surface) // bg-surface
            .padding(12.dp), // p-3
        verticalArrangement = Arrangement.spacedBy(8.dp), // gap-2
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpinningLoader2d(16.dp, QodexTokens.ForegroundSubtle) // size-4 text-foreground-subtle
            Text(label, color = QodexTokens.ForegroundSubtle, fontSize = 14.sp) // text-ui-base
        }
    }
}

@Composable
private fun SpinningLoader2d(size: androidx.compose.ui.unit.Dp, color: Color) {
    val transition = rememberInfiniteTransition(label = "loader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "loaderAngle",
    )
    val rotation = Modifier.rotate(angle)
    Icon(
        imageVector = QodexIcons.LoaderCircle,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(size)
            .then(rotation),
    )
}

@Composable
private fun ErrorState2d(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 256.dp), // min-h-64
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically), // gap-3
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = QodexTokens.Destructive, fontSize = 14.sp) // text-destructive
        OutlineButton2d(label = "Retry", onClick = onRetry) // variant outline, common.retry
    }
}

@Composable
private fun ProvidersSection2d(
    providers: List<ProviderEntry2d>,
    onSelectProvider: ((ProviderEntry2d) -> Unit)?,
) {
    if (providers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // group: space-y-2 (gap-2 rows)
        Row(
            modifier = Modifier
                .height(28.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Providers", // settings.modelProvider.customTitle → mobile section title
                color = QodexTokens.ForegroundSubtlest,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (provider in providers) {
                ProviderRow2d(provider, onSelectProvider)
            }
        }
    }
}

@Composable
private fun ProviderRow2d(provider: ProviderEntry2d, onSelectProvider: ((ProviderEntry2d) -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // h-8
            .border(1.dp, Color.Transparent, RoundedCornerShape(8.dp)) // rounded-lg border
            .background(Color.Transparent)
            .clickable(enabled = onSelectProvider != null) { onSelectProvider?.invoke(provider) }
            .padding(horizontal = 8.dp, vertical = 4.dp), // px-2 py-1
        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Provider logos are desktop-only assets; the generic Package icon is the deliberate fallback.
        Icon(
            imageVector = QodexIcons.Package,
            contentDescription = null,
            tint = QodexTokens.Foreground,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = provider.name,
            color = QodexTokens.Foreground, // text-foreground
            fontSize = 14.sp, // text-ui-base
            fontWeight = FontWeight.Medium, // font-medium
            maxLines = 1,
            overflow = TextOverflow.Ellipsis, // truncate
            modifier = Modifier.weight(1f),
        )
        val dotColor = when {
            !provider.enabled -> QodexTokens.ForegroundSubtlest
            provider.executable -> QodexTokens.Success
            else -> QodexTokens.Warning
        }
        Box(
            modifier = Modifier
                .size(8.dp) // size-2
                .background(dotColor, RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun AddProviderSection2d(
    templates: List<ProviderTemplateEntry2d>,
    creating: Boolean,
    onAddTemplate: (templateId: String?, providerName: String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // space-y-4 wrapper
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // space-y-3 group section
            Text(
                "Zhipu", // settings.modelProvider.templateGroup.zhipu
                color = QodexTokens.ForegroundSubtle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            val zhipu = templates.filter { it.category == "zhipu" }
            for (template in zhipu) {
                TemplateCard2d(
                    label = template.name,
                    custom = false,
                    enabled = !creating,
                    onClick = { onAddTemplate(template.templateId, null) },
                )
            }
            TemplateCard2d(
                label = "Create custom provider",
                custom = true,
                enabled = !creating,
                onClick = { onAddTemplate(null, "New provider") }, // newProviderName i18n
            )
            val others = templates.filter { it.category == "other" }
            for (template in others) {
                TemplateCard2d(
                    label = template.name,
                    custom = false,
                    enabled = !creating,
                    onClick = { onAddTemplate(template.templateId, null) },
                )
            }
        }
    }
}

@Composable
private fun TemplateCard2d(label: String, custom: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp) // min-h-16
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp)) // rounded-lg border-border
            .background(QodexTokens.Surface) // bg-surface
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // px-4 py-3
        horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (custom) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(QodexTokens.SurfaceHover, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = QodexIcons.Plus,
                    contentDescription = null,
                    tint = QodexTokens.Foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            // Template logos are desktop-only assets; the generic fallback icon renders here.
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = QodexIcons.Package,
                    contentDescription = null,
                    tint = QodexTokens.Foreground,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = label,
            color = QodexTokens.Foreground,
            fontSize = 14.sp, // text-ui-base
            fontWeight = FontWeight.Medium, // font-medium
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = QodexIcons.ChevronRight,
            contentDescription = null,
            tint = QodexTokens.ForegroundSubtlest, // text-foreground-subtlest
            modifier = Modifier.size(16.dp), // size-4
        )
    }
}

@Composable
private fun ApiKeyFormScreen2d(
    provider: ProviderEntry2d,
    savingLocked: Boolean,
    revision: Long,
    workspacePath: String,
    onBackToList: () -> Unit,
    onSaved: () -> Unit,
    onModelChanged: () -> Unit,
    rpc: QodexRpcClient?,
) {
    var apiKey by rememberSaveable(provider.providerId) { mutableStateOf<String?>(null) }
    var visible by rememberSaveable(provider.providerId) { mutableStateOf(false) }
    var providerName by rememberSaveable(provider.providerId) { mutableStateOf(provider.name) }
    var baseUrl by rememberSaveable(provider.providerId) { mutableStateOf(provider.baseUrl ?: "") }
    var apiType by rememberSaveable(provider.providerId) { mutableStateOf(provider.apiType ?: "openai-chat-completions") }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }
    var modelEditorTarget by remember { mutableStateOf<ModelEditorTarget2d?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(3_500) // ProviderDetailFeedback success durationMs (3500)
            savedFlash = false
        }
    }

    val currentValue = apiKey ?: ""
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { // space-y-5
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostIconButton2d(
                icon = QodexIcons2d.ArrowLeft,
                size = 24.dp, // icon-sm size-6
                contentSize = 16.dp, // ArrowLeftIcon size-4
                contentDescription = "Back to provider details",
                background = Color.Transparent,
                rounded = 6.dp,
                onClick = onBackToList,
            )
            Text(
                text = provider.name,
                color = QodexTokens.Foreground,
                fontSize = 16.sp, // text-ui-lg
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            GhostIconButton2d(
                icon = QodexIcons2d.Trash,
                size = 28.dp,
                contentSize = 14.dp,
                contentDescription = "Delete provider", // deleteProvider
                background = Color.Transparent,
                rounded = 6.dp,
                tint = QodexTokens.Destructive,
                enabled = rpc != null && !saving,
                onClick = { confirmingDelete = true },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = QodexIcons.Package,
                    contentDescription = null,
                    tint = QodexTokens.Foreground,
                    modifier = Modifier.size(20.dp), // logo size-5
                )
                Text(
                    text = provider.name,
                    color = QodexTokens.Foreground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val dotColor = when {
                    !provider.enabled -> QodexTokens.ForegroundSubtlest
                    provider.executable -> QodexTokens.Success
                    else -> QodexTokens.Warning
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, RoundedCornerShape(50)),
                )
            }

            EditableField2d(
                label = "Provider name", // settings.modelProvider.name
                value = providerName,
                enabled = !saving && !savingLocked,
                onValueChange = {
                    providerName = it
                    saveFailed = false
                    savedFlash = false
                },
            )
            EditableField2d(
                label = "Base URL", // settings.modelProvider.baseUrl
                value = baseUrl,
                enabled = !saving && !savingLocked,
                monospace = true,
                onValueChange = {
                    baseUrl = it
                    saveFailed = false
                    savedFlash = false
                },
            )
            Column {
                Text(
                    text = "API format", // settings.modelProvider.apiFormat
                    color = QodexTokens.ForegroundSubtle,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (format in PROVIDER_API_FORMATS_2D) {
                        BooleanOptionChip2e(
                            label = format.second,
                            selected = apiType == format.first,
                            enabled = !saving && !savingLocked,
                            onToggle = {
                                apiType = format.first
                                saveFailed = false
                                savedFlash = false
                            },
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "API key", // settings.modelProvider.apiKey
                    color = QodexTokens.ForegroundSubtle, // text-foreground-subtle
                    fontSize = 14.sp, // text-ui-base
                    modifier = Modifier.padding(bottom = 4.dp), // mb-1
                )
                ApiKeyField2d(
                    value = currentValue,
                    visible = visible,
                    enabled = !saving && !savingLocked,
                    onValueChange = {
                        apiKey = it
                        saveFailed = false
                        savedFlash = false
                    },
                    onToggleVisibility = { visible = !visible },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp), // actions gap-2
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryButton2d(
                    label = if (saving) "Saving..." else "Save", // common.saving / common.save
                    icon = null,
                    enabled = !saving && !savingLocked,
                    onClick = {
                        val client = rpc ?: return@PrimaryButton2d
                        scope.launch {
                            saving = true
                            saveFailed = false
                            val ok = saveProviderDetails2e(
                                client,
                                provider.providerId,
                                currentValue,
                                baseUrl,
                                apiType,
                                providerName,
                                providerName.trim() != provider.name,
                            )
                            saving = false
                            if (ok) {
                                savedFlash = true
                                onSaved()
                            } else {
                                saveFailed = true
                            }
                        }
                    },
                )
            }
            if (savedFlash) {
                FeedbackBanner2d(
                    message = "${provider.name} saved", // providerSaveSuccess "{provider} saved"
                    failure = false,
                )
            }
            if (saveFailed) {
                FeedbackBanner2d(
                    message = "Failed to save ${provider.name}", // providerSaveFailure (no error detail)
                    failure = true,
                )
            }
        }

        ModelsSection2d(
            provider = provider,
            revision = revision,
            rpc = rpc,
            workspacePath = workspacePath,
            onEditModel = { model -> modelEditorTarget = ModelEditorTarget2d.Edit(model) },
            onAddModel = { modelEditorTarget = ModelEditorTarget2d.Add },
        )
    }

    if (confirmingDelete) {
        DeleteProviderConfirm2d(
            providerName = provider.name,
            onDismiss = { confirmingDelete = false },
            onConfirm = {
                val client = rpc ?: run {
                    saveFailed = true
                    confirmingDelete = false
                    return@DeleteProviderConfirm2d
                }
                scope.launch {
                    confirmingDelete = false
                    val ok = deletePersonalProvider2e(client, provider.providerId)
                    if (ok) {
                        onBackToList()
                        onSaved()
                    } else {
                        saveFailed = true
                    }
                }
            },
        )
    }

    modelEditorTarget?.let { target ->
        ModelEditorDialog2d(
            provider = provider,
            target = target,
            revision = revision,
            rpc = rpc,
            onDismiss = { modelEditorTarget = null },
            onSaved = {
                modelEditorTarget = null
                onModelChanged()
            },
        )
    }
}

// Desktop ProviderApiFormatSelect: exactly three wire formats, shown with their request paths.
private val PROVIDER_API_FORMATS_2D = listOf(
    "anthropic-messages" to "Anthropic messages (/v1/messages)",
    "openai-chat-completions" to "Chat completions (/chat/completions)",
    "openai-responses" to "Responses (/responses)",
)

@Composable
private fun DeleteProviderConfirm2d(
    providerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ModalPanel2e(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QodexTokens.Destructive, RoundedCornerShape(12.dp))
                .background(QodexTokens.Surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            Text(
                text = "Delete $providerName?", // deleteProviderConfirm
                color = QodexTokens.Foreground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Its models and API key will be removed from the engine config. This cannot be undone.",
                color = QodexTokens.ForegroundSubtle,
                fontSize = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlineButton2d(label = "Cancel", onClick = onDismiss) // common.cancel
                OutlineButton2d(label = "Delete", onClick = onConfirm) // common.delete
            }
            }
        }
    }
}

@Composable
private fun EditableField2d(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    monospace: Boolean = false,
) {
    Column {
        Text(
            text = label,
            color = QodexTokens.ForegroundSubtle,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp), // mb-1
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            textStyle = TextStyle(
                color = QodexTokens.Foreground,
                fontSize = 14.sp,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            ),
            cursorBrush = SolidColor(QodexTokens.Foreground),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 36.dp)
                        .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
                        .background(QodexTokens.Input)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            label,
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
}

// Add and Edit share one dialog and one RPC contract; only the draft seeding and the engine command differ.
internal sealed interface ModelEditorTarget2d {
    data object Add : ModelEditorTarget2d
    data class Edit(val model: ModelView2d) : ModelEditorTarget2d
}

@Composable
private fun ApiKeyField2d(
    value: String,
    visible: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            enabled = enabled,
            visualTransformation =
                if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = TextStyle(
                color = QodexTokens.Foreground, // text-foreground
                fontSize = 14.sp, // text-ui-base
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(QodexTokens.Foreground),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp) // h-9 (ApiKeyInput override)
                        .border(
                            1.dp,
                            QodexTokens.Border, // border-input-border = border (white 10%)
                            RoundedCornerShape(8.dp), // rounded-lg
                        )
                        .background(QodexTokens.Input) // bg-input (neutral-800)
                        .padding(start = 12.dp, end = 40.dp), // px-3 + pr-10
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            "Enter API key", // settings.modelProvider.apiKeyPlaceholder
                            color = QodexTokens.ForegroundSubtlest, // placeholder:text-foreground-subtlest
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        GhostIconButton2d(
            icon = if (visible) QodexIcons2d.EyeOff else QodexIcons2d.Eye,
            size = 24.dp, // icon-sm size-6
            contentSize = 14.dp, // size-3.5
            contentDescription = if (visible) "Hide API key" else "Show API key",
            background = Color.Transparent,
            rounded = 6.dp,
            onClick = onToggleVisibility,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp), // right-1.5
        )
    }
}

@Composable
internal fun FeedbackBanner2d(message: String, failure: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (failure) Color(0x4DFB2C36) else Color(0x4D00C950), // border-destructive/30 | border-success/30
                RoundedCornerShape(8.dp),
            )
            .background(
                if (failure) Color(0x1AFB2C36) else Color(0x1A00C950), // bg-destructive/10 | bg-success/10
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (failure) QodexIcons2d.CircleAlert else QodexIcons2d.CircleCheck,
            contentDescription = null,
            tint = if (failure) QodexTokens.Destructive else QodexTokens.Success,
            modifier = Modifier.size(16.dp), // size-4
        )
        Text(
            text = message,
            color = if (failure) QodexTokens.Destructive else QodexTokens.Success,
            fontSize = 12.sp, // text-ui-sm
            fontWeight = FontWeight.Medium, // font-medium
        )
    }
}

@Composable
internal fun PrimaryButton2d(
    label: String,
    icon: ImageVector?,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(min = 28.dp) // h-7
            .background(
                if (enabled) QodexTokens.Primary else QodexTokens.Primary.copy(alpha = 0.5f), // disabled:opacity-50
                RoundedCornerShape(6.dp), // rounded-md (variant override rounded-lg for Add provider)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp), // px-2
        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = QodexTokens.PrimaryForeground,
                modifier = Modifier.size(14.dp), // [&_svg]:size-3.5 default size
            )
        }
        Text(
            text = label,
            color = QodexTokens.PrimaryForeground, // text-primary-foreground
            fontSize = 14.sp, // text-ui-base
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
internal fun OutlineButton2d(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .heightIn(min = 28.dp)
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(6.dp))
            .background(Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = QodexTokens.Foreground.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
internal fun GhostIconButton2d(
    icon: ImageVector,
    size: androidx.compose.ui.unit.Dp,
    contentSize: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    background: Color,
    rounded: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    border: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    tint: Color = QodexTokens.Foreground,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(rounded))
            .border(1.dp, border, RoundedCornerShape(rounded))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.5f), // disabled:opacity-50
            modifier = Modifier.size(contentSize),
        )
    }
}

// Icons missing from QodexIcons, drawn with exact lucide geometry.
object QodexIcons2d {
    private fun builder(name: String) = ImageVector.Builder(
        name = "Qodex2d.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    private fun stroke(builder: ImageVector.Builder, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        builder.path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
            pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
            pathBuilder = block,
        )

    private fun fillCircle(
        bld: ImageVector.Builder,
        cx: Float,
        cy: Float,
        r: Float,
    ) = bld.path(
        fill = SolidColor(Color(0xFF000000)),
        stroke = null,
        pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
    ) {
        val k = r * 0.5523f
        moveTo(cx + r, cy)
        curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
    }

    val ArrowLeft: ImageVector by lazy {
        stroke(builder("arrow-left")) {
            moveTo(12f, 19f)
            lineTo(5f, 12f)
            lineTo(12f, 5f)
        }
        stroke(builder("arrow-left")) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
        }
        builder("arrow-left").build()
    }

    val Eye: ImageVector by lazy {
        val b = builder("eye")
        stroke(b) {
            moveTo(2.062f, 12.348f)
            quadTo(2.062f, 11.152f, 2.062f, 11.652f)
            quadTo(21.938f, 11.152f, 21.938f, 11.652f)
            quadTo(21.938f, 11.848f, 21.938f, 12.348f)
            quadTo(2.062f, 11.848f, 2.062f, 12.348f)
        }
        stroke(b) {
            moveTo(15f, 12f)
            quadTo(9f, 11.5f, 9f, 12f)
            quadTo(15f, 11.5f, 15f, 12f)
        }
        b.build()
    }

    val EyeOff: ImageVector by lazy {
        val b = builder("eye-off")
        stroke(b) {
            moveTo(10.733f, 5.076f)
            quadTo(21.938f, 11.151f, 21.938f, 11.651f)
            quadTo(21.938f, 11.847f, 21.938f, 12.347f)
            quadTo(20.494f, 14.337f, 20.494f, 14.837f)
        }
        stroke(b) {
            moveTo(14.084f, 14.158f)
            quadTo(9.842f, 9.416f, 9.842f, 9.916f)
        }
        stroke(b) {
            moveTo(17.479f, 17.499f)
            quadTo(6.979f, 16.39f, 6.979f, 16.89f)
        }
        stroke(b) {
            moveTo(2f, 2f)
            lineTo(22f, 22f)
        }
        stroke(b) {
            moveTo(2.126f, 8.845f)
            quadTo(7.396f, 3.075f, 7.396f, 3.575f)
        }
        b.build()
    }

    val RefreshCcw: ImageVector by lazy {
        val b = builder("refresh-ccw")
        stroke(b) {
            moveTo(21f, 12f)
            quadTo(12f, 2.5f, 12f, 3f)
            quadTo(5.26f, 5.24f, 5.26f, 5.74f)
            lineTo(3f, 8f)
        }
        stroke(b) {
            moveTo(3f, 3f)
            lineTo(3f, 8f)
            lineTo(8f, 8f)
        }
        stroke(b) {
            moveTo(3f, 12f)
            quadTo(12f, 20.5f, 12f, 21f)
            quadTo(18.74f, 17.76f, 18.74f, 18.26f)
            lineTo(21f, 16f)
        }
        stroke(b) {
            moveTo(16f, 16f)
            lineTo(21f, 16f)
            lineTo(21f, 21f)
        }
        b.build()
    }

    val LockKeyhole: ImageVector by lazy {
        val b = builder("lock-keyhole")
        stroke(b) {
            moveTo(5f, 10f)
            lineTo(19f, 10f)
            quadTo(21f, 11.5f, 21f, 12f)
            lineTo(21f, 20f)
            quadTo(19f, 21.5f, 19f, 22f)
            lineTo(5f, 22f)
            quadTo(3f, 19.5f, 3f, 20f)
            lineTo(3f, 12f)
            quadTo(5f, 9.5f, 5f, 10f)
        }
        stroke(b) {
            moveTo(13f, 16f)
            quadTo(11f, 15.5f, 11f, 16f)
            quadTo(13f, 15.5f, 13f, 16f)
        }
        stroke(b) {
            moveTo(7f, 10f)
            lineTo(7f, 7f)
            quadTo(17f, 6.5f, 17f, 7f)
            lineTo(17f, 10f)
        }
        b.build()
    }

    val CircleCheck: ImageVector by lazy {
        val b = builder("circle-check-big")
        stroke(b) {
            moveTo(21.801f, 10f)
            arcTo(10f, 10f, 0f, true, true, 17f, 3.335f)
        }
        stroke(b) {
            moveTo(9f, 11f)
            lineTo(12f, 14f)
            lineTo(22f, 4f)
        }
        b.build()
    }

    val CircleAlert: ImageVector by lazy {
        val b = builder("circle-alert")
        stroke(b) {
            moveTo(22f, 12f)
            quadTo(2f, 11.5f, 2f, 12f)
            quadTo(22f, 11.5f, 22f, 12f)
        }
        stroke(b) {
            moveTo(12f, 8f)
            lineTo(12f, 12f)
        }
        stroke(b) {
            moveTo(12f, 16f)
            lineTo(12.01f, 16f)
        }
        b.build()
    }

    val Palette: ImageVector by lazy {
        val b = builder("palette")
        stroke(b) {
            moveTo(12f, 22f)
            quadTo(12f, 1.5f, 12f, 2f)
            quadTo(22f, 10.5f, 22f, 11f)
            quadTo(17f, 15.5f, 17f, 16f)
            lineTo(14.75f, 16f)
            quadTo(13.35f, 18.3f, 13.35f, 18.8f)
            lineTo(13.65f, 19.2f)
            quadTo(12.25f, 21.5f, 12.25f, 22f)
        }
        b.path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
        ) {
            moveTo(14f, 6.5f)
            quadTo(13f, 6.0f, 13f, 6.5f)
            quadTo(14f, 6.0f, 14f, 6.5f)
            moveTo(18f, 10.5f)
            quadTo(17f, 10.0f, 17f, 10.5f)
            quadTo(18f, 10.0f, 18f, 10.5f)
            moveTo(7f, 12.5f)
            quadTo(6f, 12.0f, 6f, 12.5f)
            quadTo(7f, 12.0f, 7f, 12.5f)
            moveTo(9f, 7.5f)
            quadTo(8f, 7.0f, 8f, 7.5f)
            quadTo(9f, 7.0f, 9f, 7.5f)
        }
        b.build()
    }

    val Monitor: ImageVector by lazy {
        val b = builder("monitor")
        stroke(b) {
            moveTo(4f, 3f)
            lineTo(20f, 3f)
            quadTo(22f, 4.5f, 22f, 5f)
            lineTo(22f, 15f)
            quadTo(20f, 16.5f, 20f, 17f)
            lineTo(4f, 17f)
            quadTo(2f, 14.5f, 2f, 15f)
            lineTo(2f, 5f)
            quadTo(4f, 2.5f, 4f, 3f)
        }
        stroke(b) {
            moveTo(8f, 21f)
            lineTo(16f, 21f)
        }
        stroke(b) {
            moveTo(12f, 17f)
            lineTo(12f, 21f)
        }
        b.build()
    }

    val Moon: ImageVector by lazy {
        val b = builder("moon")
        stroke(b) {
            moveTo(12f, 3f)
            quadTo(21f, 11.5f, 21f, 12f)
            arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        }
        b.build()
    }

    val Sun: ImageVector by lazy {
        val b = builder("sun")
        stroke(b) {
            moveTo(16f, 12f)
            quadTo(8f, 11.5f, 8f, 12f)
            quadTo(16f, 11.5f, 16f, 12f)
        }
        stroke(b) {
            moveTo(12f, 2f)
            lineTo(12f, 4f)
        }
        stroke(b) {
            moveTo(12f, 20f)
            lineTo(12f, 22f)
        }
        stroke(b) {
            moveTo(4.93f, 4.93f)
            lineTo(6.34f, 6.34f)
        }
        stroke(b) {
            moveTo(17.66f, 17.66f)
            lineTo(19.07f, 19.07f)
        }
        stroke(b) {
            moveTo(2f, 12f)
            lineTo(4f, 12f)
        }
        stroke(b) {
            moveTo(20f, 12f)
            lineTo(22f, 12f)
        }
        stroke(b) {
            moveTo(6.34f, 17.66f)
            lineTo(4.93f, 19.07f)
        }
        stroke(b) {
            moveTo(19.07f, 4.93f)
            lineTo(17.66f, 6.34f)
        }
        b.build()
    }

    val Pencil: ImageVector by lazy {
        val b = builder("pencil")
        stroke(b) {
            moveTo(21.174f, 6.812f)
            lineTo(17.089f, 2.727f)
            quadTo(16.5f, 2.135f, 16.921f, 2.556f)
        }
        stroke(b) {
            moveTo(2.5f, 21.5f)
            lineTo(3.392f, 17.539f)
            quadTo(3.5f, 17.053f, 3.885f, 16.668f)
            lineTo(15.879f, 4.674f)
            quadTo(16.5f, 4.053f, 17.12f, 4.674f)
            lineTo(19.326f, 6.879f)
            quadTo(19.947f, 7.5f, 19.326f, 8.121f)
            lineTo(7.332f, 20.115f)
            quadTo(6.947f, 20.5f, 6.461f, 20.608f)
            lineTo(2.5f, 21.5f)
        }
        stroke(b) {
            moveTo(15f, 5f)
            lineTo(19f, 9f)
        }
        b.build()
    }

    val Trash: ImageVector by lazy {
        val b = builder("trash-2")
        stroke(b) {
            moveTo(3f, 6f)
            lineTo(21f, 6f)
        }
        stroke(b) {
            moveTo(19f, 6f)
            quadTo(20f, 6.5f, 20f, 7f)
            lineTo(20f, 19f)
            quadTo(19f, 21.5f, 19f, 22f)
            lineTo(5f, 22f)
            quadTo(4f, 21.5f, 4f, 21f)
            lineTo(4f, 7f)
            quadTo(4f, 6.5f, 5f, 6f)
        }
        stroke(b) {
            moveTo(8f, 6f)
            lineTo(8f, 4f)
            quadTo(9f, 2.5f, 9f, 2f)
            lineTo(15f, 2f)
            quadTo(16f, 2.5f, 16f, 3f)
            lineTo(16f, 6f)
        }
        stroke(b) {
            moveTo(10f, 11f)
            lineTo(10f, 17f)
        }
        stroke(b) {
            moveTo(14f, 11f)
            lineTo(14f, 17f)
        }
        b.build()
    }

    val Check: ImageVector by lazy {
        val b = builder("check")
        stroke(b) {
            moveTo(20f, 6f)
            lineTo(9f, 17f)
            lineTo(4f, 12f)
        }
        b.build()
    }

    val Close: ImageVector by lazy {
        val b = builder("x")
        stroke(b) {
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
        stroke(b) {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
        }
        b.build()
    }
}
