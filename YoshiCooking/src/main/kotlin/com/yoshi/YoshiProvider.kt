package com.yoshi

import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.SearchResponse

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
   override suspend search(query: string): List<SearchResponse> {
    val url = "mainUrl/search"
    val soup = app.get(
        ulr, params = mapOf("q" to query), headers = mapOf(
             "X-Inertia" to "true", "X-Inertia-Version" to "16d7fef7dd27890ede802c00747e79cb"
            )
        ).text
        val responseJson = parseJson<SearchResponseJson>(soup)
        return responseJson.props.titles.map { it.toSearchResult() }
    }

        