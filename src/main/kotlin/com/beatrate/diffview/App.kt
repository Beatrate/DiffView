package com.beatrate.diffview

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import java.io.File

class App {
    fun run(args: Array<String>) = mainBody {
        val parsedArgs = ArgParser(args).parseInto(::AppArguments)
        parsedArgs.run {
            val originalFile = File(parsedArgs.originalFile)
            val patchFile = File(parsedArgs.patchFile)
            val reportFile = File(parsedArgs.reportFile)
            if(!originalFile.isFile) {
                throw SystemExitException("FILE path doesn't exist", 1)
            }
            if(!patchFile.isFile) {
                throw SystemExitException("PATCH path doesn't exist", 1)
            }
            // TODO: Handle invalid report path.
            // Call DiffReportGenerator here.
        }
    }
}