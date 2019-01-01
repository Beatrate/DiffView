package com.beatrate.diffview

import com.beatrate.diffview.generator.DiffReportGenerateException
import com.beatrate.diffview.generator.DiffReportGenerator
import com.beatrate.diffview.parser.DiffParseException
import com.beatrate.diffview.parser.DiffParser
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import java.io.File

class App {
    fun run(args: Array<String>) = mainBody {
        ArgParser(args).parseInto(::AppArguments).run {
            val originalFile = File(originalPath)
            val patchFile = File(patchPath)

            if (!originalFile.isFile) {
                throw SystemExitException("FILE path doesn't exist", 1)
            }
            if (!patchFile.isFile) {
                throw SystemExitException("PATCH path doesn't exist", 1)
            }

            val reportFile = File(reportPath)

            try {
                val parser = DiffParser()
                val generator = DiffReportGenerator()
                generator.generate(originalFile, reportFile, parser.parse(patchFile), reportMode)
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