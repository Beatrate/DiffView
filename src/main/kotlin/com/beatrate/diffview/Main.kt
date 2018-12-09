package com.beatrate.diffview

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody

fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args).parseInto(::AppArguments)
    parsedArgs.run { println("yeet") }
}