package com.qodex.app.ui

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Task 1.1 regression: the model editor must accept every reasoning option-map
// string the engine compiles (packages/model-option-map), not only strict JSON
// objects. Production maps in config/provider/qodex-builtin.json are restricted
// CEL conditionals and were previously rejected with
// "Reasoning parameter mapping is invalid", blocking model saves.
class ReasoningMapValidationTest {

    private fun draft(
        id: String = "test-model",
        contextWindow: String = "",
        maxOutputTokens: String = "",
        levels: List<String> = listOf("low", "high"),
        map: String = "",
        useRecommended: Boolean = true,
    ): ModelDraftValues2e =
        ModelDraftValues2e(
            idValue = id,
            contextWindowValue = contextWindow,
            maxOutputTokensValue = maxOutputTokens,
            supportsImage = false,
            supportsVideo = false,
            supportsPdf = false,
            supportsJsonSchemaOutputValue = false,
            supportsNativeWebSearchValue = false,
            supportsMidConversationSystemValue = false,
            reasoningLevelValuesValue = levels,
            reasoningLevelMapValue = map,
            useRecommendedConfigValue = useRecommended,
        )

    @Test
    fun plainJsonObjectMapParsesWithLevelKeys() {
        val map = parseReasoningMap2e("""{"low":"reasoning_effort","high":"reasoning_effort_high"}""")
        assertTrue(map is ReasoningMapProgram2e.LevelKeys)
        assertEquals(setOf("low", "high"), (map as ReasoningMapProgram2e.LevelKeys).keys)
    }

    @Test
    fun productionTernaryMapCompilesAsExpression() {
        // Shape used throughout config/provider/qodex-builtin.json.
        val map = parseReasoningMap2e(
            "reasoningLevel == \"disabled\" ? {\"enable_thinking\": false} : " +
                "{\"enable_thinking\": true, \"reasoning_effort\": reasoningLevel}",
        )
        assertEquals(ReasoningMapProgram2e.Expression, map)
    }

    @Test
    fun nestedTernaryWithOrConditionCompiles() {
        val map = parseReasoningMap2e(
            "reasoningLevel == \"disabled\" || reasoningLevel == \"off\" ? {\"thinking\": {\"type\": \"disabled\"}} : " +
                "{\"thinking\": {\"type\": \"enabled\", \"budget_tokens\": 4096}}",
        )
        assertEquals(ReasoningMapProgram2e.Expression, map)
    }

    @Test
    fun singleQuotedCelStringsAreAccepted() {
        val map = parseReasoningMap2e("reasoningLevel == 'disabled' ? {'enable_thinking': false} : {}")
        assertEquals(ReasoningMapProgram2e.Expression, map)
    }

    @Test
    fun maxOutputTokensVariableIsRejectedForReasoningMap() {
        // The reasoning map compiles with variableName = "reasoningLevel";
        // referencing maxOutputTokens must be an unknown identifier, like the engine.
        assertNull(parseReasoningMap2e("maxOutputTokens > 4096 ? {} : {}"))
    }

    @Test
    fun unknownIdentifierIsRejected() {
        assertNull(parseReasoningMap2e("foo == \"disabled\" ? {} : {}"))
    }

    @Test
    fun memberAccessIsRejected() {
        assertNull(parseReasoningMap2e("reasoningLevel.enabled"))
    }

    @Test
    fun functionCallIsRejected() {
        assertNull(parseReasoningMap2e("toString(reasoningLevel)"))
    }

    @Test
    fun nonObjectResultIsRejected() {
        assertNull(parseReasoningMap2e("\"reasoning_effort\""))
        assertNull(parseReasoningMap2e("[1, 2]"))
    }

    @Test
    fun duplicateObjectKeyIsRejected() {
        assertNull(parseReasoningMap2e("{\"enable_thinking\": false, \"enable_thinking\": true}"))
    }

    @Test
    fun malformedSyntaxIsRejected() {
        assertNull(parseReasoningMap2e("reasoningLevel == \"disabled\" ? {\"enable_thinking\": false}"))
        assertNull(parseReasoningMap2e("{\"low\": \"reasoning_effort\""))
        assertNull(parseReasoningMap2e("reasoningLevel == \"disabled\" {\"a\": 1} : {\"b\": 2}"))
    }

    @Test
    fun emptyInputReturnsNull() {
        assertNull(parseReasoningMap2e(""))
        assertNull(parseReasoningMap2e("   "))
    }

    @Test
    fun validateAcceptsCelMapWithoutLevelKeyCoverage() {
        // Levels [enabled, disabled] are not object keys, but the conditional map
        // covers them at request time — must save instead of failing validation.
        assertNull(
            validateModelDraft2e(
                draft(
                    levels = listOf("enabled", "disabled"),
                    map = "reasoningLevel == \"disabled\" ? {\"enable_thinking\": false} : {\"enable_thinking\": true}",
                ),
                null,
            ),
        )
    }

    @Test
    fun validateRejectsJsonObjectMapMissingLevels() {
        assertEquals(
            ModelDraftValidationError2e.ReasoningMap,
            validateModelDraft2e(
                draft(levels = listOf("low", "high"), map = """{"low": "reasoning_effort"}"""),
                null,
            ),
        )
    }

    @Test
    fun validateAcceptsJsonObjectMapCoveringLevels() {
        assertNull(
            validateModelDraft2e(
                draft(
                    levels = listOf("low", "high"),
                    map = """{"low": "reasoning_effort", "high": "reasoning_effort"}""",
                ),
                null,
            ),
        )
    }

    @Test
    fun validateSmartModeSparseOverlayStaysValid() {
        // No levels, no map, smart mode: engine defaults apply.
        assertNull(validateModelDraft2e(draft(levels = emptyList(), map = ""), null))
    }

    @Test
    fun validateManualModeStillRequiresMapWhenLevelsPresent() {
        // Manual mode owns the hard caps: contextWindow/maxOutputTokens must be
        // filled, otherwise the draft fails on the caps before the map check.
        assertEquals(
            ModelDraftValidationError2e.ContextWindow,
            validateModelDraft2e(
                draft(levels = listOf("low"), map = "", useRecommended = false),
                null,
            ),
        )
        assertEquals(
            ModelDraftValidationError2e.ReasoningMap,
            validateModelDraft2e(
                draft(
                    contextWindow = "128000",
                    maxOutputTokens = "8192",
                    levels = listOf("low"),
                    map = "",
                    useRecommended = false,
                ),
                null,
            ),
        )
    }

    @Test
    fun validateInheritedTernaryMapPassesWithTemplateLevels() {
        // Smart mode inheriting a CEL map from the template recommendation must
        // not fail the coverage check (this exact shape blocked model saves).
        val inheritedMap = "reasoningLevel == \"disabled\" ? {\"enable_thinking\": false} : " +
            "{\"enable_thinking\": true, \"reasoning_effort\": reasoningLevel}"
        val inherited = buildJsonObject {
            put(
                "optionSpecs",
                buildJsonObject {
                    put(
                        "reasoningLevel",
                        buildJsonObject { put("map", inheritedMap) },
                    )
                },
            )
        }
        assertNull(
            validateModelDraft2e(
                draft(levels = listOf("enabled", "disabled"), map = ""),
                inherited,
            ),
        )
    }
}
