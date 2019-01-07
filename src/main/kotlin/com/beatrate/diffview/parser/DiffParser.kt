import java.util.*
        val format = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)
        val (oldFile, newFile) = pathRegex.matchEntire(line)?.destructured
            ?: throw DiffParseException("Unexpected path format")
        val rangeValues = rangeRegex.matchEntire(unparsedRange)?.groupValues
            ?: throw DiffParseException("Unexpected file range format")
            val kind = when {
                line.startsWith(Prefix.DELETED_LINE) -> LineKind.DELETED
                line.startsWith(Prefix.ADDED_LINE) -> LineKind.ADDED
                line.startsWith(Prefix.REGULAR_LINE) -> LineKind.REGULAR
                line == Prefix.NO_NEWLINE -> LineKind.REGULAR
            lines.add(Line(kind, line))