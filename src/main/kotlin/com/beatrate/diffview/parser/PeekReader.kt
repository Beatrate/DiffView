package com.beatrate.diffview.parser

import java.io.Closeable
import java.io.Reader
import java.io.BufferedReader
import java.io.File

class PeekReader(reader: Reader) : Closeable {
    private val rawReader: BufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)
    private var nextLine: String? = rawReader.readLine()
    val empty
        get() = nextLine == null
    val notEmpty
        get() = !empty

    fun peekOr(action: () -> String): String = if (empty) action() else nextLine!!

    fun peek(): String = peekOr { throw DiffParserException("Unexpected end of file") }

    fun nextOr(action: () -> String): String {
        if (empty) {
            return action()
        }
        val result = nextLine!!
        nextLine = rawReader.readLine()
        return result
    }

    fun next(): String = nextOr { throw DiffParserException("Unexpected end of file") }

    override fun close() = rawReader.close()
}

fun File.peekReader() = PeekReader(this.bufferedReader())