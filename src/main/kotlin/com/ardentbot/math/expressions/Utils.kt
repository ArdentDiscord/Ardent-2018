package com.ardentbot.math.expressions

import java.math.BigDecimal
import java.math.BigInteger

enum class Operator(val value: String, val importance: Int) {
    ADD("+",0), SUBTRACT("-",0), MULTIPLY("*",1), DIVIDE("/",1),
    POW("^",2), MOD("%",1);

    override fun toString() = value
}

fun Char.isOperator() = Operator.values().map { it.value }.contains(this.toString())

data class MathCalculation(val result:BigDecimal, val precision: Int) {
    override fun toString() = result.stripTrailingZeros().toString()
}