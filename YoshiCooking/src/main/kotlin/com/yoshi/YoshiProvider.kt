package com.yoshi

import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.LoadResponse.Companion.addRating
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer


class StreamingcommunityProvider : MainAPI() {
    override var mainUrl = "https://streamingcommunity.li"
    private var cdnUrl = "https://cdn.streamingcommunity.li" // Images
    override var name = "StreamingCommunity"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val hasChromecastSupport = true
    override var lang = "it"
    override val hasMainPage = true
   
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"

override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val dataJson = parseJson<LoadLinkData>(data)
        val document = app.get(
            "${this.mainUrl}/iframe/${dataJson.titleId}?episode_id=${dataJson.episodeId}",
            referer = mainUrl,
            headers = mapOf("User-Agent" to userAgent)
        ).document
        val firstStageUrl = document.select("iframe").attr("src")
        val documentVixcloud = app.get(
            firstStageUrl, referer = mainUrl, headers = mapOf("User-Agent" to userAgent)
        ).document.toString()
        val test =
            Regex("""window\.masterPlaylistParams = (\{[^}]+\})""").find(documentVixcloud)!!.groupValues[1].trim()
                .replace("\n", " ").replace("'", "\"")
        val tokens = parseJson<Tokens>(test)
        val realUrl = "${
            (firstStageUrl.substringBefore("?").replace("embed", "playlist"))
        }?token=${tokens.token}&token360p=${tokens.token360p}&token480p=${tokens.token480p}&token720p=${tokens.token720p}&token1080p=${tokens.token1080p}&expires=${tokens.expires}&canCast=1&n=1"
        callback.invoke(
            ExtractorLink(
                name,
                name,
                realUrl,
                isM3u8 = true,
                referer = mainUrl,
                quality = Qualities.Unknown.value
            )
        )
        return true
    }

}

override suspend fun load(url: String): LoadResponse {
        val linkData = parseJson<LinkData>(url)
        val realUrl = "${this.mainUrl}/titles/${linkData.id}-${linkData.slug}"

        val document = app.get(realUrl, referer = mainUrl).document
        val json = document.select("div#app").attr("data-page")
        val parsedJson = parseJson<LoadResponseJson>(json)

        val type = if (parsedJson.props.title.type == "tv") TvType.TvSeries else TvType.Movie

        val title = parsedJson.props.title.name ?: "No title"

        val description = parsedJson.props.title.plot
        val year = parsedJson.props.title.releaseDate?.substringAfter("-")
        val poster =
            "$cdnUrl/images/" + parsedJson.props.title.images.filter { it.type == "background" }
                .map { it.filename }.firstOrNull()
        val rating = parsedJson.props.title.score?.trim()?.toRatingInt()
        val recomm =
            parsedJson.props.sliders.firstOrNull { it.name == "related" }?.titles?.map { it.toSearchResult() }
        val actors: List<ActorData> =
            parsedJson.props.title.mainActors.map { ActorData(actor = Actor(it.name)) }

        val trailer =
            parsedJson.props.title.trailers.map { "https://www.youtube.com/watch?v=${it.youtubeId}" }
                .randomOrNull()

        val titleId = parsedJson.props.title.id.toString()

        if (type == TvType.TvSeries) {

            val seasonsCountInt = parsedJson.props.title.seasonsCount!!.toInt()

            val episodeList = (1..seasonsCountInt).map { season ->
                val documentSeason = app.get(
                    "$realUrl/stagione-$season", referer = mainUrl, headers = mapOf(
                        "X-Inertia" to "true",
                        "X-Inertia-Version" to "16d7fef7dd27890ede802c00747e79cb"
                    )
                ).text
                val parsedJsonSeason = parseJson<LoadResponseJson>(documentSeason)
                parsedJsonSeason.props.loadedSeason!!.episodes.map { it.toEpisode(titleId, season) }
            }.flatten()

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodeList) {
                this.year = year?.toIntOrNull()
                this.plot = description
                this.actors = actors
                this.recommendations = recomm
                this.tags = parsedJson.props.title.genres.mapNotNull { it.name }
                addPoster(poster)
                addRating(rating)
                addTrailer(trailer)
            }
        } else {
            val data = LoadLinkData(
                titleId = titleId,
                episodeId = titleId,
                scwsId = parsedJson.props.title.scwsId.toString()
            ).toJson()
            return newMovieLoadResponse(title, data, TvType.Movie, data) {
                this.year = year?.toIntOrNull()
                this.plot = description
                this.actors = actors
                this.recommendations = recomm
                this.tags = parsedJson.props.title.genres.mapNotNull { it.name }
                addPoster(poster)
                addRating(rating)
                addTrailer(trailer)
            }
        }
    }



