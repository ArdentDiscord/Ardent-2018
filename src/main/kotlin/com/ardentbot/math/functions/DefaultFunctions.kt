package com.ardentbot.math.functions

import ch.obermuhlner.math.big.BigDecimalMath
import com.ardentbot.math.MathException
import com.ardentbot.math.MathParser
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*

fun getDefaultFunctions() = listOf(
        ModFunction(), SinFunction(), CosFunction(), TanFunction(), CscFunction(), SecFunction(), CotFunction(),
        SqrtFunction(), NthRootFunction(), ArcsinFunction(), ArccosFunction(), ArctanFunction(), ArcsecFunction(),
        ArccscFunction(), ArccotFunction(), ATan2Function(), FactorialFunction(),LogFunction(), NaturalLogFunction(),
        RandintFunction(), RandomFunction(), CeilFunction(), FloorFunction(), RoundFunction(), RadiansFunction(),
        DegreesFunction()
)

class ModFunction : MathFunction("mod") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        throw MathException("Two arguments required")
    }

    override fun evaluate(vararg numbers: BigDecimal, parser: MathParser): BigDecimal {
        if (numbers.size != 2) throw MathException("Two arguments required")
        return numbers[0].remainder(numbers[1], parser.mathContext)
    }
}

class SinFunction : MathFunction("sin", "sine") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.sin(number, parser.mathContext)
    }
}

class CosFunction : MathFunction("cos", "cosine") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.cos(number, parser.mathContext)
    }
}

class TanFunction : MathFunction("tan", "tangent") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.tan(number, parser.mathContext)
    }
}

class CscFunction : MathFunction("csc", "cosecant") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimal.ONE.divide(BigDecimalMath.sin(number, parser.mathContext), parser.mathContext)
    }
}

class SecFunction : MathFunction("sec", "secant") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimal.ONE.divide(BigDecimalMath.cos(number, parser.mathContext), parser.mathContext)
    }
}

class CotFunction : MathFunction("cot", "cotangent") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimal.ONE.divide(BigDecimalMath.tan(number, parser.mathContext), parser.mathContext)
    }
}

class SqrtFunction : MathFunction("sqrt", "sqt", "squareroot") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.sqrt(number, parser.mathContext)
    }
}

class NthRootFunction : MathFunction("root", "nroot") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        throw MathException("2 arguments required")
    }

    override fun evaluate(vararg numbers: BigDecimal, parser: MathParser): BigDecimal {
        if (numbers.size != 2) throw MathException("2 arguments required")
        return BigDecimalMath.root(numbers[0], numbers[1], parser.mathContext)
    }
}

class ArcsinFunction : MathFunction("arcsin", "asin") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.asin(number, parser.mathContext)
    }
}

class ArccosFunction : MathFunction("arccos", "acos") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.acos(number, parser.mathContext)
    }
}

class ArctanFunction : MathFunction("arctan", "atan") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.atan(number, parser.mathContext)
    }
}


class ArcsecFunction : MathFunction("arcsec", "asec") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimal.ONE.divide(BigDecimalMath.asin(number, parser.mathContext), parser.mathContext)
    }
}

class ArccscFunction : MathFunction("arccsc", "acsc") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimal.ONE.divide(BigDecimalMath.acos(number, parser.mathContext), parser.mathContext)
    }
}

class ArccotFunction : MathFunction("arccot", "acot") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimal.ONE.divide(BigDecimalMath.tan(number, parser.mathContext), parser.mathContext)
    }
}

class ATan2Function : MathFunction("arctan2", "atan2") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        throw MathException("2 arguments required.")
    }

    override fun evaluate(vararg numbers: BigDecimal, parser: MathParser): BigDecimal {
        if (numbers.size != 2) throw MathException("2 arguments required.")
        return BigDecimalMath.atan2(numbers[0], numbers[1], parser.mathContext)
    }
}

class FactorialFunction : MathFunction("factorial") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.factorial(number.toInt())
    }
}

class DegreesFunction : MathFunction("deg", "degrees") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return number.times(BigDecimal.valueOf(180)).divide(BigDecimalMath.pi(parser.mathContext), parser.mathContext)
    }
}

class RadiansFunction : MathFunction("rad", "radians") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return number.times(BigDecimalMath.pi(parser.mathContext)).divide(BigDecimal.valueOf(180), parser.mathContext)
    }
}

class RoundFunction : MathFunction("round") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return number.setScale(0, RoundingMode.HALF_UP)
    }

    override fun evaluate(vararg numbers: BigDecimal, parser: MathParser): BigDecimal {
        if (numbers.size != 2) throw MathException("2 arguments required: round(decimal amount)")
        return numbers[0].round(MathContext(numbers[1].toInt()))
    }
}

class FloorFunction : MathFunction("floor") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return number.setScale(0, RoundingMode.FLOOR)
    }
}

class CeilFunction : MathFunction("ceil") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return number.setScale(0, RoundingMode.CEILING)
    }
}

class RandomFunction : MathFunction("random") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        throw MathException("No arguments can be used with random(). Use randint(bound) or randint(bound, seed) to generate a random integer")
    }

    override fun evaluate(vararg numbers: BigDecimal, parser: MathParser): BigDecimal {
        return Math.random().toBigDecimal()
    }
}

class RandintFunction : MathFunction("randint", "random2", noArgumentsAllowed = true) {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return Random().nextInt(number.toInt()).toBigDecimal()
    }

    override fun evaluate(vararg numbers: BigDecimal, parser: MathParser): BigDecimal {
        return (if (numbers.isEmpty()) Random().nextInt()
        else Random(numbers[1].toLong()).nextInt(numbers[0].toInt())).toBigDecimal()
    }
}

class NaturalLogFunction : MathFunction("ln", "natlog") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.log(number, parser.mathContext)
    }
}

class LogFunction : MathFunction("log") {
    override fun evaluate(number: BigDecimal, parser: MathParser): BigDecimal {
        return BigDecimalMath.log10(number, parser.mathContext)
    }

    override fun evaluate(vararg numbers: BigDecimal, parser: MathParser): BigDecimal {
        if (numbers.size != 2) throw IllegalArgumentException("Need to provide log(base, number)")
        return BigDecimalMath.log10(numbers[1], parser.mathContext).divide(BigDecimalMath.log10(numbers[0], parser.mathContext),parser.mathContext)
    }
}
