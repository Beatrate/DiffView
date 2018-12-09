package com.beatrate.diffview

import io.reflectoring.diffparser.api.UnifiedDiffParser
import org.apache.commons.io.input.ReaderInputStream
import org.junit.Test
import java.io.*

class ParserTest {
    private val filePath = "src/test/resources/HelloWorld.java"
    private val patchPath = "src/test/resources/HelloWorld.patch"

    @Test
    fun test() {
        val original = File(filePath)
        val reader = BufferedReader(InputStreamReader(FileInputStream(patchPath), Charsets.UTF_8))
        for (i in 1..6) {
            reader.readLine()
        }
        val parser = UnifiedDiffParser()
        val diffs = parser.parse(ReaderInputStream(reader, Charsets.UTF_8))

        println(diffs.count())
    }
}
