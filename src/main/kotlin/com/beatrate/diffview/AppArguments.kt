package com.beatrate.diffview

import com.beatrate.diffview.generator.ReportMode
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException

class AppArguments(parser: ArgParser) {
    val originalPaths by parser.adding("--file",
        help = "original file paths")
    val patchPath by parser.storing("--patch",
            help = "diff path")
    val reportPath by parser.storing("--out",
            help = "HTML output path")
    val reportMode by parser.storing("--mode", help = "report mode") {
        when(this) {
            "unified" -> ReportMode.UNIFIED
            "split" -> ReportMode.SPLIT
            else -> throw SystemExitException("MODE not recognized", 1)
        }
    }
}