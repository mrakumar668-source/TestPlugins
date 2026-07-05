package com.example.miruro

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class MiruroProvider : MainAPI() {
    override var mainUrl = "https://miruro.tv"
    override var name = "Miruro"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.Movie,
        TvType.OVA,
        TvType.Special
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest?
    ): HomePageResponse {
        val doc = app.get("$mainUrl/home").document
        val items = ArrayList<HomePageList>()

        val trending = doc.select("section.trending .item").mapNotNull { element ->
            element.toSearchResult()
        }
        if (trending.isNotEmpty()) {
            items.add(HomePageList("Trending", trending))
        }

        val recent = doc.select("section.recent-episodes .item").mapNotNull { element ->
            element.toSearchResult()
        }
        if (recent.isNotEmpty()) {
            items.add(HomePageList("Recent Episodes", recent))
        }

        val popular = doc.select("section.popular .item").mapNotNull { element ->
            element.toSearchResult()
        }
        if (popular.isNotEmpty()) {
            items.add(HomePageList("Popular", popular))
        }

        return HomePageResponse(items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/search?keyword=${query.replace(" ", "+")}").document
        return doc.select(".search-results .item").mapNotNull { element ->
            element.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.select("h1.title").text()
        val poster = doc.select("img.poster").attr("src")
        val year = doc.select(".info .year").text().toIntOrNull()
        val rating = doc.select(".info .rating").text().toFloatOrNull()
        val description = doc.select(".description").text()
        val genres = doc.select(".genres a").map { it.text() }

        val episodes = doc.select(".episodes-list .episode").map { element ->
            Episode(
                name = element.select(".episode-title").text(),
                link = element.select("a").attr("href"),
                episode = element.select(".episode-number").text().toIntOrNull() ?: 0
            )
        }.reversed()

        return AnimeLoadResponse(
            title = title,
            url = url,
            posterUrl = poster,
            year = year,
            rating = rating,
            plot = description,
            genres = genres,
            episodes = episodes,
            type = TvType.Anime
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        val iframeSrc = doc.select("iframe[src*=/embed/]").attr("src")
        val videoSrc = doc.select("video source").attr("src")

        val finalUrl = when {
            iframeSrc.isNotBlank() -> iframeSrc
            videoSrc.isNotBlank() -> videoSrc
            else -> {
                val scriptMatch = Regex("""(https?://[^"']*\.m3u8[^"']*)""")
                    .find(doc.html())
                scriptMatch?.groupValues?.get(1) ?: return false
            }
        }

        val quality = doc.select(".quality-selector .active").text()
            .ifEmpty { "Default" }

        callback.invoke(
            ExtractorLink(
                source = name,
                name = quality,
                url = finalUrl,
                quality = getQualityFromString(quality),
                type = if (finalUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO,
                headers = mapOf(
                    "Referer" to mainUrl,
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            )
        )

        return true
    }

    private fun org.jsoup.nodes.Element.toSearchResult(): SearchResponse? {
        val link = this.select("a").attr("href")
        val title = this.select(".title").text()
        val poster = this.select("img").attr("src")
        val episode = this.select(".episode-badge").text().toIntOrNull()

        return if (link.isNotBlank() && title.isNotBlank()) {
            AnimeSearchResponse(
                title = title,
                url = link,
                posterUrl = poster,
                type = TvType.Anime,
                episode = episode
            )
        } else null
    }

    private fun getQualityFromString(quality: String): Int {
        return when {
            quality.contains("1080") -> 1080
            quality.contains("720") -> 720
            quality.contains("480") -> 480
            quality.contains("360") -> 360
            else -> 0
        }
    }
}