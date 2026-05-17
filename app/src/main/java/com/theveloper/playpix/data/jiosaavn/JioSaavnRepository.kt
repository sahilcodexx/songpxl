package com.theveloper.playpix.data.jiosaavn

import com.theveloper.playpix.data.database.AlbumEntity
import com.theveloper.playpix.data.database.ArtistEntity
import com.theveloper.playpix.data.database.MusicDao
import com.theveloper.playpix.data.database.SongArtistCrossRef
import com.theveloper.playpix.data.database.SongEntity
import com.theveloper.playpix.data.database.SourceType
import com.theveloper.playpix.data.database.serializeArtistRefs
import com.theveloper.playpix.data.database.toAlbum
import com.theveloper.playpix.data.database.toArtist
import com.theveloper.playpix.data.database.toSong
import com.theveloper.playpix.data.model.Album
import com.theveloper.playpix.data.model.Artist
import com.theveloper.playpix.data.model.ArtistRef
import com.theveloper.playpix.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * Maps JioSaavn API responses to the app's Song / Album / Artist domain models
 * and caches them in the local Room database for offline access.
 *
 * ID strategy (avoids collision with MediaStore / other cloud sources):
 *   Songs   → hash of "jiosaavn_<songId>"   + SONG_ID_OFFSET   (12T range)
 *   Albums  → hash of "jiosaavn_<albumId>"  + ALBUM_ID_OFFSET  (13T range)
 *   Artists → hash of "jiosaavn_<artistId>" + ARTIST_ID_OFFSET (14T range)
 */
