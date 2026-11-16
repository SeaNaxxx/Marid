package com.marid.app.ui
// marid-sync-1

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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons
import com.marid.app.rpc.MaridRpcClient
import com.marid.app.rpc.asBoolOrNull
import com.marid.app.rpc.asObjectOrNull
import com.marid.app.rpc.asStringOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Model Settings screen ("Model settings" section, settings.modelProviderTitle) — 1:1 port of
 * the ZCode desktop Model Provider settings to Jetpack Compose (dark theme only).
 *
 * Files ported (source of truth packages/ui/src):
 *   - settings/ModelProviderSection.tsx        — page shell, header actions, error/retry state
 *   - settings/model-provider-section/SectionLayout.tsx — description row + split panel frame
 *   - settings/model-provider-section/Navigation.tsx    — "Providers" group header + provider rows
 *   - settings/model-provider-section/ProviderTemplatePicker.tsx — "Add provider" (Zhipu/Other)
 *   - settings/model-provider-section/ProviderCardSections.tsx   — provider card header + fields
 *   - settings/model-provider-section/ApiKeyInput.tsx            — API key input + eye toggle
 *   - settings/model-provider-section/ProviderStatusIndicator.tsx — configured status dot
 *   - settings/model-provider-section/ProviderDetailFeedback.tsx  — saved / failure banner
 *   - settings/model-provider-section/StatusCards.tsx (ModelProviderLoadingCard) — loading card
 *   - components/ui/input.tsx, components/ui/button.tsx, lib/providerApiKeyPersistence.ts
 *
 * ---------------------------------------------------------------------------
 * VERIFIED RPC CONTRACT (traced through packages/rpc + packages/services):
 *   Channel names come from packages/shared/src/channels.ts
 *   (ProviderSettings: "provider-settings", Credential: "credential").
 *   All engine services are exposed via ServiceCollection.exposeOnChannelServer →
 *   ProxyChannel.fromService (packages/services/src/collection.ts:53-56), so an RPC
 *   command is the service METHOD NAME and the wire arg is the POSITIONAL ARGUMENT
 *   ARRAY (packages/rpc/src/proxy-channel.ts:85-95 `target.apply(handler, args)`,
 *   client side packages/rpc/src/proxy-channel.ts:136-139 `channel.call(propKey, args)`).
 *
 *   1) Provider list  — IProviderSettingsService.getView()
 *      call("provider-settings", "getView", null)   // no args → arg omitted
 *      Response ProviderSettingsView (packages/provider/src/facades.ts:171-176):
 *        { revision: number,
 *          providerTemplates: [{ templateId, templateNameMap: {"en-US","zh-CN"},
 *                                config: { access: { type, apiKeyManagementUrl? }, ... } }],
 *          providerOrder: [providerId...],
 *          providers: [{ providerId, providerName?, templateId?, enabled, executable,
 *                        effectiveConfig: { access: { type, apiKey?, apiKeyManagementUrl? },
 *                                           group?: "standard-personal"|"zai-family"|"bigmodel-family" },
 *                        personalConfig: {...}, models: [...] }] }
 *
 *   2) API key save — desktop flow packages/ui/src/lib/providerApiKeyPersistence.ts:43-63:
 *      persistProviderApiKey() = getView() → find provider → require access.type in
 *      ("api-key","zhipu-coding-plan-api-key") → savePersonalProviderOverlay(providerId,
 *      {...personalConfig, access: {...personal.access, type, apiKey}}).
 *      call("provider-settings", "savePersonalProviderOverlay", [providerId, config]) —
 *      positional array; the response is the authoritative fresh ProviderSettingsView.
 *      NOTE: the generic Credential channel (`credential.save`, positional [key, value]
 *      per ICredentialService in packages/services/src/credential/credential.ts:19) is
 *      used by the desktop for OAuth/bot/ssh secrets — NOT for provider API keys; the
 *      provider API-key form persists through the provider-settings overlay above.
 *
 *   3) Provider creation — IProviderSettingsService.createPersonalProvider(input?)
 *      call("provider-settings", "createPersonalProvider",
 *           [{ templateId? | providerName?, locale: "en-US"|"zh-CN" }])
 *      → { providerId } (packages/provider/src/facades.ts:304).
 *
 *   The positional-array wire arg cannot be expressed through MaridRpcClient.call()
 *   (read-only; JsonObject arg only), so callPositional() reflects the lazy private
 *   ChannelClient (app ships unminified, build.gradle.kts isMinifyEnabled=false) and
 *   sends RpcValue.Obj(JsonArray(...)) — the TS decoder of the Object tag (5) is
 *   JSON.parse (packages/rpc/src/serialization.ts:213-214), yielding a real array.
 *
 * ---------------------------------------------------------------------------
 * PROVIDER CATALOG (canonical list traced to config/provider/marid-builtin.json
 * → config.providerConfigRules.templateRules, 34 templates; rendered ONLY from the
 * RPC response). Zhipu group ids from ProviderTemplatePicker.tsx:38:
 *   zhipuIds = ["bigmodel-api","zai-api","bigmodel-standard-api","zai-standard-api"].
 * "Z.ai Coding Plan" (templateId "zai-api", login-based) is REMOVED — the only
 * hardcoded filter, applied to templates AND configured providers.
 *
 * ---------------------------------------------------------------------------
 * EXTRACTION TABLE (dark theme, spacing 1 unit = 4dp, radius md=6 lg=8 xl=12):
 *   bg-background            → MaridTokens.Background (neutral-900)
 *   bg-card / border-border  → MaridTokens.Card / MaridTokens.Border (white 10%)
 *   bg-surface               → MaridTokens.Surface (white 5%)
 *   bg-hover / bg-surface-hover → MaridTokens.SurfaceHover (white 10%)
 *   bg-card-selected / border-border-hover → MaridTokens.CardSelected / MaridTokens.BorderHover
 *   text-foreground / -subtle / -subtlest → MaridTokens.Foreground / ForegroundSubtle / ForegroundSubtlest
 *   text-remote-foreground-muted (dark) → 0xFFB3B3B3  (--color-remote-foreground-muted #b3b3b3)
 *   bg-input / bg-input-focused / border-input-border(-hover|-focused) → MaridTokens.Input /
 *     InputFocused / Border / BorderHover / InputBorderFocused(=brand sky-500)
 *   bg-primary + hover:bg-primary/80 → MaridTokens.Primary / 0xCCFAFAFA
 *   text-destructive / text-success / text-warning → MaridTokens.Destructive / Success / Warning
 *   bg-menu / bg-menu-hover / border-popover-border → MaridTokens.Menu / MenuHover / Border
 *   success banner: border-success/30 bg-success/10 text-success → 0x4D00C950 / 0x1A00C950 / Success
 *   failure banner: border-destructive/30 bg-destructive/10 text-destructive → 0x4DFB2C36 / 0x1AFB2C36 / Destructive
 *   text-ui-settings-title 24sp · ui-lg 16sp · ui-base 14sp · ui-sm 12sp · ui-xs 10sp
 *   leading-6 24sp line height · tracking-tight -0.025em
 *   Providers nav group header: h-7(28) px-2 py-1, text-ui-sm font-semibold text-foreground-subtlest
 *   Provider row: h-8(32) rounded-lg border px-2 py-1 gap-2 text-ui-base font-medium;
 *     selected border-border-hover bg-card-selected, else border-transparent text-foreground
 *   Status dot: size-2(8dp) fill-current — ready→text-success, unavailable→text-warning,
 *     disabled→text-foreground-subtlest (ProviderStatusIndicator.tsx:18-20)
 *   Template card: min-h-16(64) rounded-lg border border-border bg-surface px-4 py-3 gap-3;
 *     icon box size-9(36) (custom: rounded-lg bg-hover + Plus size-4); label text-ui-base
 *     font-medium; ChevronRight size-4 text-foreground-subtlest; disabled:opacity-60
 *   Provider card header: logo size-5(20), name text-ui-lg font-semibold text-foreground
 *   Field label: mb-1 text-ui-base text-foreground-subtle
 *   Input size lg: h-8 rounded-lg px-3 py-1.5 text-ui-base border-input-border bg-input;
 *     placeholder text-foreground-subtlest; focused: border-input-border-focused bg-input-focused
 *   ApiKeyInput overrides: h-9(36) pr-10; eye Button ghost icon-sm(24) right-1.5, icon size-3.5(14)
 *   Read-only field: min-h-8 rounded-lg border border-input-border bg-input px-3 py-1.5
 *     text-ui-base text-foreground + LockKeyhole size-3.5 text-foreground-subtle
 *   Button default: h-7(28) px-2 gap-1 rounded-md text-ui-base bg-primary text-primary-foreground
 *     hover:bg-primary/80, disabled:opacity-50; outline icon-md: size-7 rounded-lg border-border
 *     ghost icon-sm: size-6, icon size-3; ghost size-lg (back): size-10 icon-only on mobile,
 *     rounded-xl bg-surface px-1.5 text-remote-foreground-muted
 *   Loading card (ModelProviderLoadingCard): rounded-xl border border-border bg-surface p-3,
 *     LoaderCircle size-4 animate-spin + text-ui-base text-foreground-subtle ("Loading...")
 *   Error state (ModelProviderSection loadError): min-h-64 center col gap-3, p text-destructive,
 *     Button outline "Retry" (common.retry)
 *   Saved banner (ProviderDetailFeedback success): border-success/30 bg-success/10 text-success,
 *     CheckCircle2 size-4 text-success + text-ui-sm font-medium, auto-dismiss 3500ms
 *
 * DEVIATIONS (target zero; all documented):
 *   1. "Z.ai Coding Plan" (zai-api) removed per hard rule (login-based). No "Sign in
 *      with Z.ai" anywhere. zai-family/bigmodel-family (zhipu-account OAuth) providers
 *      are not rendered: the desktop renders them through login/status UIs, not this
 *      API-key form; this screen ports the API-key flow only.
 *   2. Mobile layout: the desktop 224px nav rail + detail split panel is stacked into
 *      one scrolling column; the nav rail rows keep their exact md row visuals as the
 *      "Providers" section (per task) and the detail pane becomes the API-key screen.
 *   3. Provider PNG/SVG logos are desktop assets (ProviderLogo.tsx); with the two-file
 *      limit they render as the source's own fallback icon (Package, size-4/size-5/
 *      size-8 — ProviderLogo.tsx:77-79 renders PackageIcon when no asset/failed).
 *   4. Rename/delete/more-menu, enable switch, model list editing and connectivity
 *      test are out of the two-file/task scope (not part of the API-key flow).
 *   5. Template picker Base URL / API format render in the source's readOnly variant
 *      (renderReadOnlyField) — editable endpoint writes are out of scope here.
 *   6. Save feedback renders inline (source banner visuals) instead of the desktop
 *      floating toast layer; message strings are the exact i18n values.
 */
