package com.beatrate.diffview

import com.beatrate.diffview.generator.DiffReportGenerator
import com.beatrate.diffview.generator.GitDiffReportGenerator
import com.beatrate.diffview.generator.ReportMode
import com.beatrate.diffview.parser.GitDiffParser
import org.jsoup.Jsoup
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class DiffReportGeneratorTest {

    private fun parse(path: String) = GitDiffParser().parse(File(path))

    private fun parse(file: File) = GitDiffParser().parse(file)

    private fun generate(originalPath: String, reportPath: String, patchPath: String, mode: ReportMode) =
        GitDiffReportGenerator().generate(listOf(File(originalPath)), File(reportPath), parse(patchPath), mode)

    private fun generate(originalFile: File, reportFile: File, patchFile: File, mode: ReportMode) =
        GitDiffReportGenerator().generate(listOf(originalFile), reportFile, parse(patchFile), mode)

    private fun generate(originalFiles: List<File>, reportFile: File, patchFile: File, mode: ReportMode) =
        GitDiffReportGenerator().generate(originalFiles, reportFile, parse(patchFile), mode)

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


    private fun assertFileName(originalFile: File, patchFile: File, mode: ReportMode): Boolean {
        var equalNames = false
        createFile { report ->
            generate(originalFile, report, patchFile, mode)
            Jsoup.parse(report, Charsets.UTF_8.name(), "").run {
                val name = select("title").first()
                if (name.className() == originalFile.name) equalNames = true
            }
        }
        return equalNames
    }

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
    fun multipleFiles() {
        val patch = File("src/test/resources/twoFiles.patch")
        generate(
            listOf(
                File("src/test/resources/original/RoutesController.cs"),
                File("src/test/resources/original/Trainload.xml")
            ),
            File("target/splitTwoFiles.html"),
            patch,
            ReportMode.SPLIT
        )
        generate(
            listOf(
                File("src/test/resources/original/RoutesController.cs"),
                File("src/test/resources/original/Trainload.xml")
            ),
            File("target/unifiedTwoFiles.html"),
            patch,
            ReportMode.UNIFIED
        )
    }


    @Test
    fun renamingTest() {
        val patch = File("src/test/resources/RenameFile.patch")
        generate(
            "src/test/resources/original/toRename.txt",
            "target/renamedUnified.html",
            patch.absolutePath,
            ReportMode.UNIFIED
        )
        generate(
            "src/test/resources/original/toRename.txt",
            "target/renamedSplit.html",
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
    fun onlyAddedUnified() {
        val original = File("src/test/resources/original/FirstTest.txt")
        val patch = File("src/test/resources/3LinesAdded.patch")
        val expected = listOf(
            listOf(""),
            listOf("Finite incantantem"),
            listOf("Avada Cedavra"),
            listOf("Alahamora")
        )
        assertLines(expected, original, patch, ReportMode.UNIFIED)
    }

    @Test
    fun onlyDeletedUnified() {
        val original = File("src/test/resources/original/ThirdTest.txt")
        val patch = File("src/test/resources/3LinesDeleted.patch")
        val expected = listOf(
            listOf(""),
            listOf("del"),
            listOf("del"),
            listOf("del")
        )
        assertLines(expected, original, patch, ReportMode.UNIFIED)
    }

    @Test
    fun deletedTwoAndAddedOneUnified() {
        val original = File("src/test/resources/original/SecondTest.txt")
        val patch = File("src/test/resources/2Deleted_1Added.patch")
        val expected = listOf(
            listOf("del"),
            listOf("del"),
            listOf("add")
        )
        assertLines(expected, original, patch, ReportMode.UNIFIED)
    }

    @Test
    fun deletedTwoAndAddedTwoUnified() {
        val original = File("src/test/resources/original/FourthTest.txt")
        val patch = File("src/test/resources/2Deleted_2Added.patch")
        val expected = listOf(
            listOf("del"),
            listOf("del"),
            listOf("add"),
            listOf("add")
        )
        assertLines(expected, original, patch, ReportMode.UNIFIED)
    }

    @Test
    fun deletedOneAndAddedTwoUnified() {
        val original = File("src/test/resources/original/FifthTest.txt")
        val patch = File("src/test/resources/1Deleted_2Added.patch")
        val expected = listOf(
            listOf("del"),
            listOf("add"),
            listOf("add")
        )
        assertLines(expected, original, patch, ReportMode.UNIFIED)
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


    @Test
    fun renameFile() {
        val original = File("src/test/resources/original/toRename.txt")
        val patch = File("src/test/resources/RenameFile.patch")
        assertEquals(false, assertFileName(original, patch, ReportMode.UNIFIED), "names are equal.")
        assertEquals(false, assertFileName(original, patch, ReportMode.SPLIT), "names are equal.")
    }

}