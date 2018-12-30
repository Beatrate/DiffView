package com.beatrate.diffview.parser

import com.beatrate.diffview.common.*
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DiffParser {
    private data class CommitHeader(val author: String, val date: ZonedDateTime, val message: String)

    private class Prefix {
        companion object {
            const val AUTHOR = "From: "
            const val DATE = "Date: "
            const val MESSAGE = "Subject: "
            const val OLD_FILE = "--- "
            const val NEW_FILE = "+++ "
            const val FILE_RANGE = "@@ "
        }
    }

    private val rangeRegex = Regex("""@@\s+-(\d+),(\d+)\s+\+(\d+),(\d+)\s@@""")

    fun parse(file: File): Commit {
        file.peekReader().use { reader ->
            val (author, date, message) = parseCommitHeader(reader)
            val diffs = parseDiffs(reader)
            return Commit(message, author, date, diffs)
        }
    }

    private fun parseCommitHeader(reader: PeekReader): CommitHeader {
        reader.next()
        // Drop email inside <>.
        val author = parsePrefixed(reader, Prefix.AUTHOR).substringBeforeLast('<').trim()
        val format = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z")
        val date = parsePrefixed(reader, Prefix.DATE)
                .runCatching { ZonedDateTime.from(format.parse(this)) }
                .getOrElse { throw DiffParserException() }
        val message = parsePrefixed(reader, Prefix.MESSAGE).removePrefix("[PATCH] ")
        return CommitHeader(author, date, message)
    }

    private fun parseDiffs(reader: PeekReader): List<Diff> {
        val diffs = mutableListOf<Diff>()
        while (reader.notEmpty) {
            if (reader.peek().startsWith(Prefix.OLD_FILE)) {
                diffs.add(parseDiff(reader))
            } else {
                reader.next()
            }
        }
        return diffs
    }

    private fun parseDiff(reader: PeekReader): Diff {
        val oldFile = parsePrefixed(reader, Prefix.OLD_FILE)
        val newFile = parsePrefixed(reader, Prefix.NEW_FILE)
        val hunks = parseHunks(reader)
        return Diff(oldFile, newFile, hunks)
    }

    private fun parsePrefixed(reader: PeekReader, prefix: String): String {
        val value = reader.next()
        val stripped = value.removePrefix(prefix)
        if (value.length == stripped.length) {
            throw DiffParserException()
        }
        return stripped
    }

    private fun parseHunks(reader: PeekReader): List<Hunk> {
        val hunks = mutableListOf<Hunk>()
        while (reader.notEmpty && reader.peek().startsWith(Prefix.FILE_RANGE)) {
            hunks.add(parseHunk(reader))
        }
        return hunks
    }

    private fun parseHunk(reader: PeekReader): Hunk {
        val rangeValues = rangeRegex.matchEntire(reader.next())?.groupValues ?: throw DiffParserException()
        val oldRange = LineRange(rangeValues[1].toInt(), rangeValues[2].toInt())
        val newRange = LineRange(rangeValues[3].toInt(), rangeValues[4].toInt())

        val lines = mutableListOf<Line>()
        while (reader.notEmpty && isHunkLine(reader.peek())) {
            val line = reader.next()
            val kind = when (line.first()) {
                '-' -> LineKind.DELETED
                '+' -> LineKind.ADDED
                else -> LineKind.REGULAR
            }
            lines.add(Line(kind, line.drop(1)))
        }
        return Hunk(oldRange, newRange, lines)
    }

    private fun isHunkLine(line: String) =
            line.startsWith(" ") || line.startsWith("-") || line.startsWith("+")
}