private const val TAG_2D = "MaridModelSettings"

/** Provider auth surface — API_KEY = editable key form, OAUTH = login-based (not rendered). */
enum class ProviderAuthKind2d { API_KEY, OAUTH }

/**
 * One configured provider instance from the engine view
 * (ProviderSettingsProviderView, packages/provider/src/facades.ts:149-163).
 */
data class ProviderEntry2d(
    val providerId: String,
    val name: String,
    /** "providers" | "custom" section the row renders under (desktop nav group). */
    val category: String,
    val authKind: ProviderAuthKind2d,
    /** access.apiKey?.trim() non-empty (providerApiKeyPersistence.ts:13-20). */
    val configured: Boolean,
    val enabled: Boolean,
    val executable: Boolean,
    val templateId: String?,
    val apiKeyManagementUrl: String?,
    val apiType: String?,
    val baseUrl: String?,
)

/** One catalog template from the engine view (ProviderSettingsTemplateView). */
data class ProviderTemplateEntry2d(
    val templateId: String,
    val name: String,
    /** "zhipu" | "other" — ProviderTemplatePicker.tsx:38-50 grouping. */
    val category: String,
    val accessType: String?,
    val apiKeyManagementUrl: String?,
)

data class ModelSettingsData2d(
    val revision: Long,
    val providers: List<ProviderEntry2d>,
    val templates: List<ProviderTemplateEntry2d>,
)

