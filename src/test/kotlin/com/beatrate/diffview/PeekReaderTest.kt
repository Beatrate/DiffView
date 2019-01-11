package com.beatrate.diffview

import com.beatrate.diffview.parser.PeekReader
import com.beatrate.diffview.parser.peekReader
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeekReaderTest {
    @Test
    fun emptyFile() {
        val buffered = File("src/test/resources/original/VeryShortWorldStory.txt").bufferedReader()
        var line = buffered.readLine()
        while(line != null) line = buffered.readLine()
        val reader = PeekReader(buffered)
        assertTrue(reader.isEmpty)
        assertFalse(reader.isNotEmpty)
        assertFails { reader.peek() }
        assertFails { reader.next() }
    }

    @Test
    fun nonEmptyFile() {
        val reader = File("src/test/resources/original/VeryShortWorldStory.txt").peekReader()
        assertEquals("Failed SAT. Lost scholarship. Invented rocket.", reader.peek())
        assertEquals("Failed SAT. Lost scholarship. Invented rocket.", reader.next())
    }
}