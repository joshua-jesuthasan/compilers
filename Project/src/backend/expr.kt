package backend
import java.io.File
import java.io.IOException

// Base Expression Class
abstract class Expr {
    abstract fun eval(runtime: Runtime): Data
}

// Implementing the basic expressions
class IntLiteral(private val value: String) : Expr() {
    override fun eval(runtime: Runtime): Data = IntData(value.toInt())
}

class StringLiteral(private val value: String) : Expr() {
    override fun eval(runtime: Runtime): Data = StringData(value)
        
    fun getValue(): String = value

}

class Deref(private val name: String) : Expr() {
    override fun eval(runtime: Runtime): Data =
        runtime.symbolTable[name] ?: throw RuntimeException("Undefined variable: $name")
}


// Arithmetic operations
class Arithmetics(private val operator: Operator, private val e1: Expr, private val e2: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        when (operator) {
            Operator.Add -> {
                val left = e1.eval(runtime)
                val right = e2.eval(runtime)
                return when {
                    left is IntData && right is IntData -> IntData(left.value + right.value)
                    left is StringData || right is StringData -> StringData(left.toString() + right.toString())
                    else -> throw RuntimeException("Unsupported operands for +: ${left::class.simpleName}, ${right::class.simpleName}")
                }
            }
            Operator.Mul -> {
                val left = e1.eval(runtime)
                val right = e2.eval(runtime)
                return when {
                    left is IntData && right is IntData -> IntData(left.value * right.value)
                    left is StringData && right is IntData -> StringData(left.value.repeat(right.value))
                    left is IntData && right is StringData -> StringData(right.value.repeat(left.value))
                    else -> throw RuntimeException("Unsupported operands for *")
                }
            }
            Operator.Sub -> {
                val left = e1.eval(runtime) as? IntData ?: throw RuntimeException("Left operand is not an IntData for -")
                val right = e2.eval(runtime) as? IntData ?: throw RuntimeException("Right operand is not an IntData for -")
                return IntData(left.value - right.value)
            }
            Operator.Div -> {
                val left = e1.eval(runtime) as? IntData ?: throw RuntimeException("Left operand is not an IntData for /")
                val right = e2.eval(runtime) as? IntData ?: throw RuntimeException("Right operand is not an IntData for /")
                if (right.value == 0) throw RuntimeException("Division by zero")
                return IntData(left.value / right.value)
            }
        }
    }
}

// Adjusting to use String for operator as per grammar
enum class Operator { Add, Mul, Sub, Div }

class Compare(private val comparator: Comparator, private val e1: Expr, private val e2: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val left = e1.eval(runtime)
        val right = e2.eval(runtime)

        if (left !is IntData || right !is IntData) {
            throw RuntimeException("Comparison operands must be integers")
        }

        return when (comparator) {
            Comparator.GT -> BoolData(left.value > right.value)
            Comparator.LT -> BoolData(left.value < right.value)
            Comparator.EQ -> BoolData(left.value == right.value)
            Comparator.GE -> BoolData(left.value >= right.value)
            // You could add handling for LE (less than or equal to) if necessary
        }
    }
}

enum class Comparator { GT, LT, EQ, GE }


class Assign(private val name: String, private val expr: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val result = expr.eval(runtime)
        runtime.symbolTable[name] = result
        return result
    }
}

// Special constructs
class Block(private val statements: List<Expr>) : Expr() {
    override fun eval(runtime: Runtime): Data {
        var result: Data = NoneData // Initialize with a default value.
        statements.forEach { statement ->
            result = statement.eval(runtime) // Evaluate each statement and update 'result'.
        }
        return result // Return the result of the last expression.
    }
}


// Placeholder implementations for missing classes to match grammar actions
class PrintExpr(private val expr: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val result = expr.eval(runtime)
        println(result)
        return NoneData // Assuming print does not return a value
    }
}


// Example placeholder for AddString
class AddString(private val e1: Expr, private val e2: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val val1 = e1.eval(runtime).toString()
        val val2 = e2.eval(runtime).toString()
        return StringData(val1 + val2)
    }
}

class Invoke(private val functionName: String, private val args: List<Expr>) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val functionData = runtime.symbolTable[functionName] as? FunctionData
            ?: throw Exception("Function '$functionName' not found.")
        
        val evaluatedArgs = args.map { it.eval(runtime) }
        return functionData.invoke(evaluatedArgs, runtime)
    }
}


class Ifelse(private val condition: Expr, private val thenBranch: Expr, private val elseBranch: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        // Evaluate the condition
        val conditionResult = condition.eval(runtime)
        
        // Ensure conditionResult is BoolData for the comparison
        if (conditionResult !is BoolData) {
            throw RuntimeException("Condition did not evaluate to a boolean")
        }
        
        // Based on the condition's truth value, evaluate and return the corresponding branch
        return if (conditionResult.value) {
            thenBranch.eval(runtime)
        } else {
            elseBranch.eval(runtime)
        }
    }
}


class Declare(private val name: String, private val params: List<String>, private val body: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        // Here, we wrap the function's logic into a FunctionData object
        val functionData = FunctionData(params, body, runtime)
        runtime.symbolTable[name] = functionData
        return NoneData // Declarations generally don't produce a value.
    }
}