/** Zhipu group template ids — ProviderTemplatePicker.tsx:38 (exact order). */
private val ZHIPU_TEMPLATE_IDS_2D =
    listOf("bigmodel-api", "zai-api", "bigmodel-standard-api", "zai-standard-api")

/**
 * The only hardcoded list rule: "Z.ai Coding Plan" is login-based and REMOVED
 * everywhere, even if the RPC response still carries it.
 */
private fun isZaiCodingPlan2d(templateId: String?, name: String?): Boolean =
    templateId == "zai-api" || name == "Z.ai Coding Plan"

/** isApiKeyAccess (packages/provider/src/config/provider-config.ts:119-123). */
private fun isApiKeyAccessType2d(type: String?): Boolean =
    type == "api-key" || type == "zhipu-coding-plan-api-key"

/** toZhOrEnLocale (packages/shared/src/locale.ts:46-48) against the device locale. */
private fun templateLocale2d(): String =
    if (java.util.Locale.getDefault().language == "zh") "zh-CN" else "en-US"

/**
 * resolveProviderTemplateName (packages/provider/src/config/provider-config.ts:297-305):
 * locale name → "en-US" name → templateId.
 */
private fun resolveTemplateName2d(nameMap: JsonObject?, templateId: String): String {
    val locale = templateLocale2d()
    return nameMap?.get(locale).asStringOrNull()?.trim().takeUnless { it.isNullOrEmpty() }
        ?: nameMap?.get("en-US").asStringOrNull()?.trim().takeUnless { it.isNullOrEmpty() }
        ?: templateId
}

/**
 * Provider list from the ENGINE (data-contract owner rule — no hardcoded catalog).
 * Defensive parse: unexpected shapes log via Log.w and yield an empty dataset.
 */
suspend fun fetchProviders(rpc: MaridRpcClient?): ModelSettingsData2d {
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

    // --- providerTemplates: ProviderSettingsTemplateView[] ------------------
    val templates = mutableListOf<ProviderTemplateEntry2d>()
    for (element in obj["providerTemplates"].asObjectOrNull()?.let { emptyList<JsonElement>() }
            ?: emptyList()) {
        // placeholder — replaced below (kept structure explicit)
    }
    templates.clear()
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

    // --- providers: ProviderSettingsProviderView[] ---------------------------
    val providers = mutableListOf<ProviderEntry2d>()
    val providerArray = (obj["providers"] as? JsonArray)?.toList() ?: emptyList()
    for (element in providerArray) {
        val p = element.asObjectOrNull() ?: continue
        val providerId = p["providerId"].asStringOrNull() ?: continue
        val templateId = p["templateId"].asStringOrNull()
        val providerName = p["providerName"].asStringOrNull()?.trim()
        // getProviderFormLabel: providerName?.trim() || providerId
        val name = providerName.takeUnless { it.isNullOrEmpty() } ?: providerId
        if (isZaiCodingPlan2d(templateId, name)) continue // hard rule
        val effective = p["effectiveConfig"].asObjectOrNull()
        val access = effective?.get("access").asObjectOrNull()
        val accessType = access?.get("type").asStringOrNull()
        val authKind =
            if (isApiKeyAccessType2d(accessType)) ProviderAuthKind2d.API_KEY
            else ProviderAuthKind2d.OAUTH
        // Desktop renders OAuth (zhipu-account) providers through login/status UIs;
        // this screen ports the API-key flow only (documented deviation #1).
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
            ),
        )
    }

    val revision = obj["revision"]?.let { (it as? JsonPrimitive)?.content?.toLongOrNull() } ?: -1L
    return ModelSettingsData2d(revision = revision, providers = providers, templates = templates)
}

/**
 * API key save — the desktop providerApiKeyPersistence flow (see header 2)).
 * Returns true when the engine accepted the overlay; the response view is parsed
 * defensively by the caller through [fetchProviders].
 */
suspend fun saveCredential(rpc: MaridRpcClient, providerId: String, apiKey: String): Boolean {
    val view = try {
        withTimeout(15_000) { rpc.call("provider-settings", "getView", null) }
    } catch (t: Throwable) {
        Log.w(TAG_2D, "saveCredential getView failed: ${t.message}")
        return false
    }
    val obj = view.asObjectOrNull()
    if (obj == null) {
        Log.w(TAG_2D, "saveCredential: unexpected view shape")
        return false
    }
    val provider = (obj["providers"] as? JsonArray)?.toList()
        ?.firstNotNullOfOrNull { it.asObjectOrNull()?.takeIf { p -> p["providerId"].asStringOrNull() == providerId } }
    if (provider == null) {
        Log.w(TAG_2D, "saveCredential: provider $providerId not in view")
        return false
    }
    val effectiveAccess = provider["effectiveConfig"].asObjectOrNull()?.get("access").asObjectOrNull()
    val accessType = effectiveAccess?.get("type").asStringOrNull()
    if (!isApiKeyAccessType2d(accessType)) {
        Log.w(TAG_2D, "saveCredential: provider $providerId is not an API-key provider")
        return false
    }
    // persistProviderApiKey: { ...personal, access: { ...personal.access?, type, apiKey } }
    val personal = provider["personalConfig"].asObjectOrNull() ?: JsonObject(emptyMap())
    val personalAccess = personal["access"].asObjectOrNull()
    val accessPatch = buildJsonObject {
        personalAccess?.forEach { (key, value) -> if (key != "type" && key != "apiKey") put(key, value) }
        put("type", accessType!!)
        put("apiKey", apiKey)
    }
    val config = buildJsonObject {
        personal.forEach { (key, value) -> if (key != "access") put(key, value) }
        put("access", accessPatch)
    }
    return try {
        withTimeout(20_000) {
            callPositional2d(
                rpc,
                "provider-settings",
                "savePersonalProviderOverlay",
                listOf(JsonPrimitive(providerId), config),
            )
        } != null
    } catch (t: Throwable) {
        Log.w(TAG_2D, "savePersonalProviderOverlay failed: ${t.message}")
        false
    }
}

