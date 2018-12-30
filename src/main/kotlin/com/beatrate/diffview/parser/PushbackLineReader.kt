package com.beatrate.diffview.parser

import java.io.BufferedReader
import java.io.File
import java.util.Stack

class PushbackLineReader(private val reader: BufferedReader) {
    private var lines = Stack<String?>()

    fun readLine(): String? = if(lines.empty()) reader.readLine() else lines.pop()

    fun unread(line: String?) = lines.push(line)
}

fun File.pushBackLineReader() = PushbackLineReader(this.bufferedReader())