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
    private val BODY_SEPARATOR = "---\n"

    public var message: String = ""
        private set
    public var author: String = ""
        private set
    public var date: Instant = Instant.EPOCH
        private set


    public fun parse(diffFile: File) {
        var body = parseHeader(diffFile)

        assert(body.startsWith(BODY_SEPARATOR))
        body = body.drop(BODY_SEPARATOR.length)

        parseBody(body)
    }

    private fun parseHeader(file: File): String {
        val mailParser = MboxParser()
        mailParser.isTracking = true
        val handler = BodyContentHandler()
        mailParser.parse(FileInputStream(file), handler, Metadata(), ParseContext())
        assert(mailParser.trackingMetadata.size == 1)
        val header = mailParser.trackingMetadata[0]
        message = header!!.get("subject").removePrefix("[PATCH]").trim()
        author = header!!.get("Message:From-Name")
        date = Instant.parse(header!!.get("Creation-Date"))
        return handler.toString()
    }

    private fun parseBody(body: String) {
        val parser = UnifiedDiffParser()
        val stream = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
        val diffs = parser.parse(stream)
    }
}