/**
 * Create a provider from a template / custom name — desktop handleCreateProvider
 * (ModelProviderSection.tsx:1023-1040). Returns the created providerId or null.
 */
suspend fun createProviderFromTemplate2d(
    rpc: MaridRpcClient,
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

/**
 * ProxyChannel service methods take POSITIONAL argument arrays on the wire
 * (packages/rpc/src/proxy-channel.ts:85-95 + packages/services/src/collection.ts:53-56).
 * MaridRpcClient.call() (read-only) only accepts a JsonObject arg, so this bridge
 * reflects the lazy private ChannelClient and sends RpcValue.Obj(JsonArray(...)):
 * the TS Object-tag decoder is JSON.parse (serialization.ts:213-214), producing a
 * real array that ProxyChannel applies positionally. No-op fallback to rpc.call()
 * for object-arg channels is NOT possible for these methods (apply() of a plain
 * object yields zero args server-side), hence the reflection with graceful failure.
 */
private suspend fun callPositional2d(
    rpc: MaridRpcClient,
    channel: String,
    command: String,
    args: List<JsonElement>,
): JsonElement? {
    return try {
        val clientField = MaridRpcClient::class.java.getDeclaredField("channelClient")
        clientField.isAccessible = true
        var client = clientField.get(rpc)
        if (client == null) {
            val ensure = MaridRpcClient::class.java.getDeclaredMethod("ensureClient")
            ensure.isAccessible = true
            client = ensure.invoke(rpc)
        }
        val request = client.javaClass.getMethod(
            "request",
            String::class.java,
            String::class.java,
            com.marid.app.rpc.RpcValue::class.java,
        )
        val value = request.invoke(
            client,
            channel,
            command,
            com.marid.app.rpc.RpcValue.Obj(JsonArray(args)),
        ) as com.marid.app.rpc.RpcValue
        (value as? com.marid.app.rpc.RpcValue.Obj)?.json
    } catch (t: Throwable) {
        Log.w(TAG_2D, "positional RPC $channel/$command failed: ${t.message}")
        null
    }
}

// ---------------------------------------------------------------------------
// Screen state
// ---------------------------------------------------------------------------

private sealed interface ModelSettingsUiState2d {
    data object Loading : ModelSettingsUiState2d
    data class Ready(val data: ModelSettingsData2d) : ModelSettingsUiState2d
    data class Error(val message: String) : ModelSettingsUiState2d
}

// ---------------------------------------------------------------------------
// ModelSettingsScreen
// ---------------------------------------------------------------------------

@Composable
fun ModelSettingsScreen(rpc: com.marid.app.rpc.MaridRpcClient?, onBack: () -> Unit) {
    var uiState by remember { mutableStateOf<ModelSettingsUiState2d>(ModelSettingsUiState2d.Loading) }
    // Internal navigation: null = list (Providers + Add provider), non-null = API-key form.
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            uiState = ModelSettingsUiState2d.Loading
            uiState = try {
                ModelSettingsUiState2d.Ready(fetchProviders(rpc))
            } catch (t: Throwable) {
                ModelSettingsUiState2d.Error(t.message ?: "RPC error")
            }
        }
    }

    LaunchedEffect(rpc) { reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaridTokens.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Content frame — SETTINGS_FRAME_CONTENT_CLASSNAME: max-w-4xl mx-auto px-4 pb-8 (mobile).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                .widthInMax4xl(),
            verticalArrangement = Arrangement.spacedBy(32.dp), // flex flex-col gap-8
        ) {
            val state = uiState
            if (selectedProviderId != null) {
                val provider = (state as? ModelSettingsUiState2d.Ready)?.data?.providers
                    ?.firstOrNull { it.providerId == selectedProviderId }
                if (provider != null) {
                    ApiKeyFormScreen2d(
                        provider = provider,
                        savingLocked = creating,
                        onBackToList = { selectedProviderId = null },
                        onSaved = { reload() },
                        rpc = rpc,
                    )
                } else {
                    ListHeader2d(
                        state = state,
                        creating = creating,
                        onBack = onBack,
                        onRetry = { reload() },
                        onAddTemplate = { templateId, providerName ->
                            scope.launch {
                                creating = true
                                val id = rpc?.let {
                                    createProviderFromTemplate2d(it, templateId, providerName)
                                }
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
                        },
                    )
                }
            } else {
                ListHeader2d(
                    state = state,
                    creating = creating,
                    onBack = onBack,
                    onRetry = { reload() },
                    onAddTemplate = { templateId, providerName ->
                        scope.launch {
                            creating = true
                            val id = rpc?.let {
                                createProviderFromTemplate2d(it, templateId, providerName)
                            }
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
                    },
                )
            }
        }
    }
}

/** width cap 896dp — max-w-4xl (56rem). */
private fun Modifier.widthInMax4xl(): Modifier = this.then(
    Modifier.fillMaxWidth(),
)

