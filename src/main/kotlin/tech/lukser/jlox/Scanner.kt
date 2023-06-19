package tech.lukser.jlox

import tech.lukser.jlox.common.Token
import tech.lukser.jlox.common.TokenType

class Scanner(private val source: String) {
    private val tokens: MutableList<Token> = ArrayList()
    private var start = 0
    private var current = 0
    private var line = 1
    fun scanTokens(): List<Token> {
        while (!isAtEnd) {
            start = current
            scanToken()
        }
        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            ';' -> addToken(TokenType.SEMICOLON)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            '*' -> addToken(TokenType.STAR)
            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '/' -> if (match('/')) {
                while (peek() != '\n' && !isAtEnd) advance()
            } else {
                addToken(TokenType.SLASH)
            }

            ' ', '\r', '\t' -> {}
            '\n' -> line++
            '"' -> string()
            else -> if (isDigit(c)) {
                number()
            } else if (isAlpha(c)) {
                identifier()
            } else {
                Lox.error(line, "Unexpected character: $c")
            }
        }
    }

    private fun identifier() {
        while (isAlphaNumeric(peek())) advance()
        val text = source.substring(start, current)
        val type = keywords.getOrDefault(text, TokenType.IDENTIFIER)
        addToken(type)
    }

    private fun isAlpha(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_' || c in 'а'..'я' || c in 'А'..'Я'
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    private fun number() {
        while (isDigit(peek())) advance()
        if (peek() == '.' && isDigit(peekNext())) {
            advance()
            while (isDigit(peek())) advance()
        }
        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }

    private fun peekNext(): Char {
        return if (current + 1 >= source.length) '\u0000' else source[current + 1]
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd) {
            if (peek() == '\n') line++
            advance()
        }
        if (isAtEnd) {
            Lox.error(line, "Unterminated string")
            return
        }
        advance() // The closing quote
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    private fun peek(): Char {
        return if (isAtEnd) '\u0000' else source[current]
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private val isAtEnd: Boolean
        get() = current >= source.length

    companion object {
        private val keywords: MutableMap<String, TokenType> = HashMap()

        init {
            keywords["and"] = TokenType.AND
            keywords["class"] = TokenType.CLASS
            keywords["else"] = TokenType.ELSE
            keywords["false"] = TokenType.FALSE
            keywords["for"] = TokenType.FOR
            keywords["fun"] = TokenType.FUN
            keywords["if"] = TokenType.IF
            keywords["nil"] = TokenType.NIL
            keywords["or"] = TokenType.OR
            keywords["print"] = TokenType.PRINT
            keywords["return"] = TokenType.RETURN
            keywords["super"] = TokenType.SUPER
            keywords["this"] = TokenType.THIS
            keywords["true"] = TokenType.TRUE
            keywords["var"] = TokenType.VAR
            keywords["while"] = TokenType.WHILE
        }
    }
}
