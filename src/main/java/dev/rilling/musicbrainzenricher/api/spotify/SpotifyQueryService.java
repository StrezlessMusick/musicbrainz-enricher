package dev.rilling.musicbrainzenricher.api.spotify;

import dev.rilling.musicbrainzenricher.api.BucketService;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumRequest;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

// https://github.com/thelinmichael/spotify-web-api-java
// https://developer.spotify.com/documentation/web-api/guides/
@Service
@ThreadSafe
public class SpotifyQueryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpotifyQueryService.class);

	private final SpotifyBucketProvider spotifyBucketProvider;
	private final BucketService bucketService;

	// May be null if no credentials exist.
	// Note that getAuthorizedApiClient() should be used for access.
	private final SpotifyApi apiClient;

	@GuardedBy("reAuthLock")
	private Instant tokenExpiration;

	private final Object reAuthLock = new Object();

	SpotifyQueryService(Environment environment,
						SpotifyBucketProvider spotifyBucketProvider,
						BucketService bucketService) {
		this.spotifyBucketProvider = spotifyBucketProvider;
		this.bucketService = bucketService;

		String clientId = environment.getProperty("musicbrainz-enricher.spotify.client-id");
		String clientSecret = environment.getProperty("musicbrainz-enricher.spotify.client-secret");
		apiClient = createApiClient(clientId, clientSecret);
	}

	@NotNull
	public Optional<Album> lookUpRelease(@NotNull final String id) {
		if (apiClient == null) {
			LOGGER.warn("No credentials set, skipping lookup.");
			return Optional.empty();
		}

		bucketService.consumeSingleBlocking(spotifyBucketProvider.getBucket());

		try {
			GetAlbumRequest request = getAuthorizedApiClient().getAlbum(id).build();
			return Optional.of(request.execute());
		} catch (IOException | SpotifyWebApiException | ParseException e) {
			LOGGER.warn("Could not look up album.", e);
			return Optional.empty();
		}
	}

	// https://github.com/thelinmichael/spotify-web-api-java#client-credentials-flow
	@NotNull
	private SpotifyApi getAuthorizedApiClient() throws IOException, SpotifyWebApiException, ParseException {
		if (apiClient == null) {
			throw new IllegalStateException("Cannot authorize client if none is set.");
		}

		Instant now = Instant.now();
		synchronized (reAuthLock) {
			if (tokenExpiration == null || tokenExpiration.isBefore(now)) {
				ClientCredentials credentials = apiClient.clientCredentials().build().execute();
				apiClient.setAccessToken(credentials.getAccessToken());
				tokenExpiration = now.plusSeconds(credentials.getExpiresIn());
			}
		}
		return apiClient;
	}

	@Nullable
	private SpotifyApi createApiClient(String clientId, String clientSecret) {
		if (StringUtils.isBlank(clientId) || StringUtils.isBlank(clientSecret)) {
			LOGGER.warn("No credentials set, skipping API client creation.");
			return null;
		}
		return new SpotifyApi.Builder().setClientId(clientId).setClientSecret(clientSecret).build();
	}

}
