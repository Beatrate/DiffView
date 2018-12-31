package com.beatrate.diffview.parser

import java.io.Closeable
import java.io.Reader
import java.io.BufferedReader
import java.io.File

class PeekReader(reader: Reader) : Closeable {
    private val rawReader: BufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader)
    private var nextLine: String? = rawReader.readLine()
    val isEmpty
        get() = nextLine == null
    val isNotEmpty
        get() = !isEmpty

    fun peekOrElse(action: () -> String): String = if (isEmpty) action() else nextLine!!

    fun peek(): String = peekOrElse { throw DiffParseException("Unexpected end of file") }

    fun nextOrElse(action: () -> String): String {
        if (isEmpty) {
            return action()
        }
        val result = nextLine!!
        nextLine = rawReader.readLine()
        return result
    }

    fun next(): String = nextOrElse { throw DiffParseException("Unexpected end of file") }

    override fun close() = rawReader.close()
}

fun File.peekReader() = PeekReader(this.bufferedReader())