@Singleton
class JioSaavnRepository @Inject constructor(
    private val api: JioSaavnApiService,
    private val musicDao: MusicDao
) {
    companion object {
        private const val TAG = "JioSaavnRepo"
        private const val SONG_ID_OFFSET   = 12_000_000_000_000L
        private const val ALBUM_ID_OFFSET  = 13_000_000_000_000L
        private const val ARTIST_ID_OFFSET = 14_000_000_000_000L
        private const val PARENT_DIRECTORY = "/Cloud/JioSaavn"

        /** Pick the highest quality download URL available. */
        fun bestDownloadUrl(urls: List<JioSaavnQuality>): String {
            val preferredOrder = listOf("320kbps", "160kbps", "96kbps", "48kbps", "12kbps")
            for (quality in preferredOrder) {
                val match = urls.firstOrNull { it.quality == quality }
                if (match != null && match.url.isNotBlank()) return match.url
            }
            return urls.lastOrNull()?.url ?: ""
        }

        fun bestImageUrl(images: List<JioSaavnQuality>): String {
            val preferredOrder = listOf("500x500", "150x150", "50x50")
            for (quality in preferredOrder) {
                val match = images.firstOrNull { it.quality == quality }
                if (match != null && match.url.isNotBlank()) return match.url
            }
            return images.lastOrNull()?.url ?: ""
        }

        fun songDbId(saavnId: String): Long =
            "jiosaavn_$saavnId".hashCode().toLong().absoluteValue % 1_000_000_000L + SONG_ID_OFFSET

        fun albumDbId(saavnId: String): Long =
            "jiosaavn_album_$saavnId".hashCode().toLong().absoluteValue % 1_000_000_000L + ALBUM_ID_OFFSET

        fun artistDbId(saavnId: String): Long =
            "jiosaavn_artist_$saavnId".hashCode().toLong().absoluteValue % 1_000_000_000L + ARTIST_ID_OFFSET
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /** Search JioSaavn for songs matching [query]. Returns mapped Song list. */
    suspend fun searchSongs(query: String, limit: Int = 20): List<Song> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchSongs(query = query, limit = limit)
            if (response.status != "SUCCESS") return@withContext emptyList()
            val songs = response.data?.results ?: return@withContext emptyList()
            val entities = songs.map { it.toSongEntity() }
            cacheEntities(entities, songs)
            entities.map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "$TAG: searchSongs failed for query='$query'")
            emptyList()
        }
    }

    /**
     * Fetch songs for a specific genre and store them tagged with [genreTag] so
     * the DB genre filter can find them later.
     */
    suspend fun searchSongsForGenre(genreTag: String, limit: Int = 50): List<Song> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchSongs(query = genreTag, limit = limit)
            if (response.status != "SUCCESS") return@withContext emptyList()
            val songs = response.data?.results ?: return@withContext emptyList()
            // Override genre field so DB lookups by genre name succeed
            val entities = songs.map { it.toSongEntity().copy(genre = genreTag) }
            cacheEntities(entities, songs)
            entities.map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "$TAG: searchSongsForGenre failed for genre='$genreTag'")
            emptyList()
        }
    }

    /** Search JioSaavn for albums matching [query]. Returns mapped Album list. */
    suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchAlbums(query = query)
            if (response.status != "SUCCESS") return@withContext emptyList()
            response.data?.results?.map { it.toAlbumDomain() } ?: emptyList()
        } catch (e: Exception) {
            Timber.w(e, "$TAG: searchAlbums failed for query='$query'")
            emptyList()
        }
    }

    /** Search JioSaavn for artists matching [query]. Returns mapped Artist list. */
    suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        try {
            val response = api.searchArtists(query = query)
            if (response.status != "SUCCESS") return@withContext emptyList()
            response.data?.results?.map { it.toArtistDomain() } ?: emptyList()
        } catch (e: Exception) {
            Timber.w(e, "$TAG: searchArtists failed for query='$query'")
            emptyList()
        }
    }

    /**
     * Fetch trending songs for the home feed.
     * Rotates through genre queries daily so the home feed changes every day.
     */
    suspend fun getTrendingSongs(limit: Int = 30): List<Song> = withContext(Dispatchers.IO) {
        // Pick a query based on today's day-of-year so it changes daily without network round-trips
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val queries = listOf(
            "top hits hindi 2024",
            "best bollywood songs",
            "latest hindi songs",
            "popular punjabi songs",
            "trending indian songs",
            "best english pop songs",
            "top romantic hindi songs",
            "viral songs india",
            "best rap hindi songs",
            "top party songs india",
            "90s bollywood hits",
            "best indie songs india",
            "top lofi hindi songs",
            "best dance songs india",
            "top devotional songs india"
        )
        val query = queries[dayOfYear % queries.size]
        try {
            val response = api.searchSongs(query = query, limit = limit)
            if (response.status != "SUCCESS") return@withContext emptyList()
            val songs = response.data?.results ?: return@withContext emptyList()
            // Shuffle based on today's seed so order differs each day
            val shuffled = songs.shuffled(java.util.Random(dayOfYear.toLong()))
            val entities = shuffled.map { it.toSongEntity() }
            cacheEntities(entities, shuffled)
            entities.map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "$TAG: getTrendingSongs failed for query='$query'")
            emptyList()
        }
    }

    // ── Mapping helpers ─────────────────────────────────────────────────────

    private fun JioSaavnSong.toSongEntity(): SongEntity {
        val dbSongId   = songDbId(id)
        val dbAlbumId  = albumDbId(album?.id ?: id)
        val primaryRef = artists?.primary?.firstOrNull()
        val dbArtistId = artistDbId(primaryRef?.id ?: id)

        val streamUrl   = bestDownloadUrl(downloadUrl)
        val artUrl      = bestImageUrl(image)
        val durationMs  = (duration?.toLongOrNull() ?: 0L) * 1000L
        val yearInt     = year?.toIntOrNull() ?: 0

        val artistRefs  = buildArtistRefs()
        val artistsJson = serializeArtistRefs(artistRefs)
        val displayArtist = artistRefs.firstOrNull { it.isPrimary }?.name
            ?: primaryArtists.ifBlank { "Unknown Artist" }

        return SongEntity(
            id                  = dbSongId,
            title               = name,
            artistName          = displayArtist,
            artistId            = dbArtistId,
            albumArtist         = null,
            albumName           = album?.name ?: "",
            albumId             = dbAlbumId,
            contentUriString    = "jiosaavn://$id",
            albumArtUriString   = artUrl.ifBlank { null },
            duration            = durationMs,
            genre               = language,
            filePath            = streamUrl,        // ExoPlayer reads this field for playback
            parentDirectoryPath = PARENT_DIRECTORY,
            isFavorite          = false,
            lyrics              = null,
            trackNumber         = 0,
            discNumber          = null,
            year                = yearInt,
            dateAdded           = System.currentTimeMillis(),
            mimeType            = "audio/mpeg",
            bitrate             = 320_000,
            sampleRate          = null,
            artistsJson         = artistsJson,
            sourceType          = SourceType.JIOSAAVN
        )
    }

    private fun JioSaavnSong.buildArtistRefs(): List<ArtistRef> {
        val primary = artists?.primary?.map { ref ->
            ArtistRef(id = artistDbId(ref.id), name = ref.name, isPrimary = true)
        } ?: emptyList()
        val featured = artists?.featured?.map { ref ->
            ArtistRef(id = artistDbId(ref.id), name = ref.name, isPrimary = false)
        } ?: emptyList()
        return (primary + featured).distinctBy { it.id }
    }

    private fun JioSaavnAlbum.toAlbumDomain(): Album {
        val dbAlbumId  = albumDbId(id)
        val primaryArtist = (artists?.primary ?: primaryArtists).firstOrNull()
        val artUrl     = bestImageUrl(image)
        val yearInt    = year?.toIntOrNull() ?: 0
        return Album(
            id               = dbAlbumId,
            title            = name,
            artist           = primaryArtist?.name ?: "",
            albumArtUriString = artUrl.ifBlank { null },
            songCount        = songs?.size ?: 0,
            dateAdded        = System.currentTimeMillis(),
            year             = yearInt,
            albumArtist      = null
        )
    }

    private fun JioSaavnArtistResult.toArtistDomain(): Artist {
        val artUrl = bestImageUrl(image)
        return Artist(
            id        = artistDbId(id),
            name      = name,
            songCount = 0,
            imageUrl  = artUrl.ifBlank { null }
        )
    }

    // ── Cache helpers ───────────────────────────────────────────────────────

    private suspend fun cacheEntities(entities: List<SongEntity>, songs: List<JioSaavnSong>) {
        try {
            val albums = songs.mapNotNull { song ->
                song.album?.let { albumRef ->
                    val artistRef = song.artists?.primary?.firstOrNull()
                    AlbumEntity(
                        id               = albumDbId(albumRef.id),
                        title            = albumRef.name,
                        artistName       = artistRef?.name ?: song.primaryArtists,
                        artistId         = artistDbId(artistRef?.id ?: albumRef.id),
                        albumArtUriString = bestImageUrl(song.image).ifBlank { null },
                        songCount        = 0,
                        dateAdded        = System.currentTimeMillis(),
                        year             = song.year?.toIntOrNull() ?: 0,
                        albumArtist      = null
                    )
                }
            }.distinctBy { it.id }

            val artists = songs.flatMap { song ->
                (song.artists?.primary ?: emptyList()) + (song.artists?.featured ?: emptyList())
            }.distinctBy { it.id }.map { ref ->
                ArtistEntity(
                    id         = artistDbId(ref.id),
                    name       = ref.name,
                    trackCount = 0,
                    imageUrl   = bestImageUrl(ref.image).ifBlank { null }
                )
            }

            val crossRefs = songs.flatMap { song ->
                song.buildArtistRefs().map { artistRef ->
                    SongArtistCrossRef(
                        songId    = songDbId(song.id),
                        artistId  = artistRef.id,
                        isPrimary = artistRef.isPrimary
                    )
                }
            }

            musicDao.insertArtists(artists)
            musicDao.insertAlbums(albums)
            musicDao.insertSongs(entities)
            musicDao.insertSongArtistCrossRefs(crossRefs)
        } catch (e: Exception) {
            Timber.w(e, "$TAG: cacheEntities failed")
        }
    }
}
