package com.beatrate.diffview.parser

import com.beatrate.diffview.common.Commit
import com.beatrate.diffview.common.Diff
import com.beatrate.diffview.common.Hunk
import java.io.File
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DiffParser {
    private data class HeaderParseResult(val message: String, val author: String, val date: Instant, val body: String)
    private data class CommitMetadata(val message: String, val author: String, val date: ZonedDateTime)

    private val authorPattern = Regex("""From:\s(.*)\s<\w.*>""")
    private val datePattern = Regex("""Date:\s(\w.*)""")
    private val fromPattern = Regex("--- (.+)")
    private val toPattern = Regex("/+/+/+ (.+)")
    private val rangePattern = Regex("""@@\s+\-(\d+),(\d+)\s+\+(\d+),(\d+)\s@@""")

    fun parse(diffFile: File): Commit {
        val reader = diffFile.pushBackLineReader()
        val (message, author, date) = parseMetadata(reader)
        return Commit(message, author, date)
    }

    private fun parseMetadata(reader: PushbackLineReader): CommitMetadata {
        reader.expectAny()
        val author = reader.expectValue(authorPattern)
        val date = reader.expectValue(datePattern)
        val message = reader.expectValue("Subject:")

        val format = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z")
        val parsedDate = date.runCatching { ZonedDateTime.from(format.parse(this)) }
                .getOrElse { fail() }
        return CommitMetadata(message, author, parsedDate)
    }

    private fun parseBody(reader: PushbackLineReader): List<Diff> {
        val diffs = mutableListOf<Diff>()
        var line: String? = reader.readLine()

        while(line != null) {
            reader.whenMatch(fromPattern) {from ->
                val to = reader.expectValue(toPattern)

            }

            line = reader.readLine()
        }

        return diffs
    }

//    private fun parseHunks(reader: PushbackLineReader): List<Hunk> {
//        val hunks = mutableListOf<Hunk>()
//        var line: String? = reader.readLine()
//        while(line != null) {
//            val range = reader.expectValue(rangePattern)
//            line = reader.readLine()
//        }
//    }
}

private fun fail(): Nothing = throw DiffParserException("Illegal parser state")

private fun PushbackLineReader.expectAny(): String = readLine() ?: fail()

private fun PushbackLineReader.expectValue(regex: Regex): String =
        readLine()?.let { regex.matchEntire(it) }?.groupValues?.get(1) ?: fail()

private fun PushbackLineReader.expectValue(prefix: String): String {
    val line = readLine() ?: fail()
    if(!line.startsWith(prefix)) {
        fail()
    }
    return line.removePrefix(prefix).trimStart()
}

private fun PushbackLineReader.whenMatch(regex: Regex, action: (String) -> Unit) {
    var line: String? = readLine()
    while(line != null) {
        val value = regex.matchEntire(line)?.groupValues?.get(1)
        if(value != null) {
            action(value)
        }
        line = readLine()
    }
}