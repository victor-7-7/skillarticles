package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.remote.res.CommentRes
import ru.skillbranch.skillarticles.extensions.*
import kotlin.math.min

class CommentItemView(context: Context) : ViewGroup(context, null, 0) {
    /** Вертикальный паддинг (сверху / снизу) для вьюгруппы */
    private val defaultVSpace = context.dpToIntPx(8)

    /** Горизонтальный паддинг (слева / справа) для вьюгруппы */
    private val defaultHSpace = context.dpToIntPx(16)
    private val avatarSize = context.dpToIntPx(40)
    private val lineSize = context.dpToPx(2)

    /** Размер иконки-стрелочки (справа от tv_answer_to) */
    private val iconSize = context.dpToIntPx(12)

    private val tv_date: TextView
    private val iv_avatar: ImageView
    private val tv_author: TextView
    private val tv_body: TextView
    private val iv_answer_icon: ImageView
    private val tv_answer_to: TextView

    private val grayColor = context.getColor(R.color.color_gray)
    private val primaryColor = context.attrValue(R.attr.colorPrimary)
    private val dividerColor = context.getColor(R.color.color_divider)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dividerColor
        strokeWidth = lineSize
        style = Paint.Style.STROKE
    }

    init {
        // Ставим дефолтные отступы со всех сторон вьюхи
        setPadding(defaultHSpace, defaultVSpace, defaultHSpace, defaultVSpace)

        tv_date = TextView(context).apply {
            setTextColor(grayColor)
            textSize = 12f
            textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        }
        addView(tv_date)

        iv_avatar = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        addView(iv_avatar)

        tv_author = TextView(context).apply {
            setTextColor(primaryColor)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
        }
        addView(tv_author)

        tv_body = TextView(context).apply {
            id = R.id.tv_comment_body
            setTextColor(grayColor)
            textSize = 14f
        }
        addView(tv_body)

        tv_answer_to = TextView(context).apply {
            setTextColor(grayColor)
            textSize = 12f
            isVisible = false
        }
        addView(tv_answer_to)

        iv_answer_icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_reply_black_24dp)
            imageTintList = ColorStateList.valueOf(grayColor)
            isVisible = false
        }
        addView(iv_answer_icon)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = paddingTop
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)

        measureChild(tv_answer_to, widthMeasureSpec, heightMeasureSpec)
        if (tv_answer_to.isVisible) usedHeight += tv_answer_to.measuredHeight

        tv_date.minWidth = avatarSize

        //============== ВАРИАНТ 1 ===================
        /*
        // Дата в полную ширину, имя автора, если не вмещается, переносится
        // на вторую строку
        measureChild(tv_date, widthMeasureSpec, heightMeasureSpec)
        tv_author.width = width - paddingLeft - paddingRight - avatarSize -
                    defaultHSpace - tv_date.measuredWidth
        measureChild(tv_author, widthMeasureSpec, heightMeasureSpec)
        */

        //============== ВАРИАНТ 2 ===================
        // Имя автора в полную ширину, дата, если не вмещается, переносится
        // на вторую строку
        measureChild(tv_author, widthMeasureSpec, heightMeasureSpec)
        tv_date.width = width - paddingLeft - paddingRight - avatarSize -
                defaultHSpace - tv_author.measuredWidth
        measureChild(tv_date, widthMeasureSpec, heightMeasureSpec)

        //============================================
        usedHeight += avatarSize + defaultVSpace
        tv_body.width = width - paddingLeft - paddingRight
        measureChild(tv_body, widthMeasureSpec, heightMeasureSpec)

        usedHeight += tv_body.measuredHeight + defaultVSpace
        setMeasuredDimension(width, usedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var usedHeight = paddingTop
        val left = paddingLeft

        if (tv_answer_to.isVisible) {
            val lb = left + avatarSize + defaultHSpace / 2
            tv_answer_to.layout(
                lb,
                usedHeight,
                lb + tv_answer_to.measuredWidth,
                usedHeight + tv_answer_to.measuredHeight
            )
            // Зазор между верхней (нижней) границей вьюхи tv_answer_to
            // и верхней (нижней) границей иконки iv_answer_icon
            val diff = (tv_answer_to.measuredHeight - iconSize) / 2

            iv_answer_icon.layout(
                tv_answer_to.right + defaultHSpace / 2,
                usedHeight + diff,
                tv_answer_to.right + defaultHSpace / 2 + iconSize,
                usedHeight + iconSize + diff
            )

            usedHeight += tv_answer_to.measuredHeight
        }
        // Зазор между верхней (нижней) границей вьюхи iv_avatar
        // и верхней (нижней) границей вьюхи tv_author
        val diffVAuthor = (avatarSize - tv_author.measuredHeight) / 2
        // Зазор между верхней (нижней) границей вьюхи iv_avatar
        // и верхней (нижней) границей вьюхи tv_date
        val diffVDate = (avatarSize - tv_date.measuredHeight) / 2

        iv_avatar.layout(
            left,
            usedHeight,
            left + avatarSize,
            usedHeight + avatarSize
        )

        tv_author.layout(
            iv_avatar.right + defaultHSpace / 2,
            usedHeight + diffVAuthor,
            iv_avatar.right + defaultHSpace / 2 + tv_author.measuredWidth,
            usedHeight + tv_author.measuredHeight + diffVAuthor
        )

        tv_date.layout(
            tv_author.right + defaultHSpace / 2,
            usedHeight + diffVDate,
            // Вариант, когда дата прижата к имени
//            tv_author.right + defaultHSpace / 2 + tv_date.measuredWidth,
            // Вариант, когда дата прижата к правому краю, поскольку
            // при инициализации задано textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            r - defaultHSpace,
            usedHeight + tv_date.measuredHeight + diffVDate
        )

        usedHeight += avatarSize + defaultVSpace

        tv_body.layout(
            left,
            usedHeight,
            left + tv_body.measuredWidth,
            usedHeight + tv_body.measuredHeight
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val level = min(paddingLeft / defaultHSpace, 5)
        if (level == 1) return
        // Рисуем вертикальные линии
        for (i in 1 until level) {
            canvas.drawLine(
                i.toFloat() * defaultHSpace,
                0f,
                i.toFloat() * defaultHSpace,
                canvas.height.toFloat(),
                linePaint
            )
        }
    }

    /** Параметр pos - для демонстрации механизма пэйджинга */
    fun bind(item: CommentRes?, pos: Int) {
        if (item == null) {
            //TODO show shimmer
            iv_avatar.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_account_circle_black_24dp, null
                )
            )
            tv_author.text = "$pos"
            tv_date.text = "time loading..."
            tv_body.text = "Comment loading..."
        }
        else {
            val level = min(item.slug.split("/").size.dec(), 5)
            // Для нулевого уровня дополнительный отступ слева не нужен
            if (level > 0) setPaddingOptionally(left = level * defaultHSpace)

            if (item.user.avatar.isBlank()) {
                iv_avatar.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.ic_avatar, null
                    )
                )
            }
            else {
                Glide.with(context)
                    .load(item.user.avatar)
                    .apply(RequestOptions.circleCropTransform())
                    .override(avatarSize)
                    .into(iv_avatar)
            }
            val name = "$pos ${item.user.name}"
            tv_author.text = name
            tv_date.text = item.date.humanizeDiff()
            val message = item.body
            tv_body.text = message
            tv_answer_to.text = item.answerTo
            tv_answer_to.isVisible = item.answerTo != null
            iv_answer_icon.isVisible = item.answerTo != null
        }
    }
}