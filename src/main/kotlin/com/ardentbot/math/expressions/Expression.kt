package com.ardentbot.math.expressions

import com.ardentbot.math.MathException
import com.ardentbot.math.MathParser

class Expression(val input: String, val parser: MathParser = MathParser(100)) {
    private val variables = mutableMapOf<String, String>()

    fun getVariable(name: String): Expression? = variables[name]?.let { Expression(evaluateVariables(it), parser) }
    fun set(variable: String, value: Any) = variables.put(variable, value.toString())
    fun remove(variable: String) = variables.remove(variable)

    private fun evaluateVariables(expression: String): String {
        var insertedExpression = expression
        variables.toList().sortedByDescending { (name, _) -> name.length }
                .forEach { (name, value) -> insertedExpression = insertedExpression.replace(name, "($value)") }
        return if (insertedExpression != expression) evaluateVariables(insertedExpression)
        else expression
    }

    fun evaluate(): MathCalculation {
        try {
            val expression = evaluateVariables(input)
            try {
                val result = parser.evaluateExpression(expression)
                return MathCalculation(result, parser.precision)
            } catch (e: Throwable) {
                throw e
            }
        } catch (e: StackOverflowError) {
            e.printStackTrace()
            throw MathException("Stack overflow! Could $variables in $input be oscillating or nonexistant or is there a non-declared variable?")
        }
    }

    override fun toString() = evaluateVariables(input)
}