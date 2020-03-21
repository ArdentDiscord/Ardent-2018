package com.ardentbot.math

import ch.obermuhlner.math.big.BigDecimalMath
import com.ardentbot.math.expressions.Expression
import com.ardentbot.math.expressions.Operator
import com.ardentbot.math.expressions.isOperator
import com.ardentbot.math.functions.MathFunction
import com.ardentbot.math.functions.getDefaultFunctions
import java.math.BigDecimal
import java.math.MathContext
import java.util.*

class MathParser(val precision: Int) {
    val mathContext = MathContext(precision)
    private val constants = mutableMapOf<String, (MathContext) -> BigDecimal>()
    private val functions = getDefaultFunctions().toMutableList()

    init {
        constants.putAll(getConstants())
    }

    fun evaluateExpression(input: String): BigDecimal = evaluateBase(input)

    private fun evaluateBase(pretransformed: String): BigDecimal {
        val expression = pretransformed.replace(" ", "")
        if (expression.toConstant() != null) return expression.toConstant()!!
        else if (expression.endsWith("!") && expression.removeSuffix("!").toBigDecimalOrNull() != null) {
            return BigDecimalMath.factorial(expression.removeSuffix("!").toBigDecimal().toInt())
        }

        if (!expression.contains("(")) {
            var tokenized = tokenize(expression)

            if (tokenized.size == 1) return tokenized[0].toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("${tokenized[0]} is not a valid input")

            tokenized = tokenized.evaluate()

            return tokenized[0].toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("${tokenized[0]} is not a valid number!")
        } else {
            val leftParenthesis = expression.indexOf('(')
            val rightParenthesis = expression.findMatchingRightParenthesis()
            if (rightParenthesis == -1 || rightParenthesis <= leftParenthesis) throw Exception("No closing parenthesis")

            // can be an omission of the optional multiplication symbol
            if (leftParenthesis > 0 && expression[leftParenthesis - 1].isDigit()) {
                return evaluateBase(expression.substring(0, leftParenthesis) + "*" + expression.substring(leftParenthesis))
            }

            // ..or can be a function invocation
            if (leftParenthesis > 0 && expression[leftParenthesis - 2].isLetter()) {
                val function = functions.sortedByDescending { it.name.length }.find {
                    expression.indexOf(it.name) != -1 && (expression.indexOf(it.name) + it.name.length == leftParenthesis
                            || (it.pseudonyms.any { pseudo -> expression.indexOf(pseudo) + pseudo.length == leftParenthesis }))
                }
                        ?: throw IllegalArgumentException("No valid function found with the letter-based input in $expression")

                val functionNameStart = expression.indexOf(function.name).let {
                    if (it == -1) expression.indexOf(function.pseudonyms.filter { pseudo -> expression.indexOf(pseudo) != -1 }[0])
                    else it
                }

                val functionArgumentStart = leftParenthesis + 1

                if (functionArgumentStart == rightParenthesis) {
                    if (!function.noArgumentsAllowed) throw MathException("Specified function ${function.name} had no arguments")
                    return evaluateBase(expression.substring(0, functionNameStart) + function.evaluate(BigDecimal.valueOf(-1), this))
                }

                val arguments = expression.substring(functionArgumentStart, rightParenthesis).getArguments()
                        .map { evaluateBase(it) }
                val value = if (arguments.size == 1) function.evaluate(arguments[0], this)
                else function.evaluate(*arguments.toTypedArray(), parser = this)

                return evaluateBase(
                        expression.substring(0, functionNameStart) +
                                value +
                                expression.substring(rightParenthesis + 1)
                )

            }

            val value = evaluateBase(expression.substring(leftParenthesis + 1, rightParenthesis))
            return evaluateBase(expression.substring(0, leftParenthesis) + value.toString() + (if (rightParenthesis == expression.lastIndex) "" else expression.substring(rightParenthesis + 1)))
        }
    }

    private fun List<String>.evaluate(): MutableList<String> {
        var tokens = this
        Operator.values().filter { tokens.contains(it.toString()) }.sortedByDescending { it.importance }
                .forEach { tokens = tokens.findAndEvaluate(it) }
        return tokens.toMutableList()
    }

