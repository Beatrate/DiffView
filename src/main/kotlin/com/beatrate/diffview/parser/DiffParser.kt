import com.beatrate.diffview.common.Commit
interface DiffParser {
    fun parse(file: File): Commit
}