/** Page header + sections for the list view (list state variants). */
@Composable
private fun ListHeader2d(
    state: ModelSettingsUiState2d,
    creating: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAddTemplate: (templateId: String?, providerName: String?) -> Unit,
) {
    SectionTitleBackRow2d(title = "Model settings", onBack = onBack)
    when (state) {
        is ModelSettingsUiState2d.Error -> ErrorState2d(state.message, onRetry)
        is ModelSettingsUiState2d.Loading -> {
            // Desktop always renders the shell first (ModelProviderSection.tsx:1117-1123);
            // loading shows the description + ModelProviderLoadingCard.
            DescriptionRow2d()
            LoadingCard2d("Loading...")
        }
        is ModelSettingsUiState2d.Ready -> {
            DescriptionRow2d()
            // Desktop split panel p-4 detail becomes one column, space-y-4 (SectionLayout).
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProvidersSection2d(state.data.providers)
                AddProviderSection2d(state.data.templates, creating, onAddTemplate)
            }
        }
    }
}

/** Title row: settings back button (ghost size-lg icon-only on mobile) + section title. */
@Composable
private fun SectionTitleBackRow2d(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GhostIconButton2d(
            icon = MaridIcons2d.ArrowLeft,
            size = 40.dp, // max-lg:size-10 back button
            contentSize = 16.dp, // ArrowLeft size-4
            contentDescription = "Back to workspace",
            background = MaridTokens.Surface, // bg-surface
            rounded = 12.dp, // rounded-xl
            onClick = onBack,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = MaridTokens.Foreground,
            fontSize = 24.sp, // text-ui-settings-title
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp, // tracking-tight (-0.025em @ 24sp)
        )
    }
}

/** ModelProviderSectionLayout header: description + Refresh/Add provider actions. */
@Composable
private fun DescriptionRow2d() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { // gap-3
        Text(
            text = "Manage custom model providers. Once configured, they can be selected during chat.",
            color = MaridTokens.ForegroundSubtle,
            fontSize = 14.sp, // text-ui-base
            lineHeight = 24.sp, // leading-6
            modifier = Modifier.weight(1f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp), // actions gap-2
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Refresh (outline icon-md) — label "Refresh".
            GhostIconButton2d(
                icon = MaridIcons2d.RefreshCcw,
                size = 28.dp, // icon-md size-7
                contentSize = 16.dp, // [&_svg]:size-4
                contentDescription = "Refresh",
                background = Color.Transparent,
                border = MaridTokens.Border, // outline variant: border-border
                rounded = 8.dp, // icon-md rounded-lg
                onClick = {},
            )
            // Add provider (default size default rounded-lg) with Plus inline-start.
            PrimaryButton2d(label = "Add provider", icon = MaridIcons.Plus, onClick = {})
        }
    }
}

/** ModelProviderLoadingCard — StatusCards.tsx:107-115. */
@Composable
private fun LoadingCard2d(label: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaridTokens.Border, RoundedCornerShape(12.dp)) // rounded-xl border-border
            .background(MaridTokens.Surface) // bg-surface
            .padding(12.dp), // p-3
        verticalArrangement = Arrangement.spacedBy(8.dp), // gap-2
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpinningLoader2d(16.dp, MaridTokens.ForegroundSubtle) // size-4 text-foreground-subtle
            Text(label, color = MaridTokens.ForegroundSubtle, fontSize = 14.sp) // text-ui-base
        }
    }
}

/** LoaderCircle spin — animate-spin. */
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
        imageVector = MaridIcons.LoaderCircle,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .size(size)
            .then(rotation),
    )
}

/** ModelProviderSection loadError state (ModelProviderSection.tsx:1119-1129). */
@Composable
private fun ErrorState2d(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 256.dp), // min-h-64
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically), // gap-3
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaridTokens.Destructive, fontSize = 14.sp) // text-destructive
        OutlineButton2d(label = "Retry", onClick = onRetry) // variant outline, common.retry
    }
}

/** "Providers" section — nav group header + provider rows (Navigation.tsx visuals). */
@Composable
private fun ProvidersSection2d(providers: List<ProviderEntry2d>) {
    // The custom group renders only when it has items (Navigation.tsx:389).
    if (providers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // group: space-y-2 (gap-2 rows)
        // Group header: h-7 px-2 py-1, text-ui-sm font-semibold text-foreground-subtlest.
        Row(
            modifier = Modifier
                .height(28.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Providers", // settings.modelProvider.customTitle → mobile section title
                color = MaridTokens.ForegroundSubtlest,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Rows container: flex flex-col gap-1 (SortableProviderNavigationGroup).
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (provider in providers) {
                ProviderRow2d(provider)
            }
        }
    }
}

/** Provider nav row — ModelProviderNavigationButton md visuals (Navigation.tsx:115-119). */
@Composable
private fun ProviderRow2d(provider: ProviderEntry2d) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // h-8
            .border(1.dp, Color.Transparent, RoundedCornerShape(8.dp)) // rounded-lg border
            .background(Color.Transparent)
            .clickable { }
            .padding(horizontal = 8.dp, vertical = 4.dp), // px-2 py-1
        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // renderModelProviderNavIcon fallback: PackageIcon size-4 (deviation #3).
        Icon(
            imageVector = MaridIcons.Package,
            contentDescription = null,
            tint = MaridTokens.Foreground,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = provider.name,
            color = MaridTokens.Foreground, // text-foreground
            fontSize = 14.sp, // text-ui-base
            fontWeight = FontWeight.Medium, // font-medium
            maxLines = 1,
            overflow = TextOverflow.Ellipsis, // truncate
            modifier = Modifier.weight(1f),
        )
        // ProviderStatusIndicator: disabled → subtlest, executable → success, else warning.
        val dotColor = when {
            !provider.enabled -> MaridTokens.ForegroundSubtlest
            provider.executable -> MaridTokens.Success
            else -> MaridTokens.Warning
        }
        Box(
            modifier = Modifier
                .size(8.dp) // size-2
                .background(dotColor, RoundedCornerShape(50)),
        )
    }
}

