package com.beatrate.diffview.parser

import com.beatrate.diffview.common.Commit
import java.io.File

interface DiffParser {
    fun parse(file: File): Commit
}