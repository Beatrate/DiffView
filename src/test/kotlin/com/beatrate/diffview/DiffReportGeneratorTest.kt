package com.beatrate.diffview

import com.beatrate.diffview.generator.DiffReportGenerator
import com.beatrate.diffview.generator.ReportMode
import com.beatrate.diffview.parser.DiffParser
import org.junit.Test
import java.io.File

class DiffReportGeneratorTest {
    @Test
    fun test() {
        val patch = File("src/test/resources/OneFileMultipleHunks.patch")
        generate("src/test/resources/original/Big.txt", "target/unified.html", patch.absolutePath, ReportMode.UNIFIED)
        generate("src/test/resources/original/Big.txt", "target/split.html", patch.absolutePath, ReportMode.SPLIT)
    }

    private fun parse(path: String) = DiffParser().parse(File(path))

    private fun generate(originalPath: String, reportPath: String, patchPath: String, mode: ReportMode) =
            DiffReportGenerator().generate(File(originalPath), File(reportPath), parse(patchPath), mode)
}