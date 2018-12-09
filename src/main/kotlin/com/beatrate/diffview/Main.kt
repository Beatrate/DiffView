package com.beatrate.diffview

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import java.io.File

fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args).parseInto(::AppArguments)
    parsedArgs.run {
        val original = File(parsedArgs.file).readLines()
        val patchFile = File(parsedArgs.patch).readLines()
        println("success")
    }
}