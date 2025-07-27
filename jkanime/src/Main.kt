package com.jkanime

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Document

class JKAnime : MainAPI() {
    override var mainUrl = "https://jkanime.net"
    override var name = "JKAnime"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasSearch = true
    override val supportedTypes = setOf(TvType.Anime)

    // Función que carga la página principal de JKAnime
    override suspend fun getMainPage(): HomePageResponse {
        val res = app.get(mainUrl)
        val document = res.document

        val animes = document.select("div.anime__item a").mapNotNull {
            val title = it.attr("title") ?: return@mapNotNull null
            val href = fixUrl(it.attr("href"))
            val img = it.selectFirst("img")?.attr("src")

            newAnimeSearchResponse(title, href, TvType.Anime) {
                this.posterUrl = img
            }
        }

        return newHomePageResponse("Últimos Animes", animes)
    }

    // Función de búsqueda
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/buscar/$query".replace(" ", "+")
        val doc = app.get(url).document

        return doc.select("div.anime__item a").mapNotNull {
            val title = it.attr("title") ?: return@mapNotNull null
            val link = fixUrl(it.attr("href"))
            val img = it.select("img").attr("src")

            newAnimeSearchResponse(title, link, TvType.Anime) {
                this.posterUrl = img
            }
        }
    }

    // Carga los episodios de un anime
    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1")?.text() ?: "Sin título"
        val poster = doc.selectFirst("div.anime__details__pic img")?.attr("src")
        val description = doc.selectFirst("p")?.text() ?: ""
        val episodes = doc.select("ul#episode_list li a").map {
            val epUrl = fixUrl(it.attr("href"))
            val epName = it.text().trim()

            Episode(epUrl, epName)
        }.reversed() // Para que estén del primero al último

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            this.episodes = episodes
        }
    }

    // Extrae los enlaces de video (puedes mejorarla más adelante)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Aquí puedes agregar scraping de iframe, por ejemplo:
        val iframe = document.selectFirst("iframe")?.attr("src")
        if (iframe != null && iframe.contains("stream")) {
            callback.invoke(
                ExtractorLink(
                    name = "JKAnime",
                    source = "jkanime.net",
                    url = iframe,
                    referer = data,
                    quality = Qualities.Unknown.value,
                    isM3u8 = false
                )
            )
        }

        return true
    }
}
