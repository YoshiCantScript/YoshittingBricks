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
   override val mainPage = mainPageOf(
        "{\"name\":\"trending\",\"genre\":null}" to "I titoli del momento",
        "{\"name\":\"latest\",\"genre\":null}" to "Aggiunti di recente",
        "{\"name\":\"top10\",\"genre\":null}" to "Top 10 titoli di oggi",
        "{\"name\":\"genre\",\"genre\":\"Avventura\"}" to "Avventura",
        "{\"name\":\"genre\",\"genre\":\"Animazione\"}" to "Animazione",
        "{\"name\":\"genre\",\"genre\":\"Azione\"}" to "Azione",
        "{\"name\":\"genre\",\"genre\":\"Action & Adventure\"}" to "Action & Adventure",
        "{\"name\":\"genre\",\"genre\":\"Famiglia\"}" to "Famiglia",
        "{\"name\":\"genre\",\"genre\":\"Fantasy\"}" to "Fantasy",
        "{\"name\":\"genre\",\"genre\":\"Documentario\"}" to "Documentario",
        "{\"name\":\"genre\",\"genre\":\"Horror\"}" to "Horror",
        "{\"name\":\"genre\",\"genre\":\"Mistero\"}" to "Mistero",
        "{\"name\":\"genre\",\"genre\":\"Crime\"}" to "Crimine",
        "{\"name\":\"genre\",\"genre\":\"Dramma\"}" to "Dramma",
        "{\"name\":\"genre\",\"genre\":\"Commedia\"}" to "Commedia"
    )

    
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"
}
