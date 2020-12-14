package ru.skillbranch.skillarticles.ui.base

import android.os.Bundle
import android.view.*
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_root.*
import ru.skillbranch.skillarticles.ui.RootActivity
import ru.skillbranch.skillarticles.viewmodels.base.BaseViewModel
import ru.skillbranch.skillarticles.viewmodels.base.IViewModelState
import ru.skillbranch.skillarticles.viewmodels.base.Loading

abstract class BaseFragment<T : BaseViewModel<out IViewModelState>> : Fragment() {
    // mock root FOR TESTING
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var _mockRoot: RootActivity? = null

    val root: RootActivity
        get() = _mockRoot ?: activity as RootActivity

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    abstract val viewModel: T

    open val binding: Binding? = null
    protected abstract val layout: Int

    // Лямбда вида SomeClass.()->Unit означает, что ресивером лямбды
    // является экземпляр SomeClass
    open val prepareToolbar: (ToolbarBuilder.() -> Unit)? = null
    open val prepareBottombar: (BottombarBuilder.() -> Unit)? = null

    val toolbar: MaterialToolbar
        get() = root.toolbar

    /** Set listeners, tune views */
    abstract fun setupViews()

    // This will be called between onCreate() and onViewCreated()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layout, container, false)

    // Called immediately after onCreateView() has returned, but before
    // any saved state has been restored in to the view. The fragment's
    // view hierarchy is not however attached to its parent at this point
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //restore state
        viewModel.restoreState()
        binding?.restoreUi(savedInstanceState)

        /** Добавляем наблюдателя за стейтом вью модели данного фрагмента.
         * Если у фрагмента свойство binding не null, то при каждом
         * изменении стейта будет срабатывать метод bind объекта binding */
        viewModel.observeState(viewLifecycleOwner) {
            binding?.bind(it)
        }
        //bind default values if viewmodel not loaded data
        binding?.onFinishFragmentInflate()

        viewModel.observeNotification(viewLifecycleOwner) { root.renderNotification(it) }
        viewModel.observeNavigation(viewLifecycleOwner) { root.viewModel.navigate(it) }
        viewModel.observeLoading(viewLifecycleOwner) { renderLoading(it) }
    }

    // This is called after onViewCreated() and before onStart(). Called
    // when all saved state has been restored into the view hierarchy
    // of the fragment. This can be used to do initialization based on
    // saved state that you are letting the view hierarchy track itself,
    // such as whether check box widgets are currently checked and etc.
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        //prepare toolbar
        root.toolbarBuilder
            .prepare(prepareToolbar)
            .build(root)
        //prepare bottombar
        root.bottombarBuilder
            .prepare(prepareBottombar)
            .build(root)

        setupViews()
        /** Если у восстановленного фрагмента свойство binding
         * не null, то делаем реинициализацию каждого RenderProp
         * объекта, принадлежащего binding */
        binding?.rebind()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState()
        binding?.saveUi(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (root.toolbarBuilder.items.isNotEmpty()) {
            for ((index, menuHolder) in root.toolbarBuilder.items.withIndex()) {
                val item = menu.add(0, menuHolder.menuId, index, menuHolder.title)
                item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                    .setIcon(menuHolder.icon)
                    .setOnMenuItemClickListener {
                        menuHolder.clickListener?.invoke(it)?.let { true } ?: false
                    }

                if (menuHolder.actionViewLayout != null)
                    item.setActionView(menuHolder.actionViewLayout)
            }
        } else menu.clear()
        super.onPrepareOptionsMenu(menu)
    }

    // open для того, чтобы мы могли переопределить эту функцию
    // в любом другом дочернем фрагменте
    open fun renderLoading(loadingState: Loading) {
        root.renderLoading(loadingState)
    }
}