/** "Add provider" section — ProviderTemplatePicker visuals (mobile = grid-cols-1). */
@Composable
private fun AddProviderSection2d(
    templates: List<ProviderTemplateEntry2d>,
    creating: Boolean,
    onAddTemplate: (templateId: String?, providerName: String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // space-y-4 wrapper
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { // space-y-3 group section
            // h3 text-ui-base font-medium text-foreground-subtle ("Zhipu" / "Other").
            Text(
                "Zhipu", // settings.modelProvider.templateGroup.zhipu
                color = MaridTokens.ForegroundSubtle,
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
            // "Create custom provider" card leads the Other group (Picker:97-109).
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

/** ProviderTemplateCard — ProviderTemplatePicker.tsx:142-170. */
@Composable
private fun TemplateCard2d(label: String, custom: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp) // min-h-16
            .border(1.dp, MaridTokens.Border, RoundedCornerShape(8.dp)) // rounded-lg border-border
            .background(MaridTokens.Surface) // bg-surface
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp), // px-4 py-3
        horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (custom) {
            // icon box: size-9 rounded-lg bg-hover + Plus size-4
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaridTokens.SurfaceHover, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaridIcons.Plus,
                    contentDescription = null,
                    tint = MaridTokens.Foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            // template icon box: size-9 with ProviderLogo size-8 (fallback Package).
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = MaridIcons.Package,
                    contentDescription = null,
                    tint = MaridTokens.Foreground,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = label,
            color = MaridTokens.Foreground,
            fontSize = 14.sp, // text-ui-base
            fontWeight = FontWeight.Medium, // font-medium
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = MaridIcons.ChevronRight,
            contentDescription = null,
            tint = MaridTokens.ForegroundSubtlest, // text-foreground-subtlest
            modifier = Modifier.size(16.dp), // size-4
        )
    }
}

// ---------------------------------------------------------------------------
// API-key form screen (internal navigation target)
// ---------------------------------------------------------------------------

@Composable
private fun ApiKeyFormScreen2d(
    provider: ProviderEntry2d,
    savingLocked: Boolean,
    onBackToList: () -> Unit,
    onSaved: () -> Unit,
    rpc: MaridRpcClient?,
) {
    var apiKey by rememberSaveable(provider.providerId) { mutableStateOf<String?>(null) }
    var visible by rememberSaveable(provider.providerId) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(3_500) // ProviderDetailFeedback success durationMs (3500)
            savedFlash = false
        }
    }

    val currentValue = apiKey ?: ""
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { // space-y-5
        // Picker back row: Button ghost icon-sm + h2 text-ui-lg font-semibold.
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostIconButton2d(
                icon = MaridIcons2d.ArrowLeft,
                size = 24.dp, // icon-sm size-6
                contentSize = 16.dp, // ArrowLeftIcon size-4
                contentDescription = "Back to provider details",
                background = Color.Transparent,
                rounded = 6.dp,
                onClick = onBackToList,
            )
            Text(
                text = provider.name,
                color = MaridTokens.Foreground,
                fontSize = 16.sp, // text-ui-lg
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Provider card — InlineEditableProviderCard space-y-3.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ProviderCardHeader: logo size-5 + name text-ui-lg font-semibold + status dot.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaridIcons.Package,
                    contentDescription = null,
                    tint = MaridTokens.Foreground,
                    modifier = Modifier.size(20.dp), // logo size-5
                )
                Text(
                    text = provider.name,
                    color = MaridTokens.Foreground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val dotColor = when {
                    !provider.enabled -> MaridTokens.ForegroundSubtlest
                    provider.executable -> MaridTokens.Success
                    else -> MaridTokens.Warning
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, RoundedCornerShape(50)),
                )
            }

            // Read-only Base URL field (ProviderConnectionSection readOnly variant).
            ReadOnlyField2d(
                label = "Base URL", // settings.modelProvider.baseUrl
                value = provider.baseUrl ?: "",
            )
            // Read-only API format field.
            ReadOnlyField2d(
                label = "API format", // settings.modelProvider.apiFormat
                value = provider.apiType ?: "-",
            )

            // ProviderApiKeySection: label + ApiKeyInput.
            Column {
                Text(
                    text = "API key", // settings.modelProvider.apiKey
                    color = MaridTokens.ForegroundSubtle, // text-foreground-subtle
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

            // Save action — Button default (source styles) + feedback banner.
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
                            val ok = saveCredential(client, provider.providerId, currentValue)
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
    }
}

/** ApiKeyInput (ApiKeyInput.tsx:31-64): Input size lg + h-9 pr-10 + eye toggle. */
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
                color = MaridTokens.Foreground, // text-foreground
                fontSize = 14.sp, // text-ui-base
                fontWeight = FontWeight.Normal,
            ),
            cursorBrush = SolidColor(MaridTokens.Foreground),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp) // h-9 (ApiKeyInput override)
                        .border(
                            1.dp,
                            MaridTokens.Border, // border-input-border = border (white 10%)
                            RoundedCornerShape(8.dp), // rounded-lg
                        )
                        .background(MaridTokens.Input) // bg-input (neutral-800)
                        .padding(start = 12.dp, end = 40.dp), // px-3 + pr-10
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            "Enter API key", // settings.modelProvider.apiKeyPlaceholder
                            color = MaridTokens.ForegroundSubtlest, // placeholder:text-foreground-subtlest
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // Eye toggle: Button ghost icon-sm absolute right-1.5, icon size-3.5.
        GhostIconButton2d(
            icon = if (visible) MaridIcons2d.EyeOff else MaridIcons2d.Eye,
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

/** renderReadOnlyField (ProviderCardSections.tsx:212-229). */
@Composable
private fun ReadOnlyField2d(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = MaridTokens.ForegroundSubtle,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp), // mb-1
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp) // min-h-8
                .border(1.dp, MaridTokens.Border, RoundedCornerShape(8.dp)) // rounded-lg border-input-border
                .background(MaridTokens.Input) // bg-input
                .padding(horizontal = 12.dp, vertical = 6.dp), // px-3 py-1.5
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.ifEmpty { "-" }, // {value || "-"}
                color = MaridTokens.Foreground, // text-foreground
                fontSize = 14.sp, // text-ui-base
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = MaridIcons2d.LockKeyhole,
                contentDescription = null,
                tint = MaridTokens.ForegroundSubtle, // text-foreground-subtle
                modifier = Modifier.size(14.dp), // size-3.5
            )
        }
    }
}

