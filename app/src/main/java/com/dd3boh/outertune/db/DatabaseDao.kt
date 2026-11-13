package com.dd3boh.outertune.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.dd3boh.outertune.db.daos.AlbumsDao
import com.dd3boh.outertune.db.daos.ArtistsDao
import com.dd3boh.outertune.db.daos.PlaylistsDao
import com.dd3boh.outertune.db.daos.QueueDao
import com.dd3boh.outertune.db.daos.SongsDao
import com.dd3boh.outertune.db.entities.AlbumEntity
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.Event
import com.dd3boh.outertune.db.entities.EventWithSong
import com.dd3boh.outertune.db.entities.FormatEntity
import com.dd3boh.outertune.db.entities.GenreEntity
import com.dd3boh.outertune.db.entities.LyricsEntity
import com.dd3boh.outertune.db.entities.QueueEntity
import com.dd3boh.outertune.db.entities.QueueSongMap
import com.dd3boh.outertune.db.entities.SearchHistory
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongAlbumMap
import com.dd3boh.outertune.db.entities.SongArtistMap
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.db.entities.SongGenreMap
import com.dd3boh.outertune.db.entities.SongWithPlaycount
import com.dd3boh.outertune.extensions.toSQLiteQuery
import com.dd3boh.outertune.models.MediaMetadata
import com.dd3boh.outertune.models.MultiQueueObject
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao : SongsDao, AlbumsDao, ArtistsDao, PlaylistsDao, QueueDao {

    // TODO: random selection or algorithm     fun quickPicks(now: Long = System.currentTimeMillis()): Flow<List<Song>>
    @Transaction
    @Query("SELECT * FROM song ORDER BY RANDOM() LIMIT 20")
    fun quickPicks(): Flow<List<Song>>

    // ---- Sonora ----

    /** Canciones reproducidas recientemente, ordenadas por la última reproducción. */
    @Transaction
    @Query("""
        SELECT song.* FROM song
        JOIN (SELECT songId, MAX(timestamp) AS lastPlayed FROM event GROUP BY songId) AS recent
        ON song.id = recent.songId
        ORDER BY recent.lastPlayed DESC
        LIMIT :limit
    """)
    fun recentlyPlayedSongs(limit: Int = 12): Flow<List<Song>>

    /** Selección aleatoria de canciones de la biblioteca (para mezclas). */
    @Transaction
    @Query("SELECT * FROM song ORDER BY RANDOM() LIMIT :limit")
    fun randomSongs(limit: Int = 50): Flow<List<Song>>

    /** Canciones favoritas en orden aleatorio. */
    @Transaction
    @Query("SELECT * FROM song WHERE liked = 1 ORDER BY RANDOM() LIMIT :limit")
    fun randomLikedSongs(limit: Int = 50): Flow<List<Song>>

    /** Tiempo total escuchado (ms) desde la marca de tiempo indicada. */
    @Query("SELECT COALESCE(SUM(playTime), 0) FROM event WHERE timestamp > :fromTimeStamp")
    fun totalPlayTime(fromTimeStamp: Long): Flow<Long>

    /** Número de canciones distintas escuchadas desde la marca de tiempo indicada. */
    @Query("SELECT COUNT(DISTINCT songId) FROM event WHERE timestamp > :fromTimeStamp")
    fun distinctSongsPlayed(fromTimeStamp: Long): Flow<Int>

    /** Géneros que tienen al menos una canción, ordenados por cantidad de canciones. */
    @Query("""
        SELECT genre.* FROM genre
        JOIN song_genre_map ON genre.id = song_genre_map.genreId
        GROUP BY genre.id
        ORDER BY COUNT(song_genre_map.songId) DESC
        LIMIT :limit
    """)
    fun topGenres(limit: Int = 12): Flow<List<GenreEntity>>

    /** Canciones de un género en orden aleatorio. */
    @Transaction
    @Query("""
        SELECT * FROM song
        WHERE id IN (SELECT songId FROM song_genre_map WHERE genreId = :genreId)
        ORDER BY RANDOM()
        LIMIT :limit
    """)
    fun songsByGenre(genreId: String, limit: Int = 100): Flow<List<Song>>

    /** Número de reproducciones registradas desde la marca de tiempo indicada. */
    @Query("SELECT COUNT(*) FROM event WHERE timestamp > :fromTimeStamp")
    fun playEventCount(fromTimeStamp: Long): Flow<Int>

    @Query("SELECT * FROM format WHERE id = :id")
    fun format(id: String?): Flow<FormatEntity?>

    @Query("SELECT * FROM lyrics WHERE id = :id")
    fun lyrics(id: String?): Flow<LyricsEntity?>

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId DESC")
    fun events(): Flow<List<EventWithSong>>

    @Query("DELETE FROM event")
    fun clearListenHistory()

    @Query("SELECT * FROM search_history WHERE `query` LIKE :query || '%' ORDER BY id DESC")
    fun searchHistory(query: String = ""): Flow<List<SearchHistory>>

    @Query("DELETE FROM search_history")
    fun clearSearchHistory()

    @Transaction
    @Query("SELECT * FROM song WHERE :localOnly = isLocal")
    fun dumpPlayHistory(localOnly: Boolean): List<SongWithPlaycount>

    @Query("""
        SELECT * FROM genre
        ORDER BY genre.title ASC
    LIMIT :previewSize""")
    fun allgenresByName(previewSize: Int = Int.MAX_VALUE): List<GenreEntity>

    @Query("SELECT * FROM genre WHERE id = :id")
    fun genreById(id: String): GenreEntity?

    @Query("SELECT * FROM genre WHERE title = :name")
    fun genreByName(name: String): GenreEntity?

    @Query("SELECT * FROM genre WHERE title LIKE '%' || :query || '%' LIMIT :previewSize")
    fun genreByNameFuzzy(query: String, previewSize: Int = Int.MAX_VALUE): List<GenreEntity>

    @Transaction
    @Query("UPDATE song_genre_map SET genreId = :newId WHERE genreId = :oldId")
    fun updateSongGenreMap(oldId: String, newId: String)

    @Query(
        """
        DELETE FROM genre
        WHERE NOT EXISTS (
            SELECT 1
            FROM song_genre_map
            WHERE song_genre_map.genreId = :genreId
        )
        AND id = :genreId
    """
    )
    fun safeDeleteGenre(genreId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(genre: GenreEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: SongGenreMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(searchHistory: SearchHistory)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: Event)

    @Transaction
    fun insert(mediaMetadata: MediaMetadata, block: (SongEntity) -> SongEntity = { it }) {
        if (insert(mediaMetadata.toSongEntity().let(block)) == -1L) return
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId()
            insert( // TODO: use upsert???
                ArtistEntity(
                    id = artistId,
                    name = artist.name,
                )
            )
            insert(
                SongArtistMap(
                    songId = mediaMetadata.id,
                    artistId = artistId,
                    position = index
                )
            )
        }
        mediaMetadata.genre?.forEachIndexed { index, genre ->
            val genreId = genreByName(genre.title)?.id ?: GenreEntity.generateGenreId()
            insert( // TODO: use upsert???
                GenreEntity(
                    id = genreId,
                    title = genre.title,
                )
            )
            insert(
                SongGenreMap(
                    songId = mediaMetadata.id,
                    genreId = genreId,
                    index = index
                )
            )
        }

        mediaMetadata.album?.let {
            val album = albumsByName(it.title)
            val albumId = album?.id ?: GenreEntity.generateGenreId()
            upsert(
                AlbumEntity(
                    id = albumId,
                    title = it.title,
                    thumbnailUrl = album?.thumbnailUrl?: mediaMetadata.thumbnailUrl,
                    songCount = 1,
                    duration = (album?.duration ?: 0) + mediaMetadata.duration,
                )
            )
            insert(
                SongAlbumMap(
                    songId = mediaMetadata.id,
                    albumId = albumId,
                    index = album?.songCount ?: 0
                )
            )
        }
    }

    @Upsert
    fun upsert(lyrics: LyricsEntity)

    @Upsert
    fun upsert(format: FormatEntity)

    @Delete
    fun delete(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics where id = :id")
    fun deleteLyricById(id: String)

    @Delete
    fun delete(searchHistory: SearchHistory)

    @Delete
    fun delete(event: Event)


    /**
     * WARNING: This removes all queue song data and re-adds the queue. Did you mean to use updateQueue()?
     */
    @Transaction
    fun saveQueue(mq: MultiQueueObject) {
        if (mq.queue.isEmpty()) {
            return
        }

        insert(
            QueueEntity(
                id = mq.id,
                title = mq.title,
                shuffled = mq.shuffled,
                queuePos = mq.queuePos,
                lastSongPos = mq.lastSongPos,
                index = mq.index,
            )
        )

        deleteAllQueueSongs(mq.id)
        // insert songs

        // why does kotlin not have for i loop???
        var i = 0
        while (i < mq.getSize()) {
            insert(mq.queue[i]) // make sure song exists
            insert(
                QueueSongMap(
                    queueId = mq.id,
                    songId = mq.queue[i].id,
                    index = i.toLong(),
                    shuffledIndex = mq.queue[i].shuffleIndex.toLong()
                )
            )
            i ++
        }
    }

    /**
     * Nukes
     */

    @Transaction
    @Query("DELETE FROM genre")
    fun nukeLocalGenre()

    @Transaction
    @Query("""
DELETE FROM format 
WHERE format.id IS NOT NULL 
AND NOT EXISTS (
    SELECT 1 FROM song WHERE song.id = format.id
);
    """)
    fun nukeDanglingFormatEntities()

    @Transaction
    @Query("DELETE FROM lyrics WHERE lyrics.id IN (SELECT song.id FROM song WHERE song.isLocal)")
    fun nukeLocalLyrics()

    @Transaction
    @Query("DELETE FROM lyrics WHERE lyrics.id NOT IN (SELECT song.id FROM song)")
    fun nukeDanglingLyrics()

    @Transaction
    @Query("DELETE FROM playlist WHERE isLocal = 0")
    fun nukeRemotePlaylists()

    @Transaction
    fun nukeLocalData() {
        nukeLocalSongs()
        nukeArtists()
        nukeAlbums()
        nukeLocalGenre()
    }

    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        raw("PRAGMA wal_checkpoint(FULL)".toSQLiteQuery())
    }
}
