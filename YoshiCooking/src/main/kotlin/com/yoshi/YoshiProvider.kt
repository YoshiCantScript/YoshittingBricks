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

private fun MainPageTitles.toSearchResult(): SearchResponse {
        val title = this.name ?: "No title found!"
        val isMovie = this.type?.contains("movie") ?: true
        val link = LinkData(
            id = this.id, slug = this.slug
        ).toJson()

        val posterUrl =
            "$cdnUrl/images/" + this.images.filter { it.type == "poster" }.map { it.filename }
                .firstOrNull()

        return if (isMovie) {
            newMovieSearchResponse(title, link, TvType.Movie) {
                addPoster(posterUrl)
            }
        } else {
            newTvSeriesSearchResponse(title, link, TvType.TvSeries) {
                addPoster(posterUrl)
            }
        }
    }


override suspend fun search(query: String) List<SearchResponse> {
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

//For search

data class SearchResponseJson(
    @JsonProperty("props") var props: Props = Props(),
)

data class Props(
    @JsonProperty("titles") var titles: ArrayList<MainPageTitles> = arrayListOf(),
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