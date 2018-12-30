package com.beatrate.diffview.common

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

data class Commit(var message: String = "",
                  var author: String = "",
                  var date: ZonedDateTime = Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneOffset.UTC)) {
    val diffs = mutableListOf<Diff>()
}