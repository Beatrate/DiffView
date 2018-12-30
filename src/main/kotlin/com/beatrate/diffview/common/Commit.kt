package com.beatrate.diffview.common

import java.time.ZonedDateTime

data class Commit(val message: String,
                  val author: String,
                  val date: ZonedDateTime,
                  val diffs: List<Diff>)