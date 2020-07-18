package ru.skillbranch.skillarticles.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.utils.DialogAdapter
import ru.skillbranch.skillarticles.viewmodels.articles.ArticlesViewModel

/**
 * A simple [Fragment] subclass
 */
class ChooseCategoryDialog : DialogFragment() {
    private val viewModel: ArticlesViewModel by activityViewModels()
    private val selectedCats = mutableListOf<String>() // e.g. ["1","5","7"]
    private val args: ChooseCategoryDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // todo save checked state and implement custom items
        //====================== ВАРИАНТ 1 ===================================
//        val categories = args.categories.toList().map {
//            "${it.title} (${it.articlesCount})"
//        }.toTypedArray()
        //====================================================================
        // Для передачи в метод setMultiChoiceItems (вариант 1) или в
        // конструктор адаптера (вариант 2)
        val checked = BooleanArray(args.categories.size)
        checked.forEachIndexed { index, _ ->
            if (args.selectedCategories.contains(args.categories[index].categoryId)) {
                checked[index] = true
                selectedCats.add(args.categories[index].categoryId)
            }
        }

        val adb = AlertDialog.Builder(requireContext())
            .setTitle("Choose category")
            .setPositiveButton("Apply") { _, _ ->
                viewModel.applyCategories(selectedCats)
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
        val adapter = DialogAdapter(
            requireContext(),
            args.categories,
            checked
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
}