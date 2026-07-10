package com.sirjpagdi.mcqscanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistence layer using SharedPreferences + JSON.
 * Supports multiple named, savable/loadable tests (Save Test / Load Test),
 * a theme preference, and per-test paper size.
 */
object Prefs {
    private const val FILE = "mcq_scanner_prefs"
    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    enum class PaperSize { FULL, HALF, QUARTER }
    enum class AppTheme { LIGHT, DARK, BLACK }

    data class Template(
        val numQuestions: Int,
        val choicesPerQuestion: Int,
        val columns: Int,
        val paperSize: PaperSize = PaperSize.FULL
    )

    data class Test(
        val name: String,
        val template: Template,
        val answerKey: List<String>
    )

    data class ResultRecord(
        val studentName: String,
        val score: Int,
        val total: Int,
        val timestamp: Long
    )

    // ---- Theme ----
    fun saveTheme(ctx: Context, theme: AppTheme) {
        sp(ctx).edit().putString("theme", theme.name).apply()
    }

    fun loadTheme(ctx: Context): AppTheme {
        val raw = sp(ctx).getString("theme", AppTheme.LIGHT.name)
        return try { AppTheme.valueOf(raw ?: AppTheme.LIGHT.name) } catch (e: Exception) { AppTheme.LIGHT }
    }

    // ---- Currently active (in-progress, unsaved) test being configured ----
    fun saveTemplate(ctx: Context, t: Template) {
        sp(ctx).edit()
            .putInt("t_questions", t.numQuestions)
            .putInt("t_choices", t.choicesPerQuestion)
            .putInt("t_columns", t.columns)
            .putString("t_papersize", t.paperSize.name)
            .apply()
    }

    fun loadTemplate(ctx: Context): Template? {
        val p = sp(ctx)
        val q = p.getInt("t_questions", -1)
        if (q <= 0) return null
        val paper = try {
            PaperSize.valueOf(p.getString("t_papersize", PaperSize.FULL.name) ?: PaperSize.FULL.name)
        } catch (e: Exception) { PaperSize.FULL }
        return Template(q, p.getInt("t_choices", 4), p.getInt("t_columns", 1), paper)
    }

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

    fun activeTestName(ctx: Context): String = sp(ctx).getString("active_test_name", "") ?: ""
    fun setActiveTestName(ctx: Context, name: String) {
        sp(ctx).edit().putString("active_test_name", name).apply()
    }

    // ---- Saved tests library (named Save Test / Load Test) ----
    private fun testsRaw(ctx: Context): JSONObject {
        val raw = sp(ctx).getString("saved_tests", null) ?: return JSONObject()
        return JSONObject(raw)
    }

    fun saveTest(ctx: Context, test: Test) {
        val all = testsRaw(ctx)
        val obj = JSONObject()
        obj.put("questions", test.template.numQuestions)
        obj.put("choices", test.template.choicesPerQuestion)
        obj.put("columns", test.template.columns)
        obj.put("papersize", test.template.paperSize.name)
        obj.put("key", JSONArray(test.answerKey))
        all.put(test.name, obj)
        sp(ctx).edit().putString("saved_tests", all.toString()).apply()
    }

    fun loadTest(ctx: Context, name: String): Test? {
        val all = testsRaw(ctx)
        if (!all.has(name)) return null
        val o = all.getJSONObject(name)
        val keyArr = o.getJSONArray("key")
        val key = (0 until keyArr.length()).map { keyArr.optString(it, "") }
        val paper = try {
            PaperSize.valueOf(o.optString("papersize", PaperSize.FULL.name))
        } catch (e: Exception) { PaperSize.FULL }
        return Test(
            name,
            Template(o.getInt("questions"), o.getInt("choices"), o.getInt("columns"), paper),
            key
        )
    }

    fun listTestNames(ctx: Context): List<String> {
        val all = testsRaw(ctx)
        return all.keys().asSequence().toList().sorted()
    }

    fun deleteTest(ctx: Context, name: String) {
        val all = testsRaw(ctx)
        all.remove(name)
        sp(ctx).edit().putString("saved_tests", all.toString()).apply()
    }

    fun saveHistory(ctx: Context, record: ResultRecord) {
        val raw = sp(ctx).getString("history_records", null)
        val arr = if (raw == null) JSONArray() else JSONArray(raw)
        val obj = JSONObject()
        obj.put("studentName", record.studentName)
        obj.put("score", record.score)
        obj.put("total", record.total)
        obj.put("timestamp", record.timestamp)
        arr.put(obj)
        sp(ctx).edit().putString("history_records", arr.toString()).apply()
    }

    fun loadHistory(ctx: Context): List<ResultRecord> {
        val raw = sp(ctx).getString("history_records", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { idx ->
            val o = arr.optJSONObject(idx) ?: JSONObject()
            ResultRecord(
                o.optString("studentName", "Unnamed"),
                o.optInt("score", 0),
                o.optInt("total", 0),
                o.optLong("timestamp", 0L)
            )
        }
    }
}
