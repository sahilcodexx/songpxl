package com.theveloper.playpix.data.streaming

import com.theveloper.playpix.data.jiosaavn.JioSaavnRepository
import com.theveloper.playpix.data.model.Album
import com.theveloper.playpix.data.model.Artist
import com.theveloper.playpix.data.model.Song
import com.theveloper.playpix.data.soundcloud.SoundCloudRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates JioSaavn (primary) and SoundCloud (fallback) into a single streaming source.
 *
 * Search strategy:
 *   - JioSaavn is always queried first.
 *   - SoundCloud is only queried if JioSaavn returns 0 songs.
 *   - Album / Artist searches: JioSaavn only (SoundCloud has no meaningful album/artist API).
 *
 * Trending feed: JioSaavn charts populate the home screen.
 */
@Singleton
class StreamingRepository @Inject constructor(
    private val jioSaavn: JioSaavnRepository,
    private val soundCloud: SoundCloudRepository
) {
    companion object {
        private const val TAG = "StreamingRepo"
    }

    /**
     * Search for songs. JioSaavn first — SoundCloud only if JioSaavn returns nothing.
     */
    suspend fun searchSongs(query: String, limit: Int = 20): List<Song> {
        val jioSaavnResults = jioSaavn.searchSongs(query = query, limit = limit)
        if (jioSaavnResults.isNotEmpty()) {
            Timber.d("$TAG: JioSaavn returned ${jioSaavnResults.size} results for '$query'")
            return jioSaavnResults
        }
        Timber.d("$TAG: JioSaavn returned 0 results — falling back to SoundCloud for '$query'")
        return soundCloud.searchSongs(query = query, limit = limit / 2)
    }

    /**
     * Search albums — JioSaavn only.
     */
    suspend fun searchAlbums(query: String): List<Album> =
        jioSaavn.searchAlbums(query = query)

    /**
     * Search artists — JioSaavn only.
     */
    suspend fun searchArtists(query: String): List<Artist> =
        jioSaavn.searchArtists(query = query)

    /**
     * Fetch trending songs for the home feed — JioSaavn.
     */
    suspend fun getTrendingSongs(limit: Int = 30): List<Song> =
        jioSaavn.getTrendingSongs(limit = limit)
}
