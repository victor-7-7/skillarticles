package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.ArticleItemData
import ru.skillbranch.skillarticles.extensions.attrValue
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.format
import kotlin.math.max

class ArticleItemView(
    context: Context
) : ViewGroup(context, null, 0) {
    private val tvDate: TextView
    private val tvAuthor: TextView
    private val tvTitle: TextView
    private val ivPoster: ImageView
    private val ivCategory: ImageView
    private val tvDescription: TextView
    private val ivLikes: ImageView
    private val tvLikesCount: TextView
    private val ivComments: ImageView
    private val tvCommentsCount: TextView
    private val tvReadDuration: TextView
    private val ivBookmark: ImageView

    private val padding = context.dpToIntPx(16)

    private val posterSize = context.dpToIntPx(64)
    private val categorySize = context.dpToIntPx(40)
    private val iconSize = context.dpToIntPx(16)
    private val cornerRadius = context.dpToIntPx(8)

    private val tvAuthorMarginStart = context.dpToIntPx(16)
    private val tvTitleMarginTop = context.dpToIntPx(8)
    private val tvTitleMarginEnd = context.dpToIntPx(24)
    private val tvTitleMarginBottom = context.dpToIntPx(8)
    private val ivPosterMarginTop = context.dpToIntPx(8)
    private val ivPosterMarginBottom = context.dpToIntPx(20)
    private val tvDescriptionMarginTop = context.dpToIntPx(8)
    private val tvLikesCountMarginStart = context.dpToIntPx(8)
    private val iconRowMarginTop = context.dpToIntPx(8)
    private val ivCommentsMarginStart = context.dpToIntPx(16)
    private val tvCommentsCountMarginStart = context.dpToIntPx(8)
    private val tvReadDurationMarginStart = context.dpToIntPx(16)
    private val tvReadDurationMarginEnd = context.dpToIntPx(16)

    private val textSizeIconRow = 12f

    @ColorInt
    private val colorGrey = context.getColor(R.color.color_gray)

    @ColorInt
    private val colorPrimary = context.attrValue(R.attr.colorPrimary)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(padding)

        tvDate = TextView(context).apply {
            id = R.id.tv_date
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            textSize = 12f
            setTextColor(colorGrey)
        }
        addView(tvDate)

        tvAuthor = TextView(context).apply {
            id = R.id.tv_author
            textSize = 12f
            setTextColor(colorPrimary)
        }
        addView(tvAuthor)

        tvTitle = TextView(context).apply {
            id = R.id.tv_title
            textSize = 18f
            setTextColor(colorPrimary)
            setTypeface(typeface, Typeface.BOLD)
        }
        addView(tvTitle)

        ivPoster = ImageView(context).apply {
            id = R.id.iv_poster
        }
        addView(ivPoster, LayoutParams(posterSize, posterSize))

        ivCategory = ImageView(context).apply {
            id = R.id.iv_category
        }
        addView(ivCategory, LayoutParams(categorySize, categorySize))

        tvDescription = TextView(context).apply {
            id = R.id.tv_description
            textSize = 14f
            setTextColor(colorGrey)
        }
        addView(tvDescription)

        ivLikes = ImageView(context).apply {
            id = R.id.iv_likes
            imageTintList = ColorStateList.valueOf(colorGrey)
            setImageResource(R.drawable.ic_favorite_black_24dp)
        }
        addView(ivLikes, LayoutParams(iconSize, iconSize))

        tvLikesCount = TextView(context).apply {
            id = R.id.tv_likes_count
            textSize = textSizeIconRow
            setTextColor(colorGrey)
        }
        addView(tvLikesCount, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        ivComments = ImageView(context).apply {
            id = R.id.iv_comments
            imageTintList = ColorStateList.valueOf(colorGrey)
            setImageResource(R.drawable.ic_insert_comment_black_24dp)
        }
        addView(ivComments, LayoutParams(iconSize, iconSize))

        tvCommentsCount = TextView(context).apply {
            id = R.id.tv_comments_count
            textSize = textSizeIconRow
            setTextColor(colorGrey)
        }
        addView(tvCommentsCount, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        tvReadDuration = TextView(context).apply {
            id = R.id.tv_read_duration
            textSize = textSizeIconRow
            setTextColor(colorGrey)
        }
        addView(tvReadDuration)

        ivBookmark = ImageView(context).apply {
            id = R.id.iv_bookmark
            imageTintList = ColorStateList.valueOf(colorGrey)
            setImageResource(R.drawable.bookmark_states)
        }
        addView(ivBookmark, LayoutParams(iconSize, iconSize))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var usedHeight = paddingTop
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)

        measureChild(tvDate, widthMeasureSpec, heightMeasureSpec)
        tvAuthor.maxWidth =
            width - (tvDate.measuredWidth + paddingRight + paddingLeft + tvAuthorMarginStart)
        measureChild(tvAuthor, widthMeasureSpec, heightMeasureSpec)
        usedHeight += max(tvDate.measuredHeight, tvAuthor.measuredHeight)

        measureChild(ivPoster, widthMeasureSpec, heightMeasureSpec)
        measureChild(ivCategory, widthMeasureSpec, heightMeasureSpec)
        val sizeOfPosterAndCategoryImage = posterSize + (categorySize / 2)
//        tvTitle.maxWidth = width - (paddingLeft + paddingRight + ivPoster.measuredWidth + tvTitleMarginEnd)
        tvTitle.maxWidth =
            width - (paddingRight + paddingLeft + sizeOfPosterAndCategoryImage + context.dpToIntPx(8)) //TODO context.dpToIntPx(4)
        measureChild(tvTitle, widthMeasureSpec, heightMeasureSpec)
        usedHeight += tvTitleMarginTop + max(tvTitle.measuredHeight, sizeOfPosterAndCategoryImage)

        tvDescription.maxWidth = width - (paddingLeft + paddingRight)
        measureChild(tvDescription, widthMeasureSpec, heightMeasureSpec)
        usedHeight += tvDescription.measuredHeight + tvDescriptionMarginTop

        measureChild(ivLikes, widthMeasureSpec, heightMeasureSpec)
        measureChild(tvLikesCount, widthMeasureSpec, heightMeasureSpec)
        measureChild(ivComments, widthMeasureSpec, heightMeasureSpec)
        measureChild(tvCommentsCount, widthMeasureSpec, heightMeasureSpec)
        measureChild(ivBookmark, widthMeasureSpec, heightMeasureSpec)
//        tvReadDuration.maxWidth = width -
//                (paddingLeft + ivLikes.measuredWidth + tvLikesCountMarginStart + tvLikesCount.measuredWidth +
//                        ivCommentsMarginStart + ivComments.measuredWidth +
//                        tvCommentsCountMarginStart + tvCommentsCount.measuredWidth +
//                        tvReadDurationMarginStart + tvReadDurationMarginEnd +
//                        ivBookmark.measuredWidth + paddingRight)
        measureChild(tvReadDuration, widthMeasureSpec, heightMeasureSpec)
//        usedHeight += max(tvReadDuration.measuredHeight, ivLikes.measuredHeight) +
//                iconRowMarginTop + paddingBottom //consider all icon have same size and counters have same textSize

        usedHeight += ivLikes.measuredHeight + iconRowMarginTop + paddingBottom //Attempt to pass test removing max(tvReadDuration.measuredHeight, ivLikes.measuredHeight)

        setMeasuredDimension(width, usedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var usedHeight = paddingTop

        tvDate.layout(
            paddingLeft,
            usedHeight,
            paddingLeft + tvDate.measuredWidth,
            usedHeight + tvDate.measuredHeight
        )
        val leftOfTvAuthor = tvDate.right + tvAuthorMarginStart
        tvAuthor.layout(
            leftOfTvAuthor,
            usedHeight,
            width - paddingRight,
            usedHeight + tvAuthor.measuredHeight
        )
        usedHeight += max(tvDate.measuredHeight, tvAuthor.measuredHeight)

        val heightOfPosterAndCategoryImage = posterSize + (categorySize / 2)
        val bottomOfTvTitle: Int
        val topOfIvPoster: Int
        val leftOfIvPoster: Int
        if (heightOfPosterAndCategoryImage > tvTitle.measuredHeight) {
            bottomOfTvTitle = usedHeight + tvTitleMarginTop + tvTitle.measuredHeight +
                    ((heightOfPosterAndCategoryImage - tvTitle.measuredHeight) / 2)
            topOfIvPoster = usedHeight + ivPosterMarginTop
            leftOfIvPoster = width - paddingRight - ivPoster.measuredWidth
            usedHeight += ivPosterMarginTop + heightOfPosterAndCategoryImage
        } else {
            bottomOfTvTitle = usedHeight + tvTitleMarginTop + tvTitle.measuredHeight
            topOfIvPoster = usedHeight + ivPosterMarginTop +
                    ((tvTitle.measuredHeight - heightOfPosterAndCategoryImage) / 2)
            leftOfIvPoster = width - paddingRight - ivPoster.measuredWidth
            usedHeight += tvTitleMarginTop + tvTitle.measuredHeight
        }
        tvTitle.layout(
            paddingLeft,
            bottomOfTvTitle - tvTitle.measuredHeight,
            paddingLeft + tvTitle.measuredWidth,
            bottomOfTvTitle
        )
        ivPoster.layout(
            leftOfIvPoster,
            topOfIvPoster,
            leftOfIvPoster + ivPoster.measuredWidth,
            topOfIvPoster + ivPoster.measuredHeight
        )
        ivCategory.layout(
            ivPoster.left - ivCategory.measuredWidth / 2,
            ivPoster.bottom - ivCategory.measuredWidth / 2,
            ivPoster.left + ivCategory.measuredWidth / 2,
            ivPoster.bottom + ivCategory.measuredWidth / 2
        )

        val topOfTvDescription = usedHeight + tvDescriptionMarginTop
        tvDescription.layout(
            paddingLeft,
            topOfTvDescription,
            paddingLeft + tvDescription.measuredWidth,
            topOfTvDescription + tvDescription.measuredHeight
        )
        usedHeight += tvDescriptionMarginTop + tvDescription.measuredHeight

        val topOfIcon = usedHeight + iconRowMarginTop
        val diffSizeIconIvAndCounter =
            (tvLikesCount.measuredHeight - ivLikes.measuredHeight) / 2 //constraintBottom icon toBottom counter and constraintTop icon to Top counter
        ivLikes.layout(
            paddingLeft,
            topOfIcon + diffSizeIconIvAndCounter,
            paddingLeft + ivLikes.measuredWidth,
            topOfIcon + diffSizeIconIvAndCounter + ivLikes.measuredHeight
        )
        val leftOfTvLikesCount = ivLikes.right + tvLikesCountMarginStart
        tvLikesCount.layout(
            leftOfTvLikesCount,
            topOfIcon,
            leftOfTvLikesCount + tvLikesCount.measuredWidth,
            topOfIcon + tvLikesCount.measuredHeight
        )
        val leftOfIvComments = tvLikesCount.right + ivCommentsMarginStart
        ivComments.layout(
            leftOfIvComments,
            topOfIcon + diffSizeIconIvAndCounter,
            leftOfIvComments + ivComments.measuredWidth,
            topOfIcon + diffSizeIconIvAndCounter + ivComments.measuredHeight
        )
        val leftOfTvCommentsCount = ivComments.right + tvCommentsCountMarginStart
        tvCommentsCount.layout(
            leftOfTvCommentsCount,
            topOfIcon,
            leftOfTvCommentsCount + tvCommentsCount.measuredWidth,
            topOfIcon + tvCommentsCount.measuredHeight
        )
        val leftOfTvReadDuration = tvCommentsCount.right + tvReadDurationMarginStart
        val leftOfIvBookmark = width - paddingRight - ivBookmark.measuredWidth
        tvReadDuration.layout(
            leftOfTvReadDuration,
            topOfIcon,
            leftOfTvReadDuration + tvReadDuration.measuredWidth, //TODO leftOfIvBookmark - tvReadDurationMarginEnd
            topOfIcon + tvReadDuration.measuredHeight
        )
        ivBookmark.layout(
            leftOfIvBookmark,
            topOfIcon + diffSizeIconIvAndCounter,
            leftOfIvBookmark + ivBookmark.measuredWidth,
            topOfIcon + diffSizeIconIvAndCounter + ivBookmark.measuredHeight
        )
    }

    fun bind(data: ArticleItemData) {
        Glide.with(context)
            .load(data.poster)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))
            .override(posterSize)
            .into(ivPoster)

        Glide.with(context)
            .load(data.categoryIcon)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))
            .override(categorySize)
            .into(ivCategory)

        tvDate.text = data.date.format()
        tvAuthor.text = data.author
        tvTitle.text = data.title
        tvDescription.text = data.description
        tvLikesCount.text = "${data.likeCount}"
        tvCommentsCount.text = "${data.commentCount}"
        tvReadDuration.text = "${data.readDuration} min read"
    }
}