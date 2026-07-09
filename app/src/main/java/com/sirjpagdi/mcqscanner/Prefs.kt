package com.sirjpagdi.mcqscanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight persistence layer using SharedPreferences + JSON.
 * Keeps things dependency-free and easy to inspect/debug.
 */
object Prefs {
    private const val FILE = "mcq_scanner_prefs"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---- Template (sheet layout) ----
    data class Template(val numQuestions: Int, val choicesPerQuestion: Int, val columns: Int)

    fun saveTemplate(ctx: Context, t: Template) {
        sp(ctx).edit()
            .putInt("t_questions", t.numQuestions)
            .putInt("t_choices", t.choicesPerQuestion)
            .putInt("t_columns", t.columns)
            .apply()
    }

    fun loadTemplate(ctx: Context): Template? {
        val p = sp(ctx)
        val q = p.getInt("t_questions", -1)
        if (q <= 0) return null
        return Template(
            q,
            p.getInt("t_choices", 4),
            p.getInt("t_columns", 1)
        )
    }

    // ---- Answer key ----
    // Stored as JSON array of strings; "" means no correct answer / void question.
    fun saveAnswerKey(ctx: Context, answers: List<String>) {
        val arr = JSONArray()
        answers.forEach { arr.put(it) }
        sp(ctx).edit().putString("answer_key", arr.toString()).apply()
    }

    fun loadAnswerKey(ctx: Context): List<String>? {
        val raw = sp(ctx).getString("answer_key", null) ?: return null
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { arr.optString(it, "") }
    }

    // ---- Results history ----
    data class ResultRecord(
        val studentName: String,
        val score: Int,
        val total: Int,
        val detected: List<String>,
        val correctKey: List<String>,
        val timestamp: Long
    )

    fun addResult(ctx: Context, r: ResultRecord) {
        val history = loadHistoryRaw(ctx)
        val obj = JSONObject()
        obj.put("name", r.studentName)
        obj.put("score", r.score)
        obj.put("total", r.total)
        obj.put("detected", JSONArray(r.detected))
        obj.put("key", JSONArray(r.correctKey))
        obj.put("ts", r.timestamp)
        history.put(obj)
        sp(ctx).edit().putString("history", history.toString()).apply()
    }

    private fun loadHistoryRaw(ctx: Context): JSONArray {
        val raw = sp(ctx).getString("history", null) ?: return JSONArray()
        return JSONArray(raw)
    }

    fun loadHistory(ctx: Context): List<ResultRecord> {
        val arr = loadHistoryRaw(ctx)
        val out = mutableListOf<ResultRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val detected = mutableListOf<String>()
            val detArr = o.getJSONArray("detected")
            for (j in 0 until detArr.length()) detected.add(detArr.getString(j))
            val keyArr = o.getJSONArray("key")
            val key = mutableListOf<String>()
            for (j in 0 until keyArr.length()) key.add(keyArr.getString(j))
            out.add(
                ResultRecord(
                    o.getString("name"),
                    o.getInt("score"),
                    o.getInt("total"),
                    detected,
                    key,
                    o.getLong("ts")
                )
            )
        }
        return out
    }
}
