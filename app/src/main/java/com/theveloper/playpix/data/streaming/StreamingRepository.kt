package com.theveloper.playpix.data.streaming

import com.theveloper.playpix.data.itunes.ItunesRepository
import com.theveloper.playpix.data.jiosaavn.JioSaavnRepository
import com.theveloper.playpix.data.model.Album
import com.theveloper.playpix.data.model.Artist
import com.theveloper.playpix.data.model.Song
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for all streaming music.
 *
 * Search strategy:
 *   1. JioSaavn (Hindi + all Indian music)
 *   2. iTunes fallback (English songs) — only if JioSaavn returns nothing
 *
 * Trending feed / Daily Mix: JioSaavn charts (rotated daily).
 * Album / Artist search: JioSaavn only.
 */
@Singleton
class StreamingRepository @Inject constructor(
    private val jioSaavn: JioSaavnRepository,
    private val iTunes: ItunesRepository,
) {
    companion object {
        private const val TAG = "StreamingRepo"
    }

    /**
     * Search songs. JioSaavn first — iTunes fallback only if JioSaavn returns nothing.
     */
    suspend fun searchSongs(query: String, limit: Int = 20): List<Song> {
        val jioResults = jioSaavn.searchSongs(query = query, limit = limit)
        if (jioResults.isNotEmpty()) {
            Timber.d("$TAG: JioSaavn → ${jioResults.size} songs for '$query'")
            return jioResults
        }
        Timber.d("$TAG: JioSaavn 0 results — trying iTunes for '$query'")
        val itunesResults = iTunes.searchSongs(query = query, limit = limit)
        Timber.d("$TAG: iTunes → ${itunesResults.size} songs for '$query'")
        return itunesResults
    }

    /** Search albums — JioSaavn only. */
    suspend fun searchAlbums(query: String): List<Album> =
        jioSaavn.searchAlbums(query = query)

    /** Search artists — JioSaavn only. */
    suspend fun searchArtists(query: String): List<Artist> =
        jioSaavn.searchArtists(query = query)

    /**
     * Fetch trending songs for the home screen and Daily Mix.
     * Parallel fetch: JioSaavn trending (Hindi) + iTunes trending (English).
     */
    suspend fun getTrendingSongs(limit: Int = 30): List<Song> {
        val jioSongs = try {
            jioSaavn.getTrendingSongs(limit = limit)
        } catch (e: Exception) {
            Timber.w(e, "$TAG: JioSaavn trending failed")
            emptyList()
        }
        Timber.d("$TAG: getTrendingSongs → ${jioSongs.size} songs")
        return jioSongs
    }

    /**
     * Fetch songs for a genre tag (used by LibraryStateHolder genre section).
     */
    suspend fun searchSongsForGenre(genreTag: String, limit: Int = 50): List<Song> =
        jioSaavn.searchSongsForGenre(genreTag = genreTag, limit = limit)
}
