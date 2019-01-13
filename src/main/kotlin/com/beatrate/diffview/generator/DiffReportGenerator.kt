package com.beatrate.diffview.generator

import com.beatrate.diffview.common.Commit
import java.io.File

interface DiffReportGenerator {
    fun generate(originalFiles: List<File>, reportFile: File, commit: Commit, mode: ReportMode)
}