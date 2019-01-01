package com.beatrate.diffview

import com.beatrate.diffview.generator.ReportMode
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException

class AppArguments(parser: ArgParser) {
    val originalFile by parser.storing("--file",
            help = "original file path")
    val patchFile by parser.storing("--patch",
            help = "diff path")
    val reportFile by parser.storing("--out",
            help = "HTML output path")
    val reportMode by parser.storing("--mode", help = "report mode") {
        when(this) {
            "unified" -> ReportMode.UNIFIED
            "split" -> ReportMode.SPLIT
            else -> throw SystemExitException("MODE not recognized", 1)
        }
    }
}