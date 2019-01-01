package com.beatrate.diffview.common

data class Diff(val kind: DiffKind, val oldFile: String, val newFile: String, val hunks: List<Hunk>)