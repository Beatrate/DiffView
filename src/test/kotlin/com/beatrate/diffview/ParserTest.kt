package com.beatrate.diffview

import com.beatrate.diffview.parser.DiffParser
import org.junit.Test
import java.io.File
import java.time.*
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun simpleHeaderTest() {
        val file = File("src/test/resources/HelloWorld.patch")
        val parser = DiffParser()
        val commit = parser.parse(file)
        assertEquals("mrkurbatov", commit.author)
        assertEquals("Update HelloWorld.java", commit.message)
        val date = ZonedDateTime.of(2017, 11, 30, 23, 20, 4, 0, ZoneOffset.ofHours(3))
        assertEquals(date, commit.date)
    }
}