// for loading links
private data class LoadLinkData(
    val titleId: String? = null, val episodeId: String? = null, val scwsId: String? = null
)

private data class Tokens(
    @JsonProperty("token") var token: String? = null,
    @JsonProperty("token360p") var token360p: String? = null,
    @JsonProperty("token480p") var token480p: String? = null,
    @JsonProperty("token720p") var token720p: String? = null,
    @JsonProperty("token1080p") var token1080p: String? = null,
    @JsonProperty("expires") var expires: String? = null
)


//for load

private data class LinkData(
    val id: Int? = null, val slug: String? = null
)

data class LoadResponseJson(
    @JsonProperty("props") var props: LoadProps = LoadProps(),

    )


data class LoadProps(
    @JsonProperty("title") var title: LoadTitle = LoadTitle(),
    @JsonProperty("loadedSeason") var loadedSeason: LoadedSeason? = LoadedSeason(),
    @JsonProperty("sliders") var sliders: ArrayList<Sliders> = arrayListOf(),
)


data class LoadedSeason(

    @JsonProperty("id") var id: Int? = null,
    @JsonProperty("number") var number: Int? = null,
    @JsonProperty("name") var name: String? = null,
    @JsonProperty("plot") var plot: String? = null,
    @JsonProperty("release_date") var releaseDate: String? = null,
    @JsonProperty("title_id") var titleId: Int? = null,
    @JsonProperty("created_at") var createdAt: String? = null,
    @JsonProperty("updated_at") var updatedAt: String? = null,
    @JsonProperty("episodes") var episodes: ArrayList<Episodes> = arrayListOf()

)

data class Episodes(

    @JsonProperty("id") var id: Int? = null,
    @JsonProperty("number") var number: Int? = null,
    @JsonProperty("name") var name: String? = null,
    @JsonProperty("plot") var plot: String? = null,
    @JsonProperty("duration") var duration: Int? = null,
    @JsonProperty("scws_id") var scwsId: Int? = null,
    @JsonProperty("season_id") var seasonId: Int? = null,
    @JsonProperty("created_by") var createdBy: String? = null,
    @JsonProperty("created_at") var createdAt: String? = null,
    @JsonProperty("updated_at") var updatedAt: String? = null,
    @JsonProperty("images") var images: ArrayList<Images> = arrayListOf()

)

data class Sliders( //To get reccomended

    @JsonProperty("name") var name: String? = null,
    @JsonProperty("label") var label: String? = null,
    @JsonProperty("titles") var titles: ArrayList<MainPageTitles> = arrayListOf()

)


data class LoadTitle(

    @JsonProperty("id") var id: Int = 0,
    @JsonProperty("name") var name: String? = null,
    @JsonProperty("slug") var slug: String? = null,
    @JsonProperty("plot") var plot: String? = null,
    @JsonProperty("quality") var quality: String? = null,
    @JsonProperty("type") var type: String? = null,
    @JsonProperty("original_name") var originalName: String? = null,
    @JsonProperty("score") var score: String? = null,
    @JsonProperty("tmdb_id") var tmdbId: Int? = null,
    @JsonProperty("imdb_id") var imdbId: String? = null, //tt11122333
    @JsonProperty("scws_id") var scwsId: Int? = null,
    @JsonProperty("release_date") var releaseDate: String? = null,
    @JsonProperty("last_air_date") var lastAirDate: String? = null,
    @JsonProperty("seasons_count") var seasonsCount: Int? = null,
    @JsonProperty("seasons") var seasons: ArrayList<Episodes> = arrayListOf(),
    @JsonProperty("trailers") var trailers: ArrayList<Trailers> = arrayListOf(),
    @JsonProperty("images") var images: ArrayList<Images> = arrayListOf(),
    @JsonProperty("genres") var genres: ArrayList<Genres> = arrayListOf(),
    @JsonProperty("main_actors") var mainActors: ArrayList<MainActors> = arrayListOf(),
)

data class Trailers(
    @JsonProperty("youtube_id") var youtubeId: String? = null,
)


data class Genres(
    @JsonProperty("name") var name: String? = null,
)

data class MainActors(
    @JsonProperty("name") var name: String
)
