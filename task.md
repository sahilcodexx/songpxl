# PlayPix Logic Rewrite Task

## Status: IN PROGRESS

## Plan
Clean rewrite: remove DB dependency from streaming path, direct API → Song mapping

### Files to rewrite:
1. [x] JioSaavnApiService.kt — keep as-is (works)
2. [ ] JioSaavnRepository.kt — remove DB caching, pure API → Song mapper
3. [ ] StreamingRepository.kt — add iTunes fallback for English
4. [ ] DailyMixStateHolder.kt — remove loadPersistedDailyMix DB dependency
5. [ ] MusicRepositoryImpl.getAudioFiles() — ensure it works
6. [ ] Build APK v1.0.8

## Key Decisions
- No DB writes for streaming songs — pure in-memory
- iTunes API for English fallback: https://itunes.apple.com/search?term=X&media=music&limit=20
- Song.path = stream URL (filePath in SongEntity)  
- Song.albumArtUriString = cover image URL
- contentUriString = "jiosaavn://ID" for streaming detection
- JioSaavn: downloadUrl[].url (320kbps preferred), image[].url (500x500 preferred)

## iTunes API response shape
{
  resultCount: N,
  results: [{
    trackId, trackName, artistName, collectionName,
    previewUrl (30s AAC), artworkUrl100,
    trackTimeMillis, primaryGenreName
  }]
}

## Build config
- Keystore: /home/user/PixelPlayer/vz-playpix.jks, alias: songpxl, pass: songpxl123
- Version: 1.0.8 (code 9)
- Output: app/build/outputs/apk/release/