    private fun List<String>.findAndEvaluate(operator: Operator): MutableList<String> {
        var tokens = this
        var index = tokens.indexOf(operator.toString())
        while (index != -1) {
            val first = tokens[index - 1].asNumber()
            val second = tokens[index + 1].asNumber()
            val result = when (operator) {
                Operator.ADD -> first.add(second, mathContext)
                Operator.SUBTRACT -> first.subtract(second, mathContext)
                Operator.MULTIPLY -> first.multiply(second, mathContext)
                Operator.DIVIDE -> first.divide(second, mathContext)
                Operator.POW -> BigDecimalMath.pow(first, second, mathContext)
                Operator.MOD -> first.remainder(second, mathContext)
            }

            tokens = appendResult(tokens, index, result)
            index = tokens.indexOf(operator.toString())
        }
        return tokens.toMutableList()
    }

    private fun appendResult(tokenized: List<String>, tokenIndex: Int, value: BigDecimal): MutableList<String> {
        return (tokenized.subList(0, tokenIndex - 1) + value.toString() + tokenized.subList(tokenIndex + 2, tokenized.size)).toMutableList()
    }

    fun String.asNumber() = evaluateBase(this)


    fun String.toConstant(): BigDecimal? = constants[this.toLowerCase()]?.invoke(mathContext)

    private fun String.findMatchingRightParenthesis(): Int {
        if (indexOf('(') == -1 || indexOf(')') == -1) return -1
        var leftCount = 0
        var rightCount = 0
        forEachIndexed { i, char ->
            if (char == '(') leftCount++ else if (char == ')') rightCount++
            if (leftCount != 0 && leftCount == rightCount && char == ')') return i
        }
        if (leftCount == 0 && rightCount == 0) return 0
        throw IllegalArgumentException("No matching parenthesis found for $this - unequal amount of left and right?")
    }

    private fun String.getArguments(): List<String> {
        val arguments = mutableListOf<String>()

        var leftParentheses = 0
        var rightParentheses = 0
        var startIndex = 0
        forEachIndexed { i, char ->
            if (char == '(') leftParentheses++
            else if (char == ')') rightParentheses++
            else if (char == ',' && leftParentheses == rightParentheses) {
                if (i == 0) throw MathException("Argument provided was simply a comma. Not allowed")
                arguments.add(substring(startIndex, i))
                startIndex = i + 1
            } else if (i == lastIndex) {
                if (leftParentheses != rightParentheses) throw MathException("Parentheses didn't match up")
                arguments.add(substring(startIndex))
            }
        }

        return arguments
    }

    fun addFunction(mathFunction: MathFunction) {
        if (mathFunction.name.length < 2) throw IllegalArgumentException("Function must have a name >1 character!")
        if (!mathFunction.name[mathFunction.name.lastIndex - 1].isLetter() || mathFunction.pseudonyms.any { pseudo -> pseudo.lastOrNull()?.let { !it.isLetter() } == true }) {
            throw IllegalArgumentException("The second-to-last letter in a function name must be a letter. eg: tan2 is ok, but tan2h isn't")
        }
        if (functions.any { it.name == mathFunction.name }) throw IllegalArgumentException("Only one function can have the name ${mathFunction.name}!")
        functions.add(mathFunction)
    }

    fun addConstant(name: String, value: (MathContext) -> BigDecimal) {
        if (name.equals("pi", true) || name.equals("e", true)) {
            throw IllegalArgumentException("Hi. Why would you even try that?")
        }
        constants[name.toLowerCase()] = value
    }

    fun tokenize(before: String): MutableList<String> {
        val expression = before.replace("-+", "-").replace("+-", "-")
                .replace("--", "+")
        val tokenized = mutableListOf<String>()
        var start = 0
        expression.forEachIndexed { i, char ->
            if (char.isOperator() && ((i > 0 && !((char == '-' || char == '+') && (Operator.values().map { it.value }.contains(expression[i - 1].toString())))) || (i == 0 && char != '-' && char != '+'))) {
                if (i != start) tokenized.add(expression.substring(start, i))
                val last = tokenized.last()
                if (last[0].isOperator() && last[0] != '-' && last[0] != '+' && char != '-' && char != '+') throw IllegalArgumentException("Invalid expression.")
                tokenized.add(expression[i].toString())
                start = i + 1
            } else if (i == expression.lastIndex) tokenized.add(expression.substring(start))
        }
        (0..tokenized.lastIndex)
                .filter { tokenized[it].length > 1 && tokenized[it].toBigDecimalOrNull() == null }
                .forEach { tokenized[it] = evaluateBase(tokenized[it]).toString() }
        return tokenized
    }
}

fun getConstants() = arrayOf("pi" to { context: MathContext -> BigDecimalMath.pi(context) }, "e" to { context: MathContext -> BigDecimalMath.e(context) })