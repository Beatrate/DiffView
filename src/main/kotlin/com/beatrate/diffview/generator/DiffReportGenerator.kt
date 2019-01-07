package com.beatrate.diffview.generator

import azadev.kotlin.css.*
import azadev.kotlin.css.colors.*
import azadev.kotlin.css.dimens.*
import com.beatrate.diffview.common.Commit
import com.beatrate.diffview.common.Diff
import com.beatrate.diffview.common.Line
import com.beatrate.diffview.common.LineKind
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File
import java.time.format.DateTimeFormatter


class DiffReportGenerator {
    fun generate(originalFile: File, reportFile: File, commit: Commit, mode: ReportMode) {
        // If there are multiple changes in different files, pick diff related to the original file.
        val diff = commit.diffs.find { it.oldFile == originalFile.name }
            ?: throw DiffReportGenerateException("Diff isn't related to original file")
        reportFile.writeText("")

        try {
            reportFile.printWriter().use { writer ->
                writer.appendHTML().html { originalFile.useLines { create(it, commit, diff, mode) } }
            }
        } catch (e: NoSuchElementException) {
            throw DiffReportGenerateException("Unexpected end of original file")
        }
    }

    private fun HTML.create(originalLines: Sequence<String>, commit: Commit, diff: Diff, mode: ReportMode) {
        head {
            meta { charset = "UTF-8" }
            title { +commit.message }
            style { unsafe { raw(generateStyle()) } }
        }
        body {
            div("container") {
                div("commit") {
                    p("commit-title") { +commit.message }
                    div("commit-meta") {
                        p { +"${commit.author} commited on ${commit.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}" }
                    }
                }
                div("diff-view") {
                    div("file-header") {
                        p { +diff.oldFile }
                    }
                    table("diff-table") {
                        tbody {
                            if (mode == ReportMode.UNIFIED) unified(originalLines, diff)
                            else {
                                classes += "diff-table-split"
                                splitView(originalLines, diff)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun TBODY.unified(lines: Sequence<String>, diff: Diff) {
        val lineIterator = lines.iterator()
        var oldIndex = 1
        var newIndex = 1
        for (hunk in diff.hunks) {
            val difference = newIndex - oldIndex
            // Draw regular until start.
            for (i in oldIndex until hunk.fromRange.offset) {
                renderLine(Line(LineKind.REGULAR, lineIterator.next()), i, (i + difference))
            }
            oldIndex = hunk.fromRange.offset
            newIndex = hunk.toRange.offset

            for (i in 1..hunk.fromRange.length) lineIterator.next()

            // Draw all hunk lines.
            for (line in hunk.lines) {
                renderLine(line, oldIndex, newIndex)
                when (line.kind) {
                    LineKind.DELETED -> ++oldIndex
                    LineKind.ADDED -> ++newIndex
                    LineKind.REGULAR -> {
                        ++oldIndex
                        ++newIndex
                    }
                }
            }
        }
        // Draw leftover regular.
        while (lineIterator.hasNext()) {
            renderLine(Line(LineKind.REGULAR, lineIterator.next()), oldIndex, newIndex)
            ++oldIndex
            ++newIndex
        }
    }

    private fun TBODY.renderLine(line: Line, oldIndex: Int, newIndex: Int) {
        tr {
            when (line.kind) {
                LineKind.DELETED -> {
                    renderDeletedLine(line.content, oldIndex)
                }
                LineKind.ADDED -> {
                    renderAddedLine(line.content, newIndex)
                }
                LineKind.REGULAR -> {
                    renderRegularLine(line.content, oldIndex, newIndex)
                }
            }
        }
    }

    private fun TR.renderDeletedLine(line: String, counter: Int) {
        td("line-cell deleted") { +counter.toString() }
        td("line-cell deleted")
        td("code-cell deleted") { +line }
    }

    private fun TR.renderAddedLine(line: String, counter: Int) {
        td("line-cell added")
        td("line-cell added") { +counter.toString() }
        td("code-cell added") { +line }
    }

    private fun TR.renderRegularLine(line: String, origCounter: Int, newCounter: Int) {
        td("line-cell") { +origCounter.toString() }
        td("line-cell") { +newCounter.toString() }
        td("code-cell") { +line }
    }


    private fun TBODY.splitView(lines: Sequence<String>, diff: Diff) {
        var counter = 0
        for (line in lines) tr {
            td("line-cell") { +(++counter).toString() }
            td("diff-table-split") { +line }
            td("line-cell") { +counter.toString() }
            td { +"" }
        }
    }

    private fun generateStyle(): String {
        return Stylesheet {
            "*" {
                margin = 0
                padding = 0
            }
            ".container" {
                paddingLeft = 20.px
                paddingRight = 20.px
                fontFamily = "Verdana, Geneva, sans-serif"
            }
            ".commit" {
                background = hex("#eaf5ff")
                borderWidth = 1.px
                borderStyle = SOLID
                borderColor = rgba(27, 31, 35, 0.15)
                margin = box(10.px, 0)
            }
            ".commit-title" {
                fontSize = 18.px
                color = hex("#05264c")
                padding = 8.px
            }
            ".commit-meta" {
                backgroundColor = hex("#fff")
                fontSize = 14.px
                padding = 8.px
            }
            ".diff-view" {
                borderWidth = 1.px
                borderRadius = 3.px
                borderStyle = SOLID
                borderColor = hex("#ddd")
            }
            ".file-header" {
                backgroundColor = hex("#fafbfc")
                borderBottomWidth = 1.px
                borderBottomStyle = SOLID
                borderBottomColor = hex("#e1e4e8")
                padding = box(5.px, 10.px)
                fontSize = 12.px
            }
            ".diff-table" {
                borderCollapse = "collapse"
                width = 100.percent
                marginTop = 10.px
                marginBottom = 10.px
            }
            ".diff-table-split" {
                tableLayout = FIXED
            }
            ".line-cell, .code-cell" {
                // Use only monospaced fonts in here.
                fontFamily = "'Courier New',Courier,monospace"
                fontSize = 14.px
                lineHeight = 20.px
            }
            ".line-cell" {
                minWidth = 30.px
                width = 1.percent
                color = rgba(27, 31, 35, 0.3)
                textAlign = RIGHT
                paddingLeft = 10.px
                paddingRight = 10.px
                overflow = HIDDEN
                textOverflow = ELLIPSIS
            }
            ".added" {
                backgroundColor = hex("#f1f8e9")
            }
            ".added.line-cell" {
                backgroundColor = hex("#dcedc8")
            }
            ".deleted" {
                backgroundColor = hex("#ffebee")
            }
            ".deleted.line-cell" {
                backgroundColor = hex("#ffcdd2")
            }
            ".diff-table-split.line-cell" {
                width = 40.px
            }
            ".code-cell" {
                whiteSpace = PRE_WRAP
                wordWrap = BREAK
            }
        }.render()
    }
}