package tech.lukser.jlox.common

class Token(val type: TokenType, val lexeme: String, val literal: Any?, val line: Int) {
    override fun toString(): String {
        return String.format("%s %s %s", type, lexeme, literal)
    }
}
