package ru.skillbranch.skillarticles.di.modules

import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.repositories.ArticlesRepository
import ru.skillbranch.skillarticles.data.repositories.IRepository
import ru.skillbranch.skillarticles.ui.articles.ArticlesFragment
import ru.skillbranch.skillarticles.ui.articles.IArticlesView

@InstallIn(FragmentComponent::class)
@Module
abstract class ArticlesModule {
    // Пример инъекции интерфейса через Hilt
    @Binds
    abstract fun bindArticlesView(fragment: ArticlesFragment): IArticlesView

    companion object {
        // Подсказываем хилту, что фрагмент (привязанный методом
        // bindArticlesView к текущему FragmentComponent) имеет
        // тип ArticlesFragment (lecture 14, t.c. 03:09:00)
        @Provides
        fun provideArticlesFragment(fragment: Fragment) = fragment as ArticlesFragment

        @Provides
        fun provideSimpleCursorAdapter(fragment: Fragment) = SimpleCursorAdapter(
            fragment.context,
            R.layout.item_suggestion,
            null, // cursor
            // FROM: names of cursor columns for bind on view
            arrayOf("tag"),
            // TO: text view id for bind data from cursor
            intArrayOf(android.R.id.text1),
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )
    }

    // Этот метод понадобится на следующем (после даггера) занятии
    @Binds
    abstract fun bindArticlesRepo(repo: ArticlesRepository): IRepository
}