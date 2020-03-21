package com.ardentbot.math.functions

import com.ardentbot.math.MathParser
import java.math.BigDecimal

abstract class MathFunction(val name: String, vararg val pseudonyms: String, val noArgumentsAllowed: Boolean = false) {
    abstract fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal
    open fun evaluate(vararg numbers: BigDecimal, parser: MathParser) = evaluate(numbers[0], parser)
}
