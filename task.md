# Fix: Streaming data not showing in UI

## Root Causes
1. **Genres empty** - `getGenres()` reads only from DB. On fresh install DB is empty until trending fetch completes. 
   Fix: Add hardcoded fallback genres (hindi, english, punjabi, etc) that always show even if DB is empty.

2. **Home empty** - The mix generation waits for DB songs. If trending fetch is slow, UI shows "No data".
   Fix: `getHomeMixPreviewSongs` and `getAudioFiles` already call getTrendingSongs() - but there's a timing issue.
   The inner `musicDao.getAllSongs()` Room DAO flow IS reactive, so once trending songs are inserted, it WILL re-emit.
   BUT: `homeMixPreviewSongs` StateFlow with WhileSubscribed(5000) may stop if unsubscribed briefly.
   The `DailyMixStateHolder.updateDailyMix()` also calls getTrendingSongs then getAllSongsOnce.
   
3. **Search empty** - searchSongs calls streaming API then reads DB. Should work but let's verify.

## Fixes Needed

### Fix 1: getGenres() - Add hardcoded fallback genres
In MusicRepositoryImpl.getGenres(), after fetching from DB, if result is empty OR always,
also fetch trending to populate DB + return hardcoded genres list as fallback.

Better approach: Always emit a minimum set of genres (hindi, english, punjabi, pop, bollywood etc)
even when DB has none. Then DB genres get added on top.

### Fix 2: getAudioFiles / getHomeMixPreviewSongs - ensure reactive update
The issue may be: Room DAO flow emits ONCE immediately when subscribed (with current data),
then re-emits on changes. If DB is empty when flow starts, first emission = empty.
When trending inserts happen, Room triggers invalidation → DAO flow re-emits with songs.

This SHOULD work. Let me verify there's no issue with conflate() dropping intermediate emissions.

### Fix 3: Verify search actually hits API
searchSongs flow calls streamingRepository.searchSongs() then subscribes to DB.
The DB flow gets the cached results → should work.

## Status
- [ ] Fix genres - add hardcoded fallback
- [ ] Verify and fix home data flow 
- [ ] Build v1.0.4 and release
