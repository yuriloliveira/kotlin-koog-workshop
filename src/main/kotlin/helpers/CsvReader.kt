package helpers

import org.apache.commons.csv.CSVFormat
import java.io.InputStream

object CsvReader {
    fun readCSV(inputStream: InputStream, headers: List<String>): List<Map<String, String>> =
        CSVFormat.Builder.create(CSVFormat.DEFAULT).apply {
//            setHeader("movie title - year", "genre", "expanded-genres", "rating", "description")
            setHeader(*headers.toTypedArray())
            setIgnoreSurroundingSpaces(true)
        }.get().parse(inputStream.reader())
            .drop(1)
            .map { csvRecord ->
                headers.associateWith { csvRecord.get(it) }
            }
}