package com.beatrate.diffview

import com.beatrate.diffview.generator.DiffReportGenerateException
import com.beatrate.diffview.generator.DiffReportGenerator
import com.beatrate.diffview.generator.GitDiffReportGenerator
import com.beatrate.diffview.parser.DiffParseException
import com.beatrate.diffview.parser.DiffParser
import com.beatrate.diffview.parser.GitDiffParser
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import java.io.File
import java.util.stream.Collectors

class App {
    fun run(args: Array<String>) = mainBody {
        ArgParser(args).parseInto(::AppArguments).run {


            val patchFile = File(patchPath)

            if (!patchFile.isFile) {
                throw SystemExitException("PATCH path doesn't exist", 1)
            }

            val reportFile = File(reportPath)

            val originalFiles = originalPaths.stream().map {
                val file = File(it)
                if (!file.isFile) throw SystemExitException("FILE doesn't exist", 1)
                file
            }.collect(Collectors.toList())

            try {
                val parser: DiffParser = GitDiffParser()
                val generator: DiffReportGenerator = GitDiffReportGenerator()

                generator.generate(originalFiles, reportFile, parser.parse(patchFile), reportMode)
            } catch (e: DiffParseException) {
                throw SystemExitException("Patch parsing failed: ${e.message}", 1)
            } catch (e: DiffReportGenerateException) {
                throw SystemExitException("Report generation failed: ${e.message}", 1)
            } catch (e: Exception) {
                throw SystemExitException("General error: ${e.message}", 1)
            }
        }
    }
}