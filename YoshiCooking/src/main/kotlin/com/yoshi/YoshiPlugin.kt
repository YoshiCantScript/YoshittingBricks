package com.yoshi

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.APIHolder
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

@CloudstreamPlugin
class YoshiPlugin: Plugin() {
    var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as AppCompatActivity
        // All providers should be added in this manner
        registerMainAPI(StreamingcommunityProvider())

        openSettings = { ctx ->
            val frag = BlankFragment(this)
            frag.show(activity!!.supportFragmentManager, "sexFrag")
        }
    }
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
