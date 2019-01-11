package com.beatrate.diffview.parser

import com.beatrate.diffview.common.*
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
            const val NEW_FILE = "new file mode"
            const val DELETED_FILE = "deleted file mode"
        }
    }

    private val pathRegex = Regex("a/(.+) b/(.+)")
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
        val format = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)
        val date = parsePrefixed(reader, Prefix.DATE)
                .runCatching { ZonedDateTime.from(format.parse(this)) }
                .getOrElse { throw DiffParseException("Unexpected date format") }
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
        while (reader.isNotEmpty && !reader.peek().startsWith(Prefix.FILE_RANGE)) {
            // Reached the end of this diff.
            if (reader.peek().startsWith(Prefix.DIFF_HEADER)) break
            reader.next()
        }
        val hunks = parseHunks(reader)
        return Diff(kind, oldFile, newFile, hunks)
    }

    private fun parseDiffHeader(reader: PeekReader): DiffHeader {
        val line = parsePrefixed(reader, Prefix.DIFF_HEADER)
        val (oldFile, newFile) = pathRegex.matchEntire(line)?.destructured
            ?: throw DiffParseException("Unexpected path format")
        val kind = when {
            oldFile != newFile -> DiffKind.RENAME
            reader.peek().startsWith(Prefix.NEW_FILE) -> DiffKind.CREATE
            reader.peek().startsWith(Prefix.DELETED_FILE) -> DiffKind.DELETE
            else -> DiffKind.CHANGE
        }
        return DiffHeader(oldFile, newFile, kind)
    }

    private fun parsePrefixed(reader: PeekReader, prefix: String): String {
        val value = reader.next()
        val stripped = value.removePrefix(prefix)
        if (value.length == stripped.length) {
            throw DiffParseException("Unexpected line, expected prefix: $prefix")
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
        val rangeValues = rangeRegex.matchEntire(unparsedRange)?.groupValues
            ?: throw DiffParseException("Unexpected file range format")
        val oldOffset = rangeValues[1].toInt()
        val oldLength = if (rangeValues[2].isEmpty()) 1 else rangeValues[2].toInt()
        val newOffset = rangeValues[3].toInt()
        val newLength = if (rangeValues[4].isEmpty()) 1 else rangeValues[4].toInt()

        val lines = mutableListOf<Line>()
        while (reader.isNotEmpty) {
            val content = reader.peek()
            val line = when {
                content.startsWith(Prefix.DELETED_LINE) -> Line(LineKind.DELETED, content.drop(Prefix.DELETED_LINE.length))
                content.startsWith(Prefix.ADDED_LINE) -> Line(LineKind.ADDED, content.drop(Prefix.ADDED_LINE.length))
                content.startsWith(Prefix.REGULAR_LINE) -> Line(LineKind.REGULAR, content.drop(Prefix.REGULAR_LINE.length))
                content == Prefix.NO_NEWLINE -> Line(LineKind.REGULAR, content)
                else -> null
            } ?: break

            lines.add(line)
            reader.next()
        }
        return Hunk(unparsedRange, LineRange(oldOffset, oldLength), LineRange(newOffset, newLength), lines)
    }
}
