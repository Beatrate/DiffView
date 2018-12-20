package com.beatrate.diffview

import java.time.Instant

class Commit(val message: String, val author: String, val date: Instant, val diffs: List<Diff>)