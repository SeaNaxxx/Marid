package com.qodex.app.state

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * V4 conversation projection (port of the engine's delta rules in
 * packages/shared/src/qodex-protocol-v4/delta.ts):
 *  - snapshot: replace the visible rows window wholesale
 *  - row.appended / row.upserted: append or replace by rowId
 *  - row.removed: drop every row with rowId >= fromRowId
 *  - row.delta: append a string at the row's streamable path (text / inputText /
 *    output.text / summaryText)
 *  - state.updated: merged session-state keys (exposed for status surfaces)
 *
 * Rows are kept keyed by rowId so streaming deltas land in the correct row even
 * when frames arrive out of order across turns.
 */
class ConversationProjection {

    data class View(
        val rows: List<JsonObject>,
        /** Merged state.updated keys — turnHeader/usage/queue signals live here. */
        val state: JsonObject,
        /** Highest applied transport seq; diagnostics for the boot log. */
        val lastSeq: Long?,
    )

    private val rows = LinkedHashMap<Int, JsonObject>()
    private val state = HashMap<String, kotlinx.serialization.json.JsonElement>()
    var lastSeq: Long? = null
        private set

    fun applyLogicalFrame(frame: JsonObject): Boolean {
        val payload = frame["payload"] as? JsonObject ?: return false
        (frame["toSeq"] as? JsonPrimitive)?.content?.toLongOrNull()?.let { lastSeq = it }
        return when (payload["kind"]?.let { (it as? JsonPrimitive)?.content }) {
            "snapshot" -> applySnapshot(payload["snapshot"] as? JsonObject)
            "deltas" -> {
                val deltas = payload["deltas"]?.jsonArray
                var changed = false
                for (delta in deltas ?: emptyList()) {
                    (delta as? JsonObject)?.let { changed = applyDelta(it) || changed }
                }
                changed
            }
            else -> false
        }
    }

    private fun applySnapshot(snapshot: JsonObject?): Boolean {
        if (snapshot == null) return false
        rows.clear()
        val window = snapshot["rows"]?.jsonObject?.get("window")?.jsonArray
        for (row in window ?: emptyList()) {
            (row as? JsonObject)?.let { putRow(it) }
        }
        for ((key, value) in snapshot) {
            if (key != "rows") state[key] = value
        }
        return true
    }

    private fun applyDelta(delta: JsonObject): Boolean = when (delta.string("op")) {
        "row.appended" -> delta["row"]?.let { putRow(it.jsonObject) } != null
        "row.upserted" -> delta["row"]?.let { putRow(it.jsonObject) } != null
        "row.removed" -> {
            val fromRowId = delta.int("fromRowId")
            if (fromRowId == null) {
                false
            } else {
                val evicted = rows.keys.filter { it >= fromRowId }
                evicted.forEach { rows.remove(it) }
                evicted.isNotEmpty()
            }
        }
        "row.delta" -> applyRowDelta(delta)
        "state.updated" -> {
            val patch = delta["patch"] as? JsonObject ?: return false
            for ((key, value) in patch) state[key] = value
            true
        }
        // pluginUi.* / workflowRun.* deltas carry no conversation rows; they are
        // live-only plugin/workflow notifications with no mobile rendering yet.
        else -> false
    }

    private fun applyRowDelta(delta: JsonObject): Boolean {
        val rowId = delta.int("rowId")
        val path = delta.string("path")
        val append = delta.string("append")
        if (rowId == null || path.isEmpty() || append.isEmpty()) return false
        val row = rows[rowId] ?: return false
        rows[rowId] = appendToPath(row, path, append)
        return true
    }

    private fun putRow(row: JsonObject): Boolean {
        val rowId = row.int("rowId") ?: return false
        rows[rowId] = row
        return true
    }

    // row.delta appends are single-string writes at a fixed streamable path
    // (text / inputText / output.text / summaryText); deeper traversal never happens.
    private fun appendToPath(row: JsonObject, path: String, append: String): JsonObject {
        val existing = when (path) {
            "output.text" -> {
                val output = row["output"] as? JsonObject
                output?.get("text")?.let { (it as? JsonPrimitive)?.content }
            }
            else -> row[path]?.let { (it as? JsonPrimitive)?.content }
        }
        val merged = (existing ?: "") + append
        return if (path == "output.text") {
            val output = (row["output"] as? JsonObject) ?: JsonObject(emptyMap())
            JsonObject(row.toMap() + ("output" to JsonObject(output.toMap() + ("text" to JsonPrimitive(merged)))))
        } else {
            JsonObject(row.toMap() + (path to JsonPrimitive(merged)))
        }
    }

    fun view(): View = View(rows.values.toList(), JsonObject(state.toMap()), lastSeq)

    fun clear() {
        rows.clear()
        state.clear()
        lastSeq = null
    }
}

internal fun JsonObject.string(key: String): String =
    (this[key] as? JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content.orEmpty()

internal fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content?.toIntOrNull()
