package com.beatrate.diffview

import com.beatrate.diffview.common.Commit
import com.beatrate.diffview.generator.DiffReportGenerator
import com.beatrate.diffview.generator.ReportMode
import com.beatrate.diffview.parser.DiffParser
import org.jsoup.Jsoup
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class DiffReportGeneratorTest {
    @Test
    fun test() {
        val patch = File("src/test/resources/OneFileMultipleHunksToShortStory.patch")
        generate(
            "src/test/resources/original/VeryShortWorldStory.txt",
            "target/unified.html",
            patch.absolutePath,
            ReportMode.UNIFIED
        )
        generate(
            "src/test/resources/original/VeryShortWorldStory.txt",
            "target/split.html",
            patch.absolutePath,
            ReportMode.SPLIT
        )
    }

    @Test
    fun onlyAddedSplit() {
        val original = File("src/test/resources/original/FirstTest.txt")
        val patch = File("src/test/resources/3LinesAdded.patch")
        val expected = listOf(
            listOf("", ""),
            listOf("", "Finite incantantem"),
            listOf("", "Avada Cedavra"),
            listOf("", "Alahamora")
        )
        assertLines(expected, original, patch, ReportMode.SPLIT)
    }

    @Test
    fun onlyDeletedSplit() {
        val original = File("src/test/resources/original/ThirdTest.txt")
        val patch = File("src/test/resources/3LinesDeleted.patch")
        val expected = listOf(
            listOf("", ""),
            listOf("del", ""),
            listOf("del", ""),
            listOf("del", "")
        )
        assertLines(expected, original, patch, ReportMode.SPLIT)
    }

    @Test
    fun deletedTwoAndAddedOneSplit() {
        val original = File("src/test/resources/original/SecondTest.txt")
        val patch = File("src/test/resources/2Deleted_1Added.patch")
        val expected = listOf(
            listOf("del", "add"),
            listOf("del", "")
        )
        assertLines(expected, original, patch, ReportMode.SPLIT)
    }

    @Test
    fun deletedTwoAndAddedTwoSplit() {
        val original = File("src/test/resources/original/FourthTest.txt")
        val patch = File("src/test/resources/2Deleted_2Added.patch")
        val expected = listOf(
            listOf("del", "add"),
            listOf("del", "add")
        )
        assertLines(expected, original, patch, ReportMode.SPLIT)
    }

    @Test
    fun deletedOneAndAddedTwoSplit() {
        val original = File("src/test/resources/original/FifthTest.txt")
        val patch = File("src/test/resources/1Deleted_2Added.patch")
        val expected = listOf(
            listOf("del", "add"),
            listOf("", "add")
        )
        assertLines(expected, original, patch, ReportMode.SPLIT)
    }

    @Test
    fun deletedAddedAndStaticSplit() {
        val original = File("src/test/resources/original/SixthTest.txt")
        val patch = File("src/test/resources/DeletedAddedAndStatic.patch")
        val expected = listOf(
            listOf("", ""),
            listOf("del", ""),
            listOf("", ""),
            listOf("static", "static"),
            listOf("", "add"),
            listOf("static", "static"),
            listOf("static", "static"),
            listOf("static", "static"),
            listOf("static", "static")
        )
        assertLines(expected, original, patch, ReportMode.SPLIT)
    }


    @Test
    fun deletedAddedAndStaticUnified() {
        val original = File("src/test/resources/original/SixthTest.txt")
        val patch = File("src/test/resources/DeletedAddedAndStatic.patch")
        val expected = listOf(
            listOf(""),
            listOf("del"),
            listOf(""),
            listOf("static"),
            listOf("add"),
            listOf("static"),
            listOf("static"),
            listOf("static"),
            listOf("static")
        )
        assertLines(expected, original, patch, ReportMode.UNIFIED)
    }


    private fun parse(path: String) = DiffParser().parse(File(path))

    private fun parse(file: File) = DiffParser().parse(file)

    private fun generate(originalPath: String, reportPath: String, patchPath: String, mode: ReportMode) =
        DiffReportGenerator().generate(File(originalPath), File(reportPath), parse(patchPath), mode)

    private fun generate(originalFile: File, reportFile: File, patchFile: File, mode: ReportMode) =
        DiffReportGenerator().generate(originalFile, reportFile, parse(patchFile), mode)

    private fun createFile(action: (File) -> Unit) {
        val file = File.createTempFile("temp", "html")
        action(file)
        file.deleteOnExit()
    }

    private fun assertLines(expected: List<List<String>>, actual: List<List<String>>) {
        assertEquals(expected.size, actual.size, "Expected doesn't have the same line count as actual")
        for (i in 0..expected.lastIndex) {
            assertEquals(expected[i], actual[i], "Line $i doesn't match")
        }
    }

    private fun assertLines(expected: List<List<String>>, originalFile: File, patchFile: File, mode: ReportMode) {
        createFile { report ->
            generate(originalFile, report, patchFile, mode)
            Jsoup.parse(report, Charsets.UTF_8.name(), "").run {
                val lines = select("table.diff-table").first()
                    .select("tr").map { it.select("td.code-cell").map { cell -> cell.wholeText() } }
                assertLines(expected, lines)
            }
        }
    }
}