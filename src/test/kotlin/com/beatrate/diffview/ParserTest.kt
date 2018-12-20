package com.beatrate.diffview

import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun headerTest() {
        val file = File("src/test/resources/HelloWorld.patch")
        val parser = DiffParser()
        val commit = parser.parse(file)
        assertEquals("mrkurbatov", commit.author)
        assertEquals("Update HelloWorld.java", commit.message)
        val date = LocalDateTime.of(2017, Month.NOVEMBER, 30, 23, 20, 4)
        val instant = date.atOffset(ZoneOffset.ofHours(3)).toInstant()
        assertEquals(instant, commit.date)
    }

    @Test
    fun multipleChangeTest() {
        val file = File("src/test/resources/MaxHeap.patch")
        val parser = DiffParser()
        parser.parse(file)
    }
}
