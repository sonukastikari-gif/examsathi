package com.example.data.local

import com.example.domain.model.StudentResponseSnapshot
import com.example.domain.model.SubjectScore
import org.json.JSONArray
import org.json.JSONObject

object JsonHelper {
    fun serializeResponses(responses: List<StudentResponseSnapshot>): String {
        val array = JSONArray()
        for (r in responses) {
            val obj = JSONObject()
            obj.put("questionId", r.questionId)
            obj.put("questionNumber", r.questionNumber)
            obj.put("questionText", r.questionText)
            obj.put("subject", r.subject)
            obj.put("topic", r.topic)

            val opts = JSONArray()
            r.options.forEach { opts.put(it) }
            obj.put("options", opts)

            if (r.selectedOptionIndex != null) {
                obj.put("selectedOptionIndex", r.selectedOptionIndex)
            } else {
                obj.put("selectedOptionIndex", JSONObject.NULL)
            }
            obj.put("correctOptionIndex", r.correctOptionIndex)
            obj.put("isCorrect", r.isCorrect)
            obj.put("marksEarned", r.marksEarned)
            obj.put("explanation", r.explanation)
            obj.put("isMarkedForReview", r.isMarkedForReview)

            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeResponses(jsonStr: String): List<StudentResponseSnapshot> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<StudentResponseSnapshot>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optionsArray = obj.getJSONArray("options")
                val optionsList = mutableListOf<String>()
                for (j in 0 until optionsArray.length()) {
                    optionsList.add(optionsArray.getString(j))
                }

                val selectedIdx = if (obj.isNull("selectedOptionIndex")) null else obj.getInt("selectedOptionIndex")

                list.add(
                    StudentResponseSnapshot(
                        questionId = obj.getString("questionId"),
                        questionNumber = obj.optInt("questionNumber", i + 1),
                        questionText = obj.getString("questionText"),
                        subject = obj.optString("subject", "General"),
                        topic = obj.optString("topic", ""),
                        options = optionsList,
                        selectedOptionIndex = selectedIdx,
                        correctOptionIndex = obj.getInt("correctOptionIndex"),
                        isCorrect = obj.getBoolean("isCorrect"),
                        marksEarned = obj.optDouble("marksEarned", 0.0),
                        explanation = obj.optString("explanation", ""),
                        isMarkedForReview = obj.optBoolean("isMarkedForReview", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun serializeSubjectScores(map: Map<String, SubjectScore>): String {
        val obj = JSONObject()
        for ((key, value) in map) {
            val subObj = JSONObject()
            subObj.put("subject", value.subject)
            subObj.put("total", value.total)
            subObj.put("correct", value.correct)
            subObj.put("wrong", value.wrong)
            subObj.put("score", value.score)
            obj.put(key, subObj)
        }
        return obj.toString()
    }

    fun deserializeSubjectScores(jsonStr: String): Map<String, SubjectScore> {
        if (jsonStr.isBlank()) return emptyMap()
        val map = mutableMapOf<String, SubjectScore>()
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val subObj = obj.getJSONObject(key)
                map[key] = SubjectScore(
                    subject = subObj.getString("subject"),
                    total = subObj.getInt("total"),
                    correct = subObj.getInt("correct"),
                    wrong = subObj.getInt("wrong"),
                    score = subObj.getDouble("score")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
}
