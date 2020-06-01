package ru.skillbranch.skillarticles.extensions

/** Ресивер - список вхождений поисковой строки во всем тексте. Каждое
 * вхождение это пара: индекс начального и конечного символа вхождения.
 * Аргумент intervals - это список интервалов, занимаемых элементами
 * MarkdownElement в тексте.
 * Функция разбивает общий список поисковых вхождений на подсписки
 * вхождений по границам элементов MarkdownElement */
fun List<Pair<Int, Int>>.groupByBounds(intervals: List<Pair<Int, Int>>)
        : List<MutableList<Pair<Int, Int>>> {
    val list = mutableListOf<MutableList<Pair<Int, Int>>>()

/*    var remainder: Pair<Int, Int>? = null
    intervals.forEach { interval ->
        val sublist = mutableListOf<Pair<Int, Int>>()
        if (remainder != null) {
            sublist.add(Pair(remainder!!.first, remainder!!.second))
            remainder = null
        }
        sublist.addAll(filter { pair ->
            pair.first >= interval.first && pair.second < interval.second })
        val crossPair = filter { pair ->
            pair.first < interval.second && pair.second >= interval.second }
        if (crossPair.isNotEmpty()) {
            sublist.add(Pair(crossPair.first().first, interval.second))
            remainder = Pair(interval.second, crossPair.first().second)
        }
        list.add(sublist)
    }*/

    intervals.forEach { interval ->
        val sublist = mutableListOf<Pair<Int, Int>>()
        sublist.addAll(filter { pair ->
            pair.first >= interval.first && pair.second < interval.second
        })
        list.add(sublist)
    }
    return list
}