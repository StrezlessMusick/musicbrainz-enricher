# musicbrainz-enricher

> Bot to enrich musicbrainz data from linked sources.

## About

Queries musicbrainz' API and fetches data from linked source like Discogs or Spotify.

### Supported Relationship Sources

- Releases
    - Discogs
    - Spotify
    - Bandcamp
    - Apple Music / iTunes
    - Junodownload

## Configuration

The following values should be passed as startup flags (e.g. `-Dmusicbrainz-enricher.musicbrainz.username=myUserName`)

- `musicbrainz-enricher.host` (Either test.musicbrainz.org or musicbrainz.org)
- Credentials
    - Musicbrainz
        - `musicbrainz-enricher.musicbrainz.username`
        - `musicbrainz-enricher.musicbrainz.password`
    - Spotify (can be left empty to disable spotify)
        - `musicbrainz-enricher.spotify.client-id`
        - `musicbrainz-enricher.spotify.client-secret`
        
## Usage

Starting requires 2 arguments, mode and query:
`java [startup flags] -jar musicbrainz-enricher*.jar 'release' 'searchQuery'`.

### Local History DB

This application will remember entities checked already and will only re-check them after duration `n` days, where `n` defaults to 7 days. In order to reset this, delete the musicbrainz-enricher database in `~/.cache/`.
### Musicbrainz Local DB Mode 

You can massively improve throughput by using a local musicbrainz database connection instead of querying the API for enrichment targets.
Set up <https://github.com/metabrainz/musicbrainz-docker> locally with the database port open, then call this tool with the profile `musicbrainzLocalDb`, and omit the "query" parameter listed in "Usage".
