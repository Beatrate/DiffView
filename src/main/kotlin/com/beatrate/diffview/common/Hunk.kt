package com.beatrate.diffview.common

data class Hunk(val unparsedRange: String, val fromRange: LineRange, val toRange: LineRange, val lines: List<Line>)