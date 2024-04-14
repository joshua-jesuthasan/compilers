package backend

sealed interface Data {
    override fun toString(): String
}

data class IntData(val value: Int) : Data {
    
    override fun toString(): String = value.toString()
}

data class StringData(val value: String) : Data {
    override fun toString(): String = value
}

// Represents a "void" or "none" type, useful for expressions that don't produce a value.
object NoneData : Data {
    override fun toString(): String = "None"
}

// Placeholder for Boolean data type, assuming your language might need it.
data class BoolData(val value: Boolean) : Data {
    override fun toString(): String = value.toString()
}

data class ListData(val elements: List<Data>) : Data {
    override fun toString() = elements.joinToString(prefix = "[", postfix = "]") { it.toString() }
}

data class ObjectData(val properties: Map<String, Data>) : Data {
    override fun toString() = properties.map { "${it.key}: ${it.value}" }.joinToString(prefix = "{", postfix = "}")
}

// this class represents a function as a data type.
// It holds the function's parameter names, the body as an expression, and the runtime scope.
data class FunctionData(
    val parameters: List<String>, 
    val body: Expr, 
    val closure: Runtime
) : Data {
    // Invoke executes the function with given arguments within its closure scope.
    fun invoke(args: List<Data>, runtime: Runtime): Data {
        if (args.size != parameters.size) {
            throw IllegalArgumentException("Function expected ${parameters.size} arguments but got ${args.size}.")
        }
        // Prepare a new runtime scope for the function execution.
        val newScope = runtime.subscope(parameters.zip(args).toMap())
        return body.eval(newScope)
    }

    override fun toString(): String = "Function(${parameters.joinToString(", ")})"
}

