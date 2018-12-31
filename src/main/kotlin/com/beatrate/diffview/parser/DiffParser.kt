package com.beatrate.diffview.parser

import com.beatrate.diffview.common.*
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class DiffParser {
    private data class CommitHeader(val author: String, val date: ZonedDateTime, val message: String)
    private data class DiffHeader(val oldFile: String, val newFile: String, val kind: DiffKind)

    private class Prefix {
        companion object {
            const val AUTHOR = "From: "
            const val DATE = "Date: "
            const val MESSAGE = "Subject: "
            const val FILE_RANGE = "@@ "
            const val ADDED_LINE = "+"
            const val DELETED_LINE = "-"
            const val REGULAR_LINE = " "
            const val NO_NEWLINE = "\\ No newline at end of file"

            const val DIFF_HEADER = "diff --git "
            const val NULL_PATH = "/dev/null"
        }
    }

    private val rangeRegex = Regex("""\s*@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@\s*(.+)?""")

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
                .getOrElse { throw DiffParseException() }
        val message = parsePrefixed(reader, Prefix.MESSAGE).removePrefix("[PATCH] ")
        return CommitHeader(author, date, message)
    }

    private fun parseDiffs(reader: PeekReader): List<Diff> {
        val diffs = mutableListOf<Diff>()
        while (reader.isNotEmpty) {
            if (reader.peek().startsWith(Prefix.DIFF_HEADER)) {
                diffs.add(parseDiff(reader))
            } else {
                reader.next()
            }
        }
        return diffs
    }

    private fun parseDiff(reader: PeekReader): Diff {
        val (oldFile, newFile, kind) = parseDiffHeader(reader)
        while(reader.isNotEmpty && !reader.peek().startsWith(Prefix.FILE_RANGE)) {
            // Reached the end of this diff.
            if (reader.peek().startsWith(Prefix.DIFF_HEADER)) break
            reader.next()
        }
        val hunks = parseHunks(reader)
        return Diff(kind, oldFile, newFile, hunks)
    }

    private fun parseDiffHeader(reader: PeekReader): DiffHeader {
        val line = parsePrefixed(reader, Prefix.DIFF_HEADER)
        var i = line.indexOf("b/")
        if (i == -1) i = line.indexOf(' ')
        if (i < 1) throw DiffParseException()

        val oldFile = line.substring(0, i - 1).removePrefix("a/")
        val newFile = line.substring(i).removePrefix("b/")

        val wasNull = oldFile == Prefix.NULL_PATH
        val isNull = newFile == Prefix.NULL_PATH
        val kind = when {
            !wasNull && !isNull -> if(oldFile == newFile) DiffKind.CHANGE else DiffKind.RENAME
            wasNull && !isNull -> DiffKind.CREATE
            !wasNull && isNull -> DiffKind.DELETE
            else -> throw DiffParseException()
        }
        return DiffHeader(oldFile, newFile, kind)
    }

    private fun parsePrefixed(reader: PeekReader, prefix: String): String {
        val value = reader.next()
        val stripped = value.removePrefix(prefix)
        if (value.length == stripped.length) {
            throw DiffParseException()
        }
        return stripped
    }

    private fun parseHunks(reader: PeekReader): List<Hunk> {
        val hunks = mutableListOf<Hunk>()
        while (reader.isNotEmpty && reader.peek().startsWith(Prefix.FILE_RANGE)) {
            hunks.add(parseHunk(reader))
        }
        return hunks
    }

    private fun parseHunk(reader: PeekReader): Hunk {
        val unparsedRange = reader.next()
        val rangeValues = rangeRegex.matchEntire(unparsedRange)?.groupValues ?: throw DiffParseException()
        val oldStart = rangeValues[1].toInt()
        val oldLength = if(rangeValues[2].isEmpty()) 1 else rangeValues[1].toInt()
        val newStart = rangeValues[3].toInt()
        val newLength = if(rangeValues[4].isEmpty()) 1 else rangeValues[4].toInt()

        val lines = mutableListOf<Line>()
        while (reader.isNotEmpty) {
            val line = reader.peek()
            val parsedLine = when {
                line.startsWith(Prefix.DELETED_LINE) -> Line(LineKind.DELETED, line.drop(1))
                line.startsWith(Prefix.ADDED_LINE) -> Line(LineKind.ADDED, line.drop(1))
                line.startsWith(Prefix.REGULAR_LINE) -> Line(LineKind.REGULAR, line.drop(1))
                line == Prefix.NO_NEWLINE -> Line(LineKind.REGULAR, line)
                else -> null
            } ?: break

            lines.add(parsedLine)
            reader.next()
        }
        return Hunk(unparsedRange, LineRange(oldStart, oldLength), LineRange(newStart, newLength), lines)
    }
}
