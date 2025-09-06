package helpers

import java.io.FileNotFoundException

private const val GENRE_HEADER = "genre"

private const val MOVIE_TITLE_HEADER = "movie title"

val moviesByGenre: Map<String, List<String>> by lazy {
    val inputStream =
        MovieByGenreTool::class.java.classLoader.getResourceAsStream("movies.csv")
            ?: throw FileNotFoundException("File movies.csv could not be found")

    CsvReader.readCSV(
        inputStream,
        headers = listOf(MOVIE_TITLE_HEADER, GENRE_HEADER, "rating", "description"),
    )
        .groupBy(keySelector = {
            it[GENRE_HEADER]?.lowercase() ?: throw RuntimeException("Genre could not be found")
        }) {
            it[MOVIE_TITLE_HEADER] ?: throw RuntimeException("Movie title could not be found")
        }
}