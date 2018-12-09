package com.beatrate.diffview

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException

class AppArguments(parser: ArgParser) {
    val file by parser.storing("--file",
            help = "initial file path")
    val patch by parser.storing("--patch",
            help = "diff path")
    val out by parser.storing("--out",
            help = "HTML output path")
    val mode by parser.storing("--mode", help = "visualization mode") {
        when(this) {
            "unified" -> VisualizationMode.UNIFIED
            "split" -> VisualizationMode.SPLIT
            else -> throw SystemExitException("MODE not recognized", 1)
        }
    }
}