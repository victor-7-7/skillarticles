package ru.skillbranch.skillarticles.data.repositories

import androidx.lifecycle.LiveData
import ru.skillbranch.skillarticles.data.AppSettings
import ru.skillbranch.skillarticles.data.ArticlePersonalInfo
import ru.skillbranch.skillarticles.data.LocalDataHolder
import ru.skillbranch.skillarticles.data.NetworkDataHolder

object ArticleRepository {
    private val local = LocalDataHolder
    private val network = NetworkDataHolder

    fun getArticle(articleId: String) =
        local.findArticle(articleId) //2s delay from db

    fun loadArticleContent(articleId: String) =
        network.loadArticleContent(articleId) //5s delay from network

    fun loadArticlePersonalInfo(articleId: String): LiveData<ArticlePersonalInfo?> {
        return local.findArticlePersonalInfo(articleId) //1s delay from db
    }

    fun updateArticlePersonalInfo(info: ArticlePersonalInfo) {
        local.updateArticlePersonalInfo(info)
    }

    //from preferences
    fun getAppSettings(): LiveData<AppSettings> = local.getAppSettings()

    fun updateSettings(appSettings: AppSettings) {
        local.updateAppSettings(appSettings)
    }
}