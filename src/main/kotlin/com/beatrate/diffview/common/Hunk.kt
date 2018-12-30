package com.beatrate.diffview.common

data class Hunk(val fromRange: LineRange, val toRange: LineRange, val lines: List<Line>)