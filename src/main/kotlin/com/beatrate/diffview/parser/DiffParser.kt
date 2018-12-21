package com.beatrate.diffview.parser

import java.io.BufferedReader
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DiffParser {
    private data class HeaderParseResult(val message: String, val author: String, val date: Instant, val body: String)
    private data class CommitMetadata(val message: String, val author: String, val date: ZonedDateTime?)

    private val authorPattern = Regex("""From: (.+) <.+>""")
    private val datePattern = Regex("Date: (.+)")
    private val messagePattern = Regex("Subject: (.*)")

    fun parse(diffFile: File): Commit {
        val reader = diffFile.bufferedReader()
        val (message, author, date) = parseMetadata(reader)
        return Commit(message, author, date, mutableListOf<Diff>())
    }

    private fun parseMetadata(reader: BufferedReader): CommitMetadata {
        var message = ""
        var author = ""
        var date = ""

        var line: String? = reader.readLine() ?: ""
        if(!line!!.startsWith("From")) {
            return CommitMetadata(message, author, null)
        }

        line = reader.readLine()
        while(!line.isNullOrBlank()) {
            var value = ""
            when {
                authorPattern.matchEntire(line)?.groupValues?.get(1)?.let { value = it } != null -> author = value
                datePattern.matchEntire(line)?.groupValues?.get(1)?.let { value = it } != null -> date = value
                messagePattern.matchEntire(line)?.groupValues?.get(1)?.let { value = it } != null -> message = value
            }
            line = reader.readLine()
        }
        val format = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z")
        val parsedDate = date.runCatching { ZonedDateTime.from(format.parse(this)) }.getOrNull()
        return CommitMetadata(message, author, parsedDate)
    }

    private fun parseBody(body: String): List<Diff> {
        val diffs = mutableListOf<Diff>()
        return diffs
    }
}