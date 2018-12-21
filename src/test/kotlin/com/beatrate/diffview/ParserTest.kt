package com.beatrate.diffview

import com.beatrate.diffview.parser.DiffParser
import org.junit.Test
import java.io.File
import java.time.*
import java.time.temporal.Temporal
import java.time.temporal.TemporalAccessor
import java.util.*
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun headerTest() {
        val file = File("src/test/resources/HelloWorld.patch")
        val parser = DiffParser()
        val commit = parser.parse(file)
        assertEquals("mrkurbatov", commit.author)
        assertEquals("[PATCH] Update HelloWorld.java", commit.message)
        val date = ZonedDateTime.of(2017, 11, 30, 23, 20, 4, 0, ZoneOffset.ofHours(3))
        assertEquals(date, commit.date)
    }

    @Test
    fun multipleChangeTest() {
        val file = File("src/test/resources/MaxHeap.patch")
        val parser = DiffParser()
        parser.parse(file)
    }
}
