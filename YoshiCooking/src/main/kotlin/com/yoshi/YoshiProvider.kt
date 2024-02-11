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
import com.lagradost.cloudstream3.*
import okhttp3.MediaType.Companion.toMediaType
import java.net.URLEncoder


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
     "Trending",
      "$mainUrl/browse/trending"
   ),   
    
   )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val reqlink = request.data

        return HomePageResponse(
                listOf(HomePageList(request.name, home)), hasNext = home.isNotEmpty() )
    }



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
}