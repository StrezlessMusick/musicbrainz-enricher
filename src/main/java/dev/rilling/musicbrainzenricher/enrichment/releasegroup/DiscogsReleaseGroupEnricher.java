package dev.rilling.musicbrainzenricher.enrichment.releasegroup;

import dev.rilling.musicbrainzenricher.api.discogs.DiscogsMaster;
import dev.rilling.musicbrainzenricher.api.discogs.DiscogsQueryService;
import dev.rilling.musicbrainzenricher.core.DataType;
import dev.rilling.musicbrainzenricher.core.genre.GenreMatcherService;
import dev.rilling.musicbrainzenricher.enrichment.GenreEnricher;
import dev.rilling.musicbrainzenricher.util.RegexUtils;
import net.jcip.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.musicbrainz.model.RelationWs2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// https://musicbrainz.org/release-group/a63e5fa6-d6ad-47bd-986d-4a27b0c9de70
// https://www.discogs.com/master/1381500
@Service
@ThreadSafe
class DiscogsReleaseGroupEnricher implements GenreEnricher {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscogsReleaseGroupEnricher.class);

	private static final Pattern URL_REGEX = Pattern.compile("https?://www\\.discogs\\.com/master/(?<id>\\d+)");

	private final GenreMatcherService genreMatcherService;
	private final DiscogsQueryService discogsQueryService;

	DiscogsReleaseGroupEnricher(GenreMatcherService genreMatcherService, DiscogsQueryService discogsQueryService) {
		this.genreMatcherService = genreMatcherService;
		this.discogsQueryService = discogsQueryService;
	}

	@Override
	@NotNull
	public Set<String> fetchGenres(@NotNull RelationWs2 relation) {
		Optional<String> discogsId = RegexUtils.maybeGroup(URL_REGEX.matcher(relation.getTargetId()), "id");
		if (discogsId.isEmpty()) {
			LOGGER.warn("Could not find discogs ID: '{}'.", relation.getTargetId());
			return Set.of();
		}
		return discogsQueryService.lookUpMaster(discogsId.get())
			.map(release -> genreMatcherService.match(extractGenres(release)))
			.orElse(Set.of());
	}

	@NotNull
	private Set<String> extractGenres(@NotNull DiscogsMaster master) {
		Set<String> genres = new HashSet<>(master.genres());
		if (master.styles() != null) {
			genres.addAll(master.styles());
		}
		return genres;
	}

	@Override
	public boolean isRelationSupported(@NotNull RelationWs2 relation) {
		return "http://musicbrainz.org/ns/rel-2.0#discogs".equals(relation.getType()) &&
			"http://musicbrainz.org/ns/rel-2.0#url".equals(relation.getTargetType());
	}


	@Override
	@NotNull
	public DataType getDataType() {
		return DataType.RELEASE_GROUP;
	}
}
