package com.beatrate.diffview.common

data class Diff(val kind: DiffKind, val from: String, val to: String, val hunks: List<Hunk>)