package com.yoshi

import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.json.JSONObject
import com.fasterxml.jackson.annotation.*
import com.google.gson.annotations.SerializedName




class StreamingcommunityProvider : MainAPI() {
    override var mainUrl = "https://streamingcommunity.li"
    private var cdnUrl = "https://cdn.streamingcommunity.li" // Images
    override var name = "StreamingCommunity"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val hasChromecastSupport = true
    override var lang = "it"
    override val hasMainPage = true
    override val mainPage = listOf(
        MainPageData(
            "Popolari",
            "{name/browse/trending}"
        )
    )
    
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"
}

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search"
        val soup = app.get(
            url, params = mapOf("q" to query), headers = mapOf(
                "X-Inertia" to "true", "X-Inertia-Version" to "16d7fef7dd27890ede802c00747e79cb"
            )
        ).text
        val responseJson = parseJson<SearchResponseJson>(soup)
        return responseJson.props.titles.map { it.toSearchResult() }
    }

    private fun Episodes.toEpisode(titleId: String, season: Int): Episode {
        val data = LoadLinkData(
            titleId = titleId, episodeId = this.id.toString(), scwsId = this.scwsId.toString()
        ).toJson()

        val epNum = this.number
        val epTitle = this.name
        val posterUrl =
            "$cdnUrl/images/" + this.images.filter { it.type == "cover" }.map { it.filename }
                .firstOrNull()
        return Episode(data, epTitle, season, epNum, posterUrl = posterUrl)
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

    
//Mainly for SearchResponse
data class MainPageResponse(

    @JsonProperty("name") var name: String? = null,
    @JsonProperty("label") var label: String,
    @JsonProperty("titles") var titles: ArrayList<MainPageTitles> = arrayListOf()

)

data class MainPageTitles(

    @JsonProperty("id") var id: Int? = null,
    @JsonProperty("slug") var slug: String? = null,
    @JsonProperty("name") var name: String? = null,
    @JsonProperty("type") var type: String? = null,
    @JsonProperty("score") var score: String? = null,
    @JsonProperty("sub_ita") var subIta: Int? = null,
    @JsonProperty("last_air_date") var lastAirDate: String? = null,
    @JsonProperty("seasons_count") var seasonsCount: Int? = null,
    @JsonProperty("images") var images: ArrayList<Images> = arrayListOf()

)

data class Images(
    @JsonProperty("filename") var filename: String? = null,
    @JsonProperty("type") var type: String? = null,
)

data class RequestJson(
    @JsonProperty("name") var name: String, @JsonProperty("genre") var genre: String


)

//For search

data class SearchResponseJson(
    @JsonProperty("props") var props: Props = Props(),
)


data class Props(
    @JsonProperty("titles") var titles: ArrayList<MainPageTitles> = arrayListOf(),
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

data class Genres(
    @JsonProperty("name") var name: String? = null,
)

data class MainActors(
    @JsonProperty("name") var name: String
)

//for loadlink

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