class ForLoop(private val iterator: String, private val start: Expr, private val end: Expr, private val body: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val startVal = start.eval(runtime) as? IntData ?: return NoneData
        val endVal = end.eval(runtime) as? IntData ?: return NoneData
        var last: Data = NoneData
        for (i in startVal.value..endVal.value) {
            runtime.symbolTable[iterator] = IntData(i)
            last = body.eval(runtime)
        }
        return last
    }
}


class InterpolatedStringExpr(val interpolatedText: String) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val regex = Regex("\\$\\{([^}]+)\\}")
        return StringData(interpolatedText.replace(regex) {
            val expression = it.groups[1]?.value ?: ""
            try {
                val expr = parseExpression(expression, runtime)  // Parse and evaluate the expression inside ${}
                expr.eval(runtime).toString()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        })
    }
}

fun parseExpression(expression: String, runtime: Runtime): Expr {
    // Remove spaces for simplification
    val trimmedExpr = expression.replace("\\s+".toRegex(), "")

    // Handle basic arithmetic (+, -, *, /)
    val operators = listOf('+', '-', '*', '/')
    var lastOperatorIndex = -1
    for (i in trimmedExpr.indices.reversed()) {
        if (operators.contains(trimmedExpr[i])) {
            lastOperatorIndex = i
            break
        }
    }

    // If an operator is found, split the expression
    if (lastOperatorIndex != -1) {
        val operator = trimmedExpr[lastOperatorIndex]
        val left = trimmedExpr.substring(0, lastOperatorIndex)
        val right = trimmedExpr.substring(lastOperatorIndex + 1)

        val leftExpr = parseExpression(left, runtime)
        val rightExpr = parseExpression(right, runtime)

        return when (operator) {
            '+' -> Arithmetics(Operator.Add, leftExpr, rightExpr)
            '-' -> Arithmetics(Operator.Sub, leftExpr, rightExpr)
            '*' -> Arithmetics(Operator.Mul, leftExpr, rightExpr)
            '/' -> Arithmetics(Operator.Div, leftExpr, rightExpr)
            else -> throw IllegalArgumentException("Unsupported operator $operator")
        }
    }

    // Handle numbers
    trimmedExpr.toIntOrNull()?.let {
        return IntLiteral(it.toString())
    }

    // Handle variables (assuming they are stored in the runtime)
    return runtime.symbolTable[trimmedExpr]?.let {
        Deref(trimmedExpr)
    } ?: throw RuntimeException("Undefined variable or unsupported expression: $trimmedExpr")
}


class NegationExpr(private val expr: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val result = expr.eval(runtime)
        if (result is IntData) {
            return IntData(-result.value)
        } else {
            throw RuntimeException("Negation applied to non-integer type")
        }
    }
}

class TernaryExpr(val condition: Expr, val trueExpr: Expr, val falseExpr: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val cond = condition.eval(runtime) as? BoolData ?: throw RuntimeException("Ternary condition is not a boolean")
        return if (cond.value) trueExpr.eval(runtime) else falseExpr.eval(runtime)
    }
}

class ReadFileExpr(private val filePathExpr: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        // Evaluate the filePath expression to get the actual file path
        val filePath = filePathExpr.eval(runtime)
        
        if (filePath !is StringData) {
            throw RuntimeException("File path must be a string")
        }

        return try {
            val content = File(filePath.value).readText()
            StringData(content)  // Return the content as StringData
        } catch (e: IOException) {
            throw RuntimeException("Failed to read file: ${filePath.value} (${e.message})")
        }
    }
}

class SplitStringExpr(private val stringToSplit: Expr, private val delimiter: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val stringData = stringToSplit.eval(runtime) as? StringData
            ?: throw RuntimeException("The first argument to split must be a string.")
        val delimiterData = delimiter.eval(runtime) as? StringData
            ?: throw RuntimeException("The delimiter must be a string.")

        val results = stringData.value.split(delimiterData.value).map { StringData(it) }
        return ListData(results)
    }
}

class LogicalAndExpr(val left: Expr, val right: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val leftResult = left.eval(runtime) as? BoolData ?: throw RuntimeException("Left operand is not a Boolean")
        val rightResult = right.eval(runtime) as? BoolData ?: throw RuntimeException("Right operand is not a Boolean")
        return BoolData(leftResult.value && rightResult.value)
    }
}

class LogicalOrExpr(val left: Expr, val right: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val leftResult = left.eval(runtime) as? BoolData ?: throw RuntimeException("Left operand is not a Boolean")
        val rightResult = right.eval(runtime) as? BoolData ?: throw RuntimeException("Right operand is not a Boolean")
        return BoolData(leftResult.value || rightResult.value)
    }
}

class LogicalNotExpr(val expr: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val result = expr.eval(runtime) as? BoolData ?: throw RuntimeException("Operand is not a Boolean")
        return BoolData(!result.value)
    }
}

class BoolLiteral(private val value: String) : Expr() {
    override fun eval(runtime: Runtime): Data {
        return BoolData(value.toBoolean())
    }
}

