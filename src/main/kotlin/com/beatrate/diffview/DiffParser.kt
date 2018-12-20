package com.beatrate.diffview

import io.reflectoring.diffparser.api.UnifiedDiffParser
import java.io.File
import java.io.FileInputStream
import java.io.ByteArrayInputStream
import java.time.Instant
import org.apache.tika.parser.mbox.MboxParser
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler

class DiffParser {
    private data class HeaderParseResult(val message: String, val author: String, val date: Instant, val body: String)

    private val BODY_SEPARATOR = "---\n"
    private val NULL_PATH = "/dev/null"

    public fun parse(diffFile: File): Commit {
        val (message, author, date, body) = parseHeader(diffFile)
        val diffs = parseBody(body)
        return Commit(message, author, date, diffs)
    }

    private fun parseHeader(file: File): HeaderParseResult {
        val mailParser = MboxParser()
        mailParser.isTracking = true
        val handler = BodyContentHandler(-1)
        mailParser.parse(FileInputStream(file), handler, Metadata(), ParseContext())

        if(mailParser.trackingMetadata.isEmpty()) throw DiffParserException("Missing metadata.")

        val header = mailParser.trackingMetadata[0]!!
        val message = header.get("subject")?.removePrefix("[PATCH]")?.trim() ?: throw DiffParserException("Missing commit message.")
        val author = header.get("Message:From-Name") ?: throw DiffParserException("Missing commit author.")
        val date = Instant.parse(header.get("Creation-Date") ?: throw DiffParserException("Missing commit date."))

        var body = handler.toString()
        if(!body.startsWith(BODY_SEPARATOR)) throw DiffParserException("Commit header end not found.")
        body = body.drop(BODY_SEPARATOR.length)
        return HeaderParseResult(message, author, date, body)
    }

    private fun parseBody(body: String): List<Diff> {
        val parser = UnifiedDiffParser()
        val stream = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
        val rawDiffs = parser.parse(stream)
        val diffs = mutableListOf<Diff>()
        return diffs
    }
}