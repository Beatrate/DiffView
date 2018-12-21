package com.beatrate.diffview.parser

import java.time.ZonedDateTime

class Commit(val message: String, val author: String, val date: ZonedDateTime?, val diffs: List<Diff>)