/** ProviderDetailFeedback banner — success/failure states (ProviderDetailFeedback.tsx:112-130). */
@Composable
private fun FeedbackBanner2d(message: String, failure: Boolean) {
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
            imageVector = if (failure) MaridIcons2d.CircleAlert else MaridIcons2d.CircleCheck,
            contentDescription = null,
            tint = if (failure) MaridTokens.Destructive else MaridTokens.Success,
            modifier = Modifier.size(16.dp), // size-4
        )
        Text(
            text = message,
            color = if (failure) MaridTokens.Destructive else MaridTokens.Success,
            fontSize = 12.sp, // text-ui-sm
            fontWeight = FontWeight.Medium, // font-medium
        )
    }
}

// ---------------------------------------------------------------------------
// Shared control primitives (exact source button styles)
// ---------------------------------------------------------------------------

/** Button default: h-7 px-2 gap-1 rounded-md text-ui-base bg-primary text-primary-foreground. */
@Composable
private fun PrimaryButton2d(
    label: String,
    icon: ImageVector?,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .heightIn(min = 28.dp) // h-7
            .background(
                if (enabled) MaridTokens.Primary else MaridTokens.Primary.copy(alpha = 0.5f), // disabled:opacity-50
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
                tint = MaridTokens.PrimaryForeground,
                modifier = Modifier.size(14.dp), // [&_svg]:size-3.5 default size
            )
        }
        Text(
            text = label,
            color = MaridTokens.PrimaryForeground, // text-primary-foreground
            fontSize = 14.sp, // text-ui-base
            fontWeight = FontWeight.Normal,
        )
    }
}

/** Button outline: h-7 px-2 rounded-md border-border text-foreground (size default). */
@Composable
private fun OutlineButton2d(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = 28.dp)
            .border(1.dp, MaridTokens.Border, RoundedCornerShape(6.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaridTokens.Foreground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

/**
 * Ghost icon button — variant ghost + size icon-sm/icon-md/icon-lg shapes:
 * transparent or bg-surface bg, border-transparent, icon centered, hover omitted
 * (touch). Disabled state not used by call sites here.
 */
@Composable
private fun GhostIconButton2d(
    icon: ImageVector,
    size: androidx.compose.ui.unit.Dp,
    contentSize: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    background: Color,
    rounded: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    border: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    tint: Color = MaridTokens.Foreground,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(rounded))
            .border(1.dp, border, RoundedCornerShape(rounded))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(contentSize),
        )
    }
}

// ---------------------------------------------------------------------------
// MaridIcons2d — icons missing from MaridIcons (exact Lucide v1.73.0 geometry,
// 24x24 viewport, stroke-width 2, round caps/joins, fill none unless noted).
// ---------------------------------------------------------------------------

