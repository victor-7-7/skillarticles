package ru.skillbranch.skillarticles.di.modules

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import ru.skillbranch.skillarticles.data.repositories.ArticleRepository
import ru.skillbranch.skillarticles.data.repositories.IRepository
import ru.skillbranch.skillarticles.ui.article.ArticleFragment
import ru.skillbranch.skillarticles.ui.article.IArticleView

@InstallIn(FragmentComponent::class)
@Module
abstract class ArticleModule {
    // Пример инъекции интерфейса через Hilt
    @Binds
    abstract fun bindArticleView(fragment: ArticleFragment): IArticleView

    companion object {
        // Подсказываем хилту, что фрагмент (привязанный методом
        // bindArticleView к текущему FragmentComponent) имеет
        // тип ArticleFragment
        @Provides
        fun provideArticleFragment(fragment: Fragment) = fragment as ArticleFragment
    }


    // Этот метод понадобится на следующем (после даггера) занятии
    @Binds
    abstract fun bindArticleRepo(repo: ArticleRepository): IRepository

}