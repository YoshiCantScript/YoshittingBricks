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

 override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        if (page == 1 && request.data.contains("trending")) {
            val url = "${this.mainUrl}/api/sliders/fetch"
            val postDatas = listOf(
                "{\"sliders\":[{\"name\":\"trending\",\"genre\":null},{\"name\":\"latest\",\"genre\":null},{\"name\":\"top10\",\"genre\":null}]}",
                "{\"sliders\":[{\"name\":\"genre\",\"genre\":\"Avventura\"},{\"name\":\"genre\",\"genre\":\"Animazione\"},{\"name\":\"genre\",\"genre\":\"Azione\"},{\"name\":\"genre\",\"genre\":\"Action & Adventure\"},{\"name\":\"genre\",\"genre\":\"Famiglia\"},{\"name\":\"genre\",\"genre\":\"Fantasy\"}]}",
                "{\"sliders\":[{\"name\":\"genre\",\"genre\":\"Documentario\"},{\"name\":\"genre\",\"genre\":\"Horror\"},{\"name\":\"genre\",\"genre\":\"Mistero\"},{\"name\":\"genre\",\"genre\":\"Crime\"},{\"name\":\"genre\",\"genre\":\"Dramma\"},{\"name\":\"genre\",\"genre\":\"Commedia\"}]}"
            )
            val items: List<HomePageList> = postDatas.map { postData ->
                val soup = app.post(
                    url, json = JSONObject(postData)
                ).text
                val jsonResponse = parseJson<List<MainPageResponse>>(soup)
                jsonResponse.map { genre ->
                    val searchResponses = genre.titles.map { show -> //array of title
                        show.toSearchResult()
                    }
                    HomePageList(genre.label, searchResponses)


                }

            }.flatten()
            if (items.isEmpty()) throw ErrorLoadingException()
            return HomePageResponse(items, hasNext = true)

        } else if (page != 1 && !request.data.contains("top10")) { //to load other pages
            val offset = ((page - 1) * 30).toString()
            val requestJson = parseJson<RequestJson>(request.data)
            val url = when (requestJson.name) {
                "trending", "latest" -> "${this.mainUrl}/api/browse/${requestJson.name}"
                else -> "${this.mainUrl}/api/browse/genre"
            }
            val params = when (requestJson.name) {
                "trending", "latest" -> mapOf("offset" to offset)
                else -> mapOf("offset" to offset, "g" to requestJson.genre)
            }
            val soup = app.get(url, params = params).text
            val jsonResponse = parseJson<MainPageResponse>(soup)
            val items: List<HomePageList> = arrayListOf(
                HomePageList(jsonResponse.label, jsonResponse.titles.map { show -> //array of title
                    show.toSearchResult()
                })
            )

            return HomePageResponse(items, hasNext = true)
        }
        return HomePageResponse(arrayListOf(), hasNext = true)
    }

data class MainPageResponse(

    @JsonProperty("name") var name: String? = null,
    @JsonProperty("label") var label: String,
    @JsonProperty("titles") var titles: ArrayList<MainPageTitles> = arrayListOf()

private data class Tokens(
    @JsonProperty("token") var token: String? = null,
    @JsonProperty("token360p") var token360p: String? = null,
    @JsonProperty("token480p") var token480p: String? = null,
    @JsonProperty("token720p") var token720p: String? = null,
    @JsonProperty("token1080p") var token1080p: String? = null,
    @JsonProperty("expires") var expires: String? = null
)