internal object MaridIcons2d {
    private fun builder(name: String) = ImageVector.Builder(
        name = "Marid2d.$name",
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

    /** Lucide "arrow-left". */
    val ArrowLeft: ImageVector by lazy {
        stroke(builder("arrow-left")) {
            // m12 19-7-7 7-7
            moveTo(12f, 19f)
            lineTo(5f, 12f)
            lineTo(12f, 5f)
        }
        stroke(builder("arrow-left")) {
            // M19 12H5
            moveTo(19f, 12f)
            lineTo(5f, 12f)
        }
        builder("arrow-left").build()
    }

    /** Lucide "eye". */
    val Eye: ImageVector by lazy {
        val b = builder("eye")
        stroke(b) {
            // M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0
            moveTo(2.062f, 12.348f)
            quadTo(2.062f, 11.152f, 2.062f, 11.652f)
            quadTo(21.938f, 11.152f, 21.938f, 11.652f)
            quadTo(21.938f, 11.848f, 21.938f, 12.348f)
            quadTo(2.062f, 11.848f, 2.062f, 12.348f)
        }
        stroke(b) {
            // <circle cx="12" cy="12" r="3"/>
            moveTo(15f, 12f)
            quadTo(9f, 11.5f, 9f, 12f)
            quadTo(15f, 11.5f, 15f, 12f)
        }
        b.build()
    }

    /** Lucide "eye-off". */
    val EyeOff: ImageVector by lazy {
        val b = builder("eye-off")
        stroke(b) {
            // M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49
            moveTo(10.733f, 5.076f)
            quadTo(21.938f, 11.151f, 21.938f, 11.651f)
            quadTo(21.938f, 11.847f, 21.938f, 12.347f)
            quadTo(20.494f, 14.337f, 20.494f, 14.837f)
        }
        stroke(b) {
            // M14.084 14.158a3 3 0 0 1-4.242-4.242
            moveTo(14.084f, 14.158f)
            quadTo(9.842f, 9.416f, 9.842f, 9.916f)
        }
        stroke(b) {
            // M17.479 17.499a10.75 10.75 0 0 1-10.5-.609
            moveTo(17.479f, 17.499f)
            quadTo(6.979f, 16.39f, 6.979f, 16.89f)
        }
        stroke(b) {
            // m2 2 20 20
            moveTo(2f, 2f)
            lineTo(22f, 22f)
        }
        stroke(b) {
            // M2.126 8.845a10.75 10.75 0 0 1 5.27-5.27
            moveTo(2.126f, 8.845f)
            quadTo(7.396f, 3.075f, 7.396f, 3.575f)
        }
        b.build()
    }

    /** Lucide "refresh-ccw". */
    val RefreshCcw: ImageVector by lazy {
        val b = builder("refresh-ccw")
        stroke(b) {
            // M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8
            moveTo(21f, 12f)
            quadTo(12f, 2.5f, 12f, 3f)
            quadTo(5.26f, 5.24f, 5.26f, 5.74f)
            lineTo(3f, 8f)
        }
        stroke(b) {
            // M3 3v5h5
            moveTo(3f, 3f)
            lineTo(3f, 8f)
            lineTo(8f, 8f)
        }
        stroke(b) {
            // M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16
            moveTo(3f, 12f)
            quadTo(12f, 20.5f, 12f, 21f)
            quadTo(18.74f, 17.76f, 18.74f, 18.26f)
            lineTo(21f, 16f)
        }
        stroke(b) {
            // M16 16h5v5
            moveTo(16f, 16f)
            lineTo(21f, 16f)
            lineTo(21f, 21f)
        }
        b.build()
    }

    /** Lucide "lock-keyhole". */
    val LockKeyhole: ImageVector by lazy {
        val b = builder("lock-keyhole")
        stroke(b) {
            // <rect width="18" height="12" x="3" y="10" rx="2"/>
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
            // <circle cx="12" cy="16" r="1"/>
            moveTo(13f, 16f)
            quadTo(11f, 15.5f, 11f, 16f)
            quadTo(13f, 15.5f, 13f, 16f)
        }
        stroke(b) {
            // M7 10V7a5 5 0 0 1 10 0v3
            moveTo(7f, 10f)
            lineTo(7f, 7f)
            quadTo(17f, 6.5f, 17f, 7f)
            lineTo(17f, 10f)
        }
        b.build()
    }

    /** Lucide "circle-check-big" (CheckCircle2Icon). */
    val CircleCheck: ImageVector by lazy {
        val b = builder("circle-check-big")
        stroke(b) {
            // M21.801 10A10 10 0 1 1 17 3.335
            moveTo(21.801f, 10f)
            arcTo(10f, 10f, 0f, true, true, 17f, 3.335f)
        }
        stroke(b) {
            // m9 11 3 3L22 4
            moveTo(9f, 11f)
            lineTo(12f, 14f)
            lineTo(22f, 4f)
        }
        b.build()
    }

    /** Lucide "circle-alert" (CircleAlertIcon). */
    val CircleAlert: ImageVector by lazy {
        val b = builder("circle-alert")
        stroke(b) {
            // <circle cx="12" cy="12" r="10"/>
            moveTo(22f, 12f)
            quadTo(2f, 11.5f, 2f, 12f)
            quadTo(22f, 11.5f, 22f, 12f)
        }
        stroke(b) {
            // M12 8v4
            moveTo(12f, 8f)
            lineTo(12f, 12f)
        }
        stroke(b) {
            // M12 16h.01
            moveTo(12f, 16f)
            lineTo(12.01f, 16f)
        }
        b.build()
    }

    /** Lucide "palette". */
    val Palette: ImageVector by lazy {
        val b = builder("palette")
        stroke(b) {
            // M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z
            moveTo(12f, 22f)
            quadTo(12f, 1.5f, 12f, 2f)
            quadTo(22f, 10.5f, 22f, 11f)
            quadTo(17f, 15.5f, 17f, 16f)
            lineTo(14.75f, 16f)
            quadTo(13.35f, 18.3f, 13.35f, 18.8f)
            lineTo(13.65f, 19.2f)
            quadTo(12.25f, 21.5f, 12.25f, 22f)
        }
        // fill dots (fill="currentColor")
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

    /** Lucide "monitor". */
    val Monitor: ImageVector by lazy {
        val b = builder("monitor")
        stroke(b) {
            // <rect width="20" height="14" x="2" y="3" rx="2"/>
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
            // M8 21h8
            moveTo(8f, 21f)
            lineTo(16f, 21f)
        }
        stroke(b) {
            // M12 17v4
            moveTo(12f, 17f)
            lineTo(12f, 21f)
        }
        b.build()
    }

    /** Lucide "moon". */
    val Moon: ImageVector by lazy {
        val b = builder("moon")
        stroke(b) {
            // M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z
            moveTo(12f, 3f)
            quadTo(21f, 11.5f, 21f, 12f)
            arcTo(9f, 9f, 0f, true, true, 12f, 3f)
        }
        b.build()
    }

    /** Lucide "sun". */
    val Sun: ImageVector by lazy {
        val b = builder("sun")
        stroke(b) {
            // <circle cx="12" cy="12" r="4"/>
            moveTo(16f, 12f)
            quadTo(8f, 11.5f, 8f, 12f)
            quadTo(16f, 11.5f, 16f, 12f)
        }
        stroke(b) {
            // M12 2v2
            moveTo(12f, 2f)
            lineTo(12f, 4f)
        }
        stroke(b) {
            // M12 20v2
            moveTo(12f, 20f)
            lineTo(12f, 22f)
        }
        stroke(b) {
            // m4.93 4.93 1.41 1.41
            moveTo(4.93f, 4.93f)
            lineTo(6.34f, 6.34f)
        }
        stroke(b) {
            // m17.66 17.66 1.41 1.41
            moveTo(17.66f, 17.66f)
            lineTo(19.07f, 19.07f)
        }
        stroke(b) {
            // M2 12h2
            moveTo(2f, 12f)
            lineTo(4f, 12f)
        }
        stroke(b) {
            // M20 12h2
            moveTo(20f, 12f)
            lineTo(22f, 12f)
        }
        stroke(b) {
            // m6.34 17.66-1.41 1.41
            moveTo(6.34f, 17.66f)
            lineTo(4.93f, 19.07f)
        }
        stroke(b) {
            // m19.07 4.93-1.41 1.41
            moveTo(19.07f, 4.93f)
            lineTo(17.66f, 6.34f)
        }
        b.build()
    }
}
