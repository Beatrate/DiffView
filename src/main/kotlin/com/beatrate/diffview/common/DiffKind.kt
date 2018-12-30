package com.beatrate.diffview.common

enum class DiffKind {
    CREATE, DELETE, CHANGE;

    companion object {
        private const val NULL_PATH = "/dev/null"

        fun fromPaths(fromPath: String, toPath: String): DiffKind {
            if(fromPath.startsWith("a/") && (toPath.startsWith("b/"))) {
                return CHANGE
            }
            if((fromPath == NULL_PATH) && toPath.isNotEmpty() && (toPath != NULL_PATH)) {
                return CREATE
            }
            if((toPath == NULL_PATH) && fromPath.isNotEmpty() && (fromPath != NULL_PATH)) {
                return DELETE
            }
            throw IllegalArgumentException("Illegal diff path combination.")
        }
    }
}