package ru.skillbranch.skillarticles.data.remote

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*
import ru.skillbranch.skillarticles.data.remote.req.*
import ru.skillbranch.skillarticles.data.remote.res.*

interface RestService {
    // Если limit > 0, значит в БД на устройстве еще не было сохранено ни одного
    // статейного айтема (для показа списка статей, без контента). И при этом
    // параметр last будет null. Если limit < 0, например (-12), значит сервер
    // должен отдать 12 статейных айтемов, расположенных в базе сервера после
    // айтема с идентификатором из параметра last
    // https://skill-articles.skill-branch.ru/api/v1/articles?last={articleId}&limit=10
    @GET("articles")
    suspend fun articles(
        @Query("last") last: String? = null,
        @Query("limit") limit: Int = 10
    ): List<ArticleRes>

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/content
    @GET("articles/{article}/content")
    suspend fun loadArticleContent(
        @Path("article") articleId: String
    ): ArticleContentRes

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/messages
    @GET("articles/{article}/messages")
    fun loadComments(
        @Path("article") articleId: String,
        @Query("last") last: Any? = null,
        @Query("limit") limit: Int = 9
    ): Call<List<CommentRes>>

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/messages
    @GET("articles/{article}/messages")
    suspend fun loadComments2(
        @Path("article") articleId: String,
        @Query("offset") offset: Any? = null,
        @Query("limit") limit: Int = 9
    ): List<CommentRes>

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/messages
    @POST("articles/{article}/messages")
    suspend fun sendMessage(
        @Path("article") articleId: String,
        @Body message: MessageReq,
        @Header("Authorization") token: String
    ): MessageRes

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/counts
    @GET("articles/{article}/counts")
    suspend fun loadArticleCounts(
        @Path("article") articleId: String
    ): ArticleCountsRes

    // https://skill-articles.skill-branch.ru/api/v1/auth/login
    @POST("auth/login")
    suspend fun login(@Body loginReq: LoginReq): AuthRes

    // https://skill-articles.skill-branch.ru/api/v1/auth/register
    @POST("auth/register")
    suspend fun register(@Body registerReq: RegistrationReq): AuthRes

    // Look at video (lecture 12, time code 01:02:01)
    // https://skill-articles.skill-branch.ru/api/v1/auth/refresh
    @POST("auth/refresh")
    fun refreshAccessToken(@Body refreshRec: RefreshReq): Call<RefreshRes>

    // Метод имеется в приложенном к уроку коде (lecture 11)
    @POST("auth/login")
    fun loginCall(@Body loginReq: LoginReq): Call<AuthRes>

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/decrementLikes
    @POST("articles/{article}/decrementLikes")
    suspend fun decrementLike(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    ): LikeRes

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/incrementLikes
    @POST("articles/{article}/incrementLikes")
    suspend fun incrementLike(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    ): LikeRes

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/addBookmark
    @POST("articles/{article}/addBookmark")
    suspend fun addBookmark(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    )

    // https://skill-articles.skill-branch.ru/api/v1/articles/{articleId}/removeBookmark
    @POST("articles/{article}/removeBookmark")
    suspend fun removeBookmark(
        @Path("article") articleId: String,
        @Header("Authorization") token: String
    )

    // https://skill-articles.skill-branch.ru/api/v1/profile/avatar/upload
    @Multipart
    @POST("profile/avatar/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part?,
        @Header("Authorization") token: String
    ): UploadRes // возвращает серверую ссылку (URL) на выгруженный файл

    // https://skill-articles.skill-branch.ru/api/v1/profile/avatar/remove
    @PUT("profile/avatar/remove")
    suspend fun remove(
        @Header("Authorization") token: String
    )

    // https://skill-articles.skill-branch.ru/api/v1/profile
    @PUT("profile")
    suspend fun edit(
        @Body editProfileReq: EditProfileReq,
        @Header("Authorization") token: String
    ) // : EditProfileRes ????

}