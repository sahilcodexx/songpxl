package com.theveloper.pixelplay.data.jiosaavn

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the JioSaavn unofficial API (saavn.dev).
 * Base URL: https://saavn.dev/api
 */
interface JioSaavnApiService {

    /** Search songs by query. Returns up to [limit] results. */
    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): JioSaavnSearchResponse

    /** Search albums by query. */
    @GET("search/albums")
    suspend fun searchAlbums(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): JioSaavnAlbumSearchResponse

    /** Search artists by query. */
    @GET("search/artists")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): JioSaavnArtistSearchResponse

    /** Fetch song details by JioSaavn song ID. */
    @GET("songs/{id}")
    suspend fun getSong(
        @Path("id") songId: String
    ): JioSaavnSongDetailResponse

    /** Fetch chart / trending songs (home feed). */
    @GET("charts")
    suspend fun getCharts(): JioSaavnChartsResponse
}

// ── Response models ──────────────────────────────────────────────────────────

data class JioSaavnSearchResponse(
    val success: Boolean = false,
    val data: JioSaavnSongData? = null
)

data class JioSaavnSongData(
    val total: Int = 0,
    val start: Int = 0,
    val results: List<JioSaavnSong> = emptyList()
)

data class JioSaavnAlbumSearchResponse(
    val success: Boolean = false,
    val data: JioSaavnAlbumData? = null
)

data class JioSaavnAlbumData(
    val total: Int = 0,
    val results: List<JioSaavnAlbum> = emptyList()
)

data class JioSaavnArtistSearchResponse(
    val success: Boolean = false,
    val data: JioSaavnArtistData? = null
)

data class JioSaavnArtistData(
    val total: Int = 0,
    val results: List<JioSaavnArtistResult> = emptyList()
)

data class JioSaavnSongDetailResponse(
    val success: Boolean = false,
    val data: List<JioSaavnSong> = emptyList()
)

data class JioSaavnChartsResponse(
    val success: Boolean = false,
    val data: List<JioSaavnChart> = emptyList()
)

data class JioSaavnChart(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val image: List<JioSaavnQuality> = emptyList(),
    val url: String = ""
)

data class JioSaavnSong(
    val id: String = "",
    val name: String = "",
    val year: String? = null,
    val releaseDate: String? = null,
    val duration: String? = null,   // seconds as string
    val label: String? = null,
    val primaryArtists: String = "",
    val primaryArtistsId: String = "",
    val featuredArtists: String? = null,
    val explicit: Boolean = false,
    val playCount: String? = null,
    val language: String? = null,
    val hasLyrics: String? = null,
    val url: String = "",
    val copyright: String? = null,
    val album: JioSaavnAlbumRef? = null,
    val artists: JioSaavnArtistGroup? = null,
    val image: List<JioSaavnQuality> = emptyList(),
    val downloadUrl: List<JioSaavnQuality> = emptyList()
)

data class JioSaavnAlbumRef(
    val id: String = "",
    val name: String = "",
    val url: String = ""
)

data class JioSaavnArtistGroup(
    val primary: List<JioSaavnArtistRef> = emptyList(),
    val featured: List<JioSaavnArtistRef> = emptyList(),
    val all: List<JioSaavnArtistRef> = emptyList()
)

data class JioSaavnArtistRef(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val image: List<JioSaavnQuality> = emptyList(),
    val type: String = "",
    val role: String = ""
)

data class JioSaavnQuality(
    val quality: String = "",
    val url: String = ""
)

data class JioSaavnAlbum(
    val id: String = "",
    val name: String = "",
    val year: String? = null,
    val playCount: String? = null,
    val language: String? = null,
    val explicit: Boolean = false,
    val url: String = "",
    val primaryArtists: List<JioSaavnArtistRef> = emptyList(),
    val featuredArtists: List<JioSaavnArtistRef> = emptyList(),
    val artists: JioSaavnArtistGroup? = null,
    val image: List<JioSaavnQuality> = emptyList(),
    val songs: List<JioSaavnSong>? = null
)

data class JioSaavnArtistResult(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val image: List<JioSaavnQuality> = emptyList(),
    val type: String = ""
)
