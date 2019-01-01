            const val NEW_FILE = "new file mode"
            const val DELETED_FILE = "deleted file mode"
    private val pathRegex = Regex("a/(.+) b/(.+)")
                .getOrElse { throw DiffParseException("Unexpected date format") }
        while (reader.isNotEmpty && !reader.peek().startsWith(Prefix.FILE_RANGE)) {
        val (oldFile, newFile) = pathRegex.matchEntire(line)?.destructured ?: throw DiffParseException("Unexpected path format")
            oldFile != newFile -> DiffKind.RENAME
            reader.peek().startsWith(Prefix.NEW_FILE) -> DiffKind.CREATE
            reader.peek().startsWith(Prefix.DELETED_FILE) -> DiffKind.DELETE
            else -> DiffKind.CHANGE
            throw DiffParseException("Unexpected line, expected prefix: $prefix")
        val rangeValues = rangeRegex.matchEntire(unparsedRange)?.groupValues ?: throw DiffParseException("Unexpected file range format")
        val oldOffset = rangeValues[1].toInt()
        val oldLength = if (rangeValues[2].isEmpty()) 1 else rangeValues[2].toInt()
        val newOffset = rangeValues[3].toInt()
        val newLength = if (rangeValues[4].isEmpty()) 1 else rangeValues[4].toInt()
        return Hunk(unparsedRange, LineRange(oldOffset, oldLength), LineRange(newOffset, newLength), lines)