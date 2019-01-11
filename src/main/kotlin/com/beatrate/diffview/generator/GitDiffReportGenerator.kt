package com.beatrate.diffview.generator

import azadev.kotlin.css.*
import azadev.kotlin.css.colors.*
import azadev.kotlin.css.dimens.*
import com.beatrate.diffview.common.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File
import java.time.format.DateTimeFormatter
import kotlin.math.max

class GitDiffReportGenerator : DiffReportGenerator {
    override fun generate(originalFile: File, reportFile: File, commit: Commit, mode: ReportMode) {
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
                        p {
                            if (diff.kind == DiffKind.RENAME) +"${diff.oldFile} → ${diff.newFile}"
                            else +diff.oldFile
                        }
                    }
                    table("diff-table") {
                        tbody {
                            when {
                                diff.kind == DiffKind.DELETE -> deletedView()
                                mode == ReportMode.UNIFIED -> unifiedView(originalLines, diff)
                                else -> splitView(originalLines, diff)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun TBODY.deletedView() = tr {
        td("code-cell") { +"The file was deleted." }
    }

    private fun TBODY.unifiedView(lines: Sequence<String>, diff: Diff) {
        val lineIterator = lines.iterator()
        var oldIndex = 1
        var newIndex = 1
        for (hunk in diff.hunks) {
            val difference = newIndex - oldIndex
            // Draw regular until start.
            for (i in oldIndex until hunk.fromRange.offset) {
                val line = Line(LineKind.REGULAR, lineIterator.next())
                renderUnifiedLine(line, i, i + difference)
            }
            oldIndex = hunk.fromRange.offset
            newIndex = hunk.toRange.offset

            for (i in 1..hunk.fromRange.length) lineIterator.next()

            // Draw all hunk lines.
            for (line in hunk.lines) {
                renderUnifiedLine(line, oldIndex, newIndex)
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
            val line = Line(LineKind.REGULAR, lineIterator.next())
            renderUnifiedLine(line, oldIndex, newIndex)
            ++oldIndex
            ++newIndex
        }
    }

    private fun TBODY.renderUnifiedLine(line: Line, oldIndex: Int, newIndex: Int) = tr {
        when (line.kind) {
            LineKind.REGULAR -> {
                td("line-cell") { +oldIndex.toString() }
                td("line-cell") { +newIndex.toString() }
                td("change-cell")
                td("code-cell") { +line.content }
            }
            LineKind.ADDED -> {
                td("line-cell added")
                td("line-cell added") { +newIndex.toString() }
                td("change-cell added") { +"+" }
                td("code-cell added") { +line.content }
            }
            LineKind.DELETED -> {
                td("line-cell deleted") { +oldIndex.toString() }
                td("line-cell deleted")
                td("change-cell deleted") { +"-" }
                td("code-cell deleted") { +line.content }
            }
        }
    }

    private fun TBODY.splitView(lines: Sequence<String>, diff: Diff) {
        classes += "diff-table-split"

        val lineIterator = lines.iterator()
        var oldIndex = 1
        var newIndex = 1
        for (hunk in diff.hunks) {
            val difference = newIndex - oldIndex
            // Draw regular until start hunk.
            for (i in oldIndex until hunk.fromRange.offset) {
                val line = Line(LineKind.REGULAR, lineIterator.next())
                renderSplitLine(line, line, i, i + difference)
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
                    renderSplitLine(line, line, oldIndex, newIndex)
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
            val line = Line(LineKind.REGULAR, lineIterator.next())
            renderSplitLine(line, line, oldIndex, newIndex)
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
        for (i in 0 until max(deletedLines.size, addedLines.size)) {
            when {
                (addedLines.size > i) && (deletedLines.size > i) -> {
                    renderSplitLine(deletedLines[i], addedLines[i], currentOld, currentNew)
                    ++currentOld
                    ++currentNew
                }
                addedLines.size <= i -> {
                    renderSplitLine(deletedLines[i], Line(LineKind.REGULAR, ""), currentOld, currentNew)
                    ++currentOld
                }
                deletedLines.size <= i -> {
                    renderSplitLine(Line(LineKind.REGULAR, ""), addedLines[i], currentOld, currentNew)
                    ++currentNew
                }
            }
        }
        return Pair(currentOld, currentNew)
    }

    private fun TBODY.renderSplitLine(oldLine: Line, newLine: Line, oldIndex: Int, newIndex: Int) = tr {
        if (oldLine.kind == LineKind.REGULAR && newLine.kind == LineKind.REGULAR) {
            td("line-cell") { +oldIndex.toString() }
            td("change-cell")
            td("code-cell") { +oldLine.content }
            td("line-cell") { +newIndex.toString() }
            td("change-cell")
            td("code-cell") { +newLine.content }
        } else {
            when (oldLine.kind) {
                LineKind.DELETED -> {
                    td("line-cell deleted") { +oldIndex.toString() }
                    td("change-cell deleted") { +"-" }
                    td("code-cell deleted") { +oldLine.content }
                }
                else -> {
                    td("line-cell empty")
                    td("change-cell empty")
                    td("code-cell empty")
                }
            }

            when (newLine.kind) {
                LineKind.ADDED -> {
                    td("line-cell added") { +newIndex.toString() }
                    td("change-cell added") { +"+" }
                    td("code-cell added") { +newLine.content }
                }
                else -> {
                    td("line-cell empty")
                    td("change-cell empty")
                    td("code-cell empty")
                }
            }
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
            ".line-cell, .code-cell, .change-cell" {
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
            ".change-cell" {
                width = 20.px
                textAlign = CENTER
            }
        }.render()
    }
}