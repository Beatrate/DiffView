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
import kotlin.math.max


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
                renderUnifiedLine(Line(LineKind.REGULAR, lineIterator.next()), i, (i + difference), ReportMode.UNIFIED)
            }
            oldIndex = hunk.fromRange.offset
            newIndex = hunk.toRange.offset

            for (i in 1..hunk.fromRange.length) lineIterator.next()

            // Draw all hunk lines.
            for (line in hunk.lines) {
                renderUnifiedLine(line, oldIndex, newIndex, ReportMode.UNIFIED)
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
            renderUnifiedLine(Line(LineKind.REGULAR, lineIterator.next()), oldIndex, newIndex, ReportMode.UNIFIED)
            ++oldIndex
            ++newIndex
        }
    }

    private fun TBODY.renderUnifiedLine(line: Line, oldIndex: Int, newIndex: Int, mode: ReportMode) {
        tr {
            when (line.kind) {
                LineKind.DELETED -> {
                    renderDeletedLine(line.content, oldIndex, mode)
                }
                LineKind.ADDED -> {
                    renderAddedLine(line.content, newIndex, mode)
                }
                LineKind.REGULAR -> {
                    renderRegularLine(line.content, oldIndex, newIndex, mode)
                }
            }
        }
    }

    private fun TR.renderDeletedLine(line: String, counter: Int, mode: ReportMode) {
        td("line-cell deleted") { +counter.toString() }
        if (mode == ReportMode.UNIFIED) {
            td("line-cell deleted")
        }
        td("code-cell deleted") { +line }
    }

    private fun TR.renderAddedLine(line: String, counter: Int, mode: ReportMode) {
        if (mode == ReportMode.UNIFIED) {
            td("line-cell added")
        }
        td("line-cell added") { +counter.toString() }
        td("code-cell added") { +line }
    }

    private fun TR.renderRegularLine(line: String, origCounter: Int, newCounter: Int, mode: ReportMode) {
        td("line-cell") { +origCounter.toString() }
        if (mode == ReportMode.SPLIT) {
            td("code-cell") { +line }
        }
        td("line-cell") { +newCounter.toString() }
        td("code-cell") { +line }
    }

    private fun TR.renderEmptyLine() {
        td("line-cell empty")
        td("code-cell empty")
    }

    private fun TBODY.splitView(lines: Sequence<String>, diff: Diff) {
        val lineIterator = lines.iterator()
        var oldIndex = 1
        var newIndex = 1
        for (hunk in diff.hunks) {
            val difference = newIndex - oldIndex
            // Draw regular until start hunk.
            for (i in oldIndex until hunk.fromRange.offset) {
                renderUnifiedLine(Line(LineKind.REGULAR, lineIterator.next()), i, (i + difference), ReportMode.SPLIT)
            }
            oldIndex = hunk.fromRange.offset
            newIndex = hunk.toRange.offset

            for (i in 1..hunk.fromRange.length) if (lineIterator.hasNext()) lineIterator.next()

            // Draw all hunk lines.
            val deletedLines = mutableListOf<Line>()
            val addedLines = mutableListOf<Line>()
            for (line in hunk.lines) when (line.kind) {
                LineKind.DELETED -> deletedLines.add(line)
                LineKind.ADDED -> addedLines.add(line)
                LineKind.REGULAR -> {
                    val indices = checkChanges(deletedLines, addedLines, oldIndex, newIndex)
                    oldIndex = indices.first
                    newIndex = indices.second
                    addedLines.clear()
                    deletedLines.clear()
                    renderSplitLines(line, line, oldIndex, newIndex)
                    ++oldIndex
                    ++newIndex
                }
            }
            val indices = checkChanges(deletedLines, addedLines, oldIndex, newIndex)
            oldIndex = indices.first
            newIndex = indices.second
            addedLines.clear()
            deletedLines.clear()
        }
        // Draw leftover regular.
        while (lineIterator.hasNext()) {
            renderUnifiedLine(Line(LineKind.REGULAR, lineIterator.next()), oldIndex, newIndex, ReportMode.SPLIT)
            ++oldIndex
            ++newIndex
        }
    }

    private fun TBODY.checkChanges(
        deletedLines: List<Line>,
        addedLines: List<Line>,
        oldIndex: Int,
        newIndex: Int
    ): Pair<Int, Int> {
        var currentOld = oldIndex
        var currentNew = newIndex
        if (!deletedLines.isEmpty() || !addedLines.isEmpty()) {
            for (i in 0 until max(deletedLines.size, addedLines.size)) {
                when {
                    (addedLines.size > i) && (deletedLines.size > i) -> {
                        renderSplitLines(deletedLines[i], addedLines[i], currentOld, currentNew)
                        ++currentOld
                        ++currentNew
                    }
                    addedLines.size <= i -> {
                        renderSplitLines(deletedLines[i], deletedLines[i], currentOld, currentNew)
                        ++currentOld
                    }
                    deletedLines.size <= i -> {
                        renderSplitLines(addedLines[i], addedLines[i], currentOld, currentNew)
                        ++currentNew
                    }
                }
            }
        }
        return Pair(currentOld, currentNew)
    }


    private fun TBODY.renderSplitLines(firstLine: Line, secondLine: Line, oldIndex: Int, newIndex: Int) {
        if (firstLine.kind == LineKind.REGULAR) renderUnifiedLine(firstLine, oldIndex, newIndex, ReportMode.SPLIT)
        else tr {
            if (firstLine.kind == LineKind.DELETED) {
                renderDeletedLine(firstLine.content, oldIndex, ReportMode.SPLIT)
            } else if (secondLine.kind == LineKind.ADDED) renderEmptyLine()
            if (secondLine.kind == LineKind.ADDED) {
                renderAddedLine(secondLine.content, newIndex, ReportMode.SPLIT)
            } else if (firstLine.kind == LineKind.DELETED) renderEmptyLine()
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
                backgroundColor = hex("#e6ffed")
            }
            ".added.line-cell" {
                backgroundColor = hex("#cdffd8")
            }
            ".deleted" {
                backgroundColor = hex("#ffeef0")
            }
            ".empty" {
                backgroundColor = hex("#f5f5f5")
            }
            ".deleted.line-cell" {
                backgroundColor = hex("#ffdce0")
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