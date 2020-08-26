package ru.skillbranch.skillarticles.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel

/**
 * A simple [Fragment] subclass
 */
class ChoseCategoryDialog : DialogFragment() {
    private val viewModel: ArticlesViewModel by activityViewModels()
    private val selectedCats = mutableSetOf<String>() // e.g. ["1","5","7"]
    private val args: ChoseCategoryDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // todo save checked state and implement custom items
        //====================== ВАРИАНТ 1 ===================================
//        val categories = args.categories.toList().map {
//            "${it.title} (${it.articlesCount})"
//        }.toTypedArray()
        //====================================================================
        // Для передачи в метод setMultiChoiceItems (вариант 1) или в
        // конструктор адаптера (вариант 2)
        selectedCats.clear()
        selectedCats.addAll(
            // Если диалог создается заново в результате поворота экрана
            savedInstanceState?.getStringArray("checked")
            // Если диалог создается после нажатия юзером кнопки
            // Filter в экшнбаре
                ?: args.selectedCategories
        )
        val checkedArr = BooleanArray(args.categories.size)
        checkedArr.forEachIndexed { index, _ ->
            if (selectedCats.contains(args.categories[index].categoryId)) {
                checkedArr[index] = true
            }
        }
        val adb = AlertDialog.Builder(requireContext())
            .setTitle("Choose category")
            .setPositiveButton("Apply") { _, _ ->
                viewModel.applyCategories(selectedCats.toList())
            }
            .setNegativeButton("Reset") { _, _ ->
                viewModel.applyCategories(emptyList())
            }
        //====================== ВАРИАНТ 1 ===================================
        // which: the position of the item in the list that was clicked
        // isChecked: true if the click checked the item, else false
//            .setMultiChoiceItems(categories, checked) { _, which, isChecked ->
//                if (isChecked)
//                    selectedCats.add(args.categories[which].categoryId)
//                else selectedCats.remove(args.categories[which].categoryId)
//            }
        //====================== ВАРИАНТ 2 ===================================
        val adapter = CategoryAdapter(
            requireContext(),
            args.categories,
            checkedArr
        ) { view ->
            val box = view.findViewById<CheckBox>(R.id.ch_select)
            val position = view.tag as Int
            if (box.isChecked) {
                box.isChecked = false
                selectedCats.remove(args.categories[position].categoryId)
            } else {
                box.isChecked = true
                selectedCats.add(args.categories[position].categoryId)
            }
        }
        adb.setAdapter(adapter, null)
        //====================================================================
        return adb.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArray("checked", selectedCats.toTypedArray())
        super.onSaveInstanceState(outState)
    }
}

class CategoryAdapter(
    private val cxt: Context,
    private val categories: Array<CategoryData>,
    private var checkedArr: BooleanArray,
    private val listener: (view: View) -> Unit
) : ArrayAdapter<CategoryData>(cxt, 0, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val data = getItem(position)
        val view: View
        if (convertView != null) view = convertView
        else {
            view = LayoutInflater.from(cxt)
                .inflate(R.layout.item_dialog, parent, false)
            view.setOnClickListener(listener)
        }
        view.tag = position

        val box = view.findViewById<CheckBox>(R.id.ch_select)
        box.isClickable = false
        box.isChecked = checkedArr[position]
        val iconView = view.findViewById<ImageView>(R.id.iv_icon)
        Glide.with(cxt)
            .load(data?.icon)
            .apply(RequestOptions.circleCropTransform())
            .override(cxt.dpToIntPx(40))
            .into(iconView)
        view.findViewById<TextView>(R.id.tv_category).text = data?.title
        val count = "(${data?.articlesCount})"
        view.findViewById<TextView>(R.id.tv_count).text = count
        return view
    }

    override fun getCount() = categories.size
}


