# PlayPix Streaming Integration

## Goal
Replace local playback with JioSaavn + SoundCloud streaming.
**ZERO UI/UX changes.**

## API Strategy
- Search: JioSaavn first → if no results → SoundCloud
- Browse (Home/Library): JioSaavn trending/charts
- Playback: ExoPlayer already supports HTTP streams, just swap the URL

## JioSaavn (unofficial)
Base URL: https://jiosaavn-api-privatechill.vercel.app (or saavn.me)
Endpoints:
- GET /search/songs?query=&page=&limit=
- GET /search/albums?query=
- GET /search/artists?query=
- GET /songs/{id}
- GET /albums?id=
- GET /artists/{id}
- GET /charts (trending)
- GET /playlists?id=

## SoundCloud (fallback)
Use SoundCloud widget/stream API (no key needed for search)
- https://api-v2.soundcloud.com/search?q=&client_id=...
- Need a client_id (can be extracted from SC web app)

## What changes (backend only)
1. New data layer: `StreamingRepository` with JioSaavn + SoundCloud clients
2. Map API responses → existing Song/Album/Artist models (NO model changes)
3. Replace MediaStore scanner with API calls in HomeViewModel, LibraryViewModel, SearchViewModel
4. Swap playback URL in MusicService (ExoPlayer handles HTTP natively)
5. Keep Room DB for caching API results (favorites, history, playlists still work)

## What NEVER changes
- All screens, composables, navigation
- ViewModels interfaces (just swap data source)
- Models/entities used by UI
- Theme, colors, animations

## Files to modify
- data/network/ — add JioSaavnApiService, SoundCloudApiService
- data/repository/ — add StreamingRepository, modify MusicRepository
- data/model/ — NO changes
- data/worker/ — replace MediaStore sync with API prefetch
- presentation/viewmodel/ — swap repository calls only

## Status
- [ ] Explore existing repo structure
- [ ] Build JioSaavn API client
- [ ] Build SoundCloud API client  
- [ ] Build StreamingRepository (merge + fallback logic)
- [ ] Wire into existing ViewModels
- [ ] Test build
