package com.beatrate.diffview

import com.beatrate.diffview.common.*
import com.beatrate.diffview.parser.DiffParser
import org.junit.Test
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class DiffParserTest {
    @Test
    fun header() {
        val commit = parse("src/test/resources/OneFileOneHunk.patch")
        assertEquals("Nikita Ivanov", commit.author)
        assertEquals("Update SingleChange", commit.message)
        val date = ZonedDateTime.of(2019, 1, 1, 12, 34, 41, 0, ZoneOffset.ofHours(3))
        assertEquals(date, commit.date)
    }

    @Test
    fun singleDiffSingleHunk() {
        val diff = parseDiff("src/test/resources/OneFileOneHunk.patch")
        assertEquals(DiffKind.CHANGE, diff.kind)
        assertEquals(1, diff.hunks.size)
        val hunk = diff.hunks.first()
        assertEquals("@@ -1,5 +1,8 @@", hunk.unparsedRange)
        assertEquals(1, hunk.fromRange.offset)
        assertEquals(5, hunk.fromRange.length)
        assertEquals(1, hunk.toRange.offset)
        assertEquals(8, hunk.toRange.length)
        val lines = linesOf(
                " Gown removed carelessly. Head, less so.",
                " - Joss Whedon",
                " ",
                "+Failed SAT. Lost scholarship. Invented rocket.",
                "+- William Shatner",
                "+",
                " Automobile warranty expires. So does engine.",
                " - Stan Lee"
        )
        assertEquals(lines, hunk.lines)
    }

    @Test
    fun singleDiffMultipleHunks() {
        val diff = parseDiff("src/test/resources/OneFileMultipleHunks.patch")
        val expected = listOf(
                linesOf(
                        "-Vacuum collision. Orbits diverge. Farewell, love.",
                        "-- David Brin",
                        "+“Cellar?” “Gate to, uh … hell, actually.”",
                        "+- Ronald D. Moore",
                        " ",
                        " Gown removed carelessly. Head, less so.",
                        " - Joss Whedon"
                ),
                linesOf(
                        " With bloody hands, I say good-bye.",
                        " - Frank Miller",
                        " ",
                        "-Wasted day. Wasted life. Dessert, please.",
                        "-- Steven Meretzky",
                        "+Epitaph: Foolish humans, never escaped Earth.",
                        "+- Vernor Vinge"
                )
        )
        assertEquals(expected.size, diff.hunks.size)
        for (i in 0..expected.lastIndex) {
            assertEquals(expected[i], diff.hunks[i].lines)
        }
    }

    @Test
    fun multipleFileOneHunk() {
        val diffs = parseDiffs("src/test/resources/MultipleFilesOneHunk.patch", 2)
        assertEquals(1, diffs[0].hunks.size)
        assertEquals(1, diffs[1].hunks.size)
    }

    @Test
    fun multipleFileMultipleHunks() {
        val diffs = parseDiffs("src/test/resources/MultipleFilesMultipleHunks.patch", 2)
        assertEquals(2, diffs[0].hunks.size)
        assertEquals(2, diffs[1].hunks.size)
    }

    @Test
    fun noNewline() {
        val diff = parseDiff("src/test/resources/NoNewline.patch")
        assertEquals("\\ No newline at end of file" , diff.hunks.first().lines.last().content)
    }

    @Test
    fun create() {
        val diff = parseDiff("src/test/resources/Create.patch")
        assertEquals(DiffKind.CREATE, diff.kind)
    }

    @Test
    fun createEmpty() {
        val diff = parseDiff("src/test/resources/CreateEmpty.patch")
        assertEquals(DiffKind.CREATE, diff.kind)
    }

    @Test
    fun delete() {
        val diff = parseDiff("src/test/resources/Delete.patch")
        assertEquals(DiffKind.DELETE, diff.kind)
    }

    @Test
    fun deleteEmpty() {
        val diff = parseDiff("src/test/resources/DeleteEmpty.patch")
        assertEquals(DiffKind.DELETE, diff.kind)
    }

    @Test
    fun rename() {
        val diff = parseDiff("src/test/resources/Rename.patch")
        assertEquals(DiffKind.RENAME, diff.kind)
    }

    @Test
    fun renameWithChange() {
        val commit = parse("src/test/resources/RenameWithChange.patch")
        assertEquals(1, commit.diffs.size)
        val diff = commit.diffs.first()
        assertEquals(DiffKind.RENAME, diff.kind)
        assertEquals(1, diff.hunks.size)
    }

    private fun parse(path: String): Commit = DiffParser().parse(File(path))

    private fun parseDiff(path: String): Diff {
        val commit = parse(path)
        assertEquals(1, commit.diffs.size)
        return commit.diffs.first()
    }

    private fun parseDiffs(path: String, expectedCount: Int): List<Diff> {
        val commit = parse(path)
        assertEquals(expectedCount, commit.diffs.size)
        return commit.diffs
    }

    private fun linesOf(vararg lines: String): List<Line> =
            lines.map {
                when (it.firstOrNull()) {
                    '-' -> Line(LineKind.DELETED, it.drop(1))
                    '+' -> Line(LineKind.ADDED, it.drop(1))
                    ' ' -> Line(LineKind.REGULAR, it.drop(1))
                    else -> throw IllegalArgumentException("Hunk line missing a marker")
                }
            }
}
