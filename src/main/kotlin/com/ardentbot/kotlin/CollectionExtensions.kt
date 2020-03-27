package com.ardentbot.kotlin

import com.ardentbot.core.ArdentRegister
import com.ardentbot.core.Flag
import com.ardentbot.core.toUser
import java.net.URLEncoder

fun List<String>.toUsersDisplay(register: ArdentRegister): String = joinToString { it.toUser(register)?.display() ?: "unknown" }

fun <T> MutableList<T>.removeIndices(vararg indices: Int): MutableList<T> {
    return if (indices.isEmpty()) this
    else {
        removeAt(indices[0])
        removeIndices(
                *indices.slice(IntRange(1, indices.size - 1)).map { if (it > indices[0]) it - 1 else it }
                        .toIntArray()
        )
    }
}

fun <T> List<T>.without(index: Int): MutableList<T> {
    val mutable = toMutableList()
    mutable.removeAt(index)
    return mutable
}

fun <T> List<T>.without(obj: T): MutableList<T> {
    val mutable = toMutableList()
    mutable.remove(obj)
    return mutable
}

fun <T> List<T>.concat() = joinToString(" ") { it.toString() }

fun <K> MutableList<K>.forEach(consumer: (MutableIterator<K>, current: K) -> Unit) {
    val iterator = iterator()
    while (iterator.hasNext()) consumer.invoke(iterator, iterator.next())
}
