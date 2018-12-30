package com.beatrate.diffview.common

data class Diff(val from: String, val to: String, val hunks: List<Hunk>)