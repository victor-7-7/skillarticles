package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {
    //    private val LINE_SEPARATOR: String
//    init {
//        val separ   = System.getProperty("line.separator")
//        LINE_SEPARATOR = separ ?: "\n"
//        // for unit tests separ == '\r\n', for android/instrumental = '\n'
//    }
    private const val LINE_SEPARATOR = "\n"
    // group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"
    // .+? знак ? означает ленивый поиск (lazy) - удовлетворимся
    // совпадением с минимальным числом символов
    private const val HEADER_GROUP = "(^#{1,6} .+$)"
    private const val QUOTE_GROUP = "(^> .+?$)"
    private const val ITALIC_GROUP =
        "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    private const val BOLD_GROUP =
        "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?[^~]?~{2}(?!~))"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"
    private const val LINK_GROUP = "(\\[[^\\[\\]]*?]\\(.+?\\)|^\\[*?]\\(.*?\\))"
    // (?s) <- embedded flag Pattern.DOTALL для многострочного блока кода
//    private const val BLOCK_CODE_GROUP = "((?s)\\v(?<!`)```[^`\\s].*?[^`\\s]```(?!`)\\v)"
    private const val ORDERED_LIST_GROUP = "(^\\d+?. .+$)"
    private const val BLOCK_CODE_GROUP = "(^```[\\S\\s]+?```)"
//    private const val ORDERED_LIST_GROUP = "(^\\d{1,2}\\.\\s.+?$)"

    // result regex
    private const val MARKDOWN_GROUPS =
        "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP" +
                "|$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP" +
                "|$INLINE_GROUP|$LINK_GROUP|$BLOCK_CODE_GROUP|$ORDERED_LIST_GROUP"

    private val elementsPattern by lazy {
        Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE)
    }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    /**
     * clear markdown text to string without markdown characters
     */
    fun clear(string: String?): String? {
        string ?: return null
        // Очищаем размеченный текст от разметочных символов
        return buildPlainText(
            parse(string).elements,
            StringBuilder("")
        ).toString()
    }

    private fun buildPlainText(elems: List<Element>, sb: StringBuilder)
            : StringBuilder {
        elems.forEach {
            if (it.elements.isEmpty()) sb.append(it.text)
            else buildPlainText(it.elements, sb)
        }
        return sb
    }

    /**
     * find markdown elements in markdown text
     */
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@ while (
            matcher.find(lastStartIndex)
        ) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()
            // if something is found then everything before is TEXT
            if (lastStartIndex < startIndex) {
                parents.add(
                    Element.Text(
                        string.subSequence(
                            lastStartIndex, startIndex
                        )
                    )
                )
            }
            // found text
            var text: CharSequence
            // groups range for iterate by groups
            val groups = 1..11
            var group = -1
            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }
            when (group) {
                // not found -> break
                -1 -> break@loop
                // unordered list
                1 -> {
                    // text without "*. "
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)
                    // next find start from position endIndex (last regex character)
                    lastStartIndex = endIndex
                }
                // header
                2 -> {
                    val reg = "^#{1,6}".toRegex()
                        .find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length
                    // text without "{#} "
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)
                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // quote
                3 -> {
                    // text without "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.Quote(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // italic
                4 -> {
                    // text without "*{}*" and "_{}_" at bounds
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.Italic(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // bold
                5 -> {
                    // text without "**{}**" and "__{}__" at bounds
                    text = string.subSequence(
                        startIndex.plus(2), endIndex.minus(2)
                    )
                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.Bold(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // strike
                6 -> {
                    // text without "~~{}~~"  at bounds
                    text = string.subSequence(
                        startIndex.plus(2), endIndex.minus(2)
                    )
                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.Strike(text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // rule
                7 -> {
                    // text without "***/---/___"  insert space character
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // inline code
                8 -> {
                    // text without "`{}`"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val element = Element.InlineCode(text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // link
                9 -> {
                    // full text for regex
                    text = string.subSequence(startIndex, endIndex)
                    val (title: String, link: String) =
                        "\\[(.*)]\\((.*)\\)".toRegex().find(text)!!.destructured
                    val element = Element.Link(link, title)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                // block code
                10 -> {
                    // text without "```{}```"
                    text = string.subSequence(
                        startIndex.plus(3),
                        endIndex.minus(3)
                    )
                    if (text.contains(LINE_SEPARATOR)) {
                        for ((index, line) in text.lines().withIndex()) {
                            val element = when (index) {
                                0 -> Element.BlockCode(
                                    Element.BlockCode.Type.START,
                                    line + LINE_SEPARATOR
                                )
                                text.lines().lastIndex ->
                                    Element.BlockCode(Element.BlockCode.Type.END, line)
                                else -> Element.BlockCode(
                                    Element.BlockCode.Type.MIDDLE,
                                    line + LINE_SEPARATOR
                                )
                            }
                            parents.add(element)
                        }
                    } else parents.add(
                        Element.BlockCode(Element.BlockCode.Type.SINGLE, text)
                    )
                    lastStartIndex = endIndex
                }
                // ordered list
                11 -> {
                    val reg = "^\\d+?.".toRegex()
                        .find(string.subSequence(startIndex, endIndex))
                    val num = reg!!.value
                    // increment for excluding space character after period
                    text = string.subSequence(
                        startIndex.plus(num.length.inc()), endIndex
                    )
                    // find inner elements
                    val subs = findElements(text)
                    val element = Element.OrderedListItem(num, text, subs)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
            }
        }
        if (lastStartIndex < string.length) {
            val text = string.subSequence(lastStartIndex, string.length)
            parents.add(Element.Text(text))
        }
        return parents
    }
}

data class MarkdownText(val elements: List<Element>)

sealed class Element {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Rule(
        override val text: CharSequence = " ", // for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        val type: Type = Type.MIDDLE,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element() {
        enum class Type { START, END, MIDDLE, SINGLE }
    }
}

