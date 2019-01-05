package com.beatrate.diffview.generator

import azadev.kotlin.css.*
import azadev.kotlin.css.colors.*
import azadev.kotlin.css.dimens.*
import com.beatrate.diffview.common.Commit
import com.beatrate.diffview.common.Diff
import com.beatrate.diffview.common.LineKind
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.ArrayList




class DiffReportGenerator {
    fun generate(originalFile: File, reportFile: File, commit: Commit, mode: ReportMode) {
        // If there are multiple changes in different files, pick diff related to the original file.
        val diff = commit.diffs.find { it.oldFile == originalFile.name }
            ?: throw DiffReportGenerateException("Diff isn't related to original file")
        reportFile.writeText("")
        reportFile.printWriter().use { it.appendHTML().html { create(originalFile, commit, diff, mode) } }
    }

    private fun HTML.create(originalFile: File, commit: Commit, diff: Diff, mode: ReportMode) {
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
                        if (mode == ReportMode.UNIFIED) generateUnified(originalFile, diff)
                        else generateSplit(originalFile)
                    }
                }
            }
        }
    }

    private fun TABLE.generateUnified(originalFile: File, diff: Diff) {
        tbody {
            originalFile.useLines {
                var originalFileCounter = 1
                var newFileCounter = 1
                var hunksCounter = 0
                var changed = false
                var iteratorCounter = 1
                for (line in it) {
                    if ((diff.hunks.size > 0) && (diff.hunks.size > hunksCounter)) {
                        val endOfHunk = diff.hunks[hunksCounter].fromRange.offset + diff.hunks[hunksCounter].fromRange.length - 1
                        if ((diff.hunks[hunksCounter].fromRange.offset <= originalFileCounter) &&
                            (endOfHunk >= originalFileCounter)) {
                            var i = 0
                            while (diff.hunks[hunksCounter].lines.size > i) tr {
                                if (diff.hunks[hunksCounter].lines[i].kind == LineKind.DELETED) {
                                    td("deleted-num-cell") { +(originalFileCounter).toString() }
                                    td("deleted-num-cell") { +"" }
                                    td("deleted-line-cell") { +("-  " + diff.hunks[hunksCounter].lines[i].content) }
                                    ++originalFileCounter
                                } else if (diff.hunks[hunksCounter].lines[i].kind == LineKind.ADDED) {
                                    td("added-num-cell") { +"" }
                                    td("added-num-cell") { +(newFileCounter).toString() }
                                    td("added-line-cell") { +("+  " + diff.hunks[hunksCounter].lines[i].content) }
                                    ++newFileCounter
                                } else if (diff.hunks[hunksCounter].lines[i].kind == LineKind.REGULAR) {
                                    td("line-cell") { +(originalFileCounter).toString() }
                                    td("line-cell") { +(newFileCounter).toString() }
                                    td { +("   " + diff.hunks[hunksCounter].lines[i].content) }
                                    ++originalFileCounter
                                    ++newFileCounter
                                }
                                ++i
                            }
                            changed = true
                            ++hunksCounter
                        }
                    }
                    if ((!changed) && (iteratorCounter == originalFileCounter)) tr {
                        td("line-cell") { +(originalFileCounter).toString() }
                        td("line-cell") { +(newFileCounter).toString() }
                        td { +("   " + line) }
                        ++originalFileCounter
                        ++newFileCounter
                    } else {
                        changed = false
                    }
                    ++iteratorCounter
                }
            }

        }
    }

    private fun TABLE.generateSplit(originalFile: File) {
        classes += "diff-table-split"
        var counter = 0
        tbody {
            originalFile.useLines {
                for (line in it) tr {
                    td("line-cell") { +(++counter).toString() }
                    td("diff-table-split") { +line }
                    td("line-cell") { +counter.toString() }
                    td { +"" }
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
            ".line-cell .code-cell" {
                // Use only monospaced fonts in here.
                fontSize = 14.px
                lineHeight = 20.px
            }
            ".line-cell" {
                height = 25.px
                minWidth = 30.px
                width = 1.percent
                color = rgba(27, 31, 35, 0.3)
                textAlign = RIGHT
                paddingLeft = 10.px
                paddingRight = 10.px
                overflow = HIDDEN
                textOverflow = ELLIPSIS
            }
            ".deleted-num-cell" {
//                ref = ".line-cell" //не вышло, нет такого.
                height = 25.px
                minWidth = 30.px
                width = 1.percent
                color = rgba(27, 31, 35, 0.3)
                backgroundColor = hex("#ffcdd2")
                textAlign = RIGHT
                paddingLeft = 10.px
                paddingRight = 10.px
                overflow = HIDDEN
                textOverflow = ELLIPSIS
            }
            ".added-num-cell" {
                height = 25.px
                minWidth = 30.px
                width = 1.percent
                color = rgba(27, 31, 35, 0.3)
                backgroundColor = hex("#dcedc8")
                textAlign = RIGHT
                paddingLeft = 10.px
                paddingRight = 10.px
                overflow = HIDDEN
                textOverflow = ELLIPSIS
            }
            ".deleted-line-cell" {
                backgroundColor = hex("#ffebee")
            }
            ".added-line-cell" {
                backgroundColor = hex("#f1f8e9")
            }
            ".diff-table-split .line-cell" {
                width = 40.px
            }
            ".code-cell" {
                whiteSpace = PRE_WRAP
                wordWrap = BREAK
            }
        }.render()
    }
}