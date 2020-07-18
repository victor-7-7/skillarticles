package ru.skillbranch.skillarticles.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.CategoryData
import ru.skillbranch.skillarticles.extensions.dpToIntPx

class DialogAdapter(
    private val cxt: Context,
    val categories: Array<CategoryData>,
    private var checked: BooleanArray,
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
        box.isChecked = checked[position]
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