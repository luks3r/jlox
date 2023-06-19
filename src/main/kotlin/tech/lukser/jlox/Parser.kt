package tech.lukser.jlox

import tech.lukser.jlox.common.Expr
import tech.lukser.jlox.common.Expr.Assign
import tech.lukser.jlox.common.Stmt
import tech.lukser.jlox.common.Token
import tech.lukser.jlox.common.TokenType
import tech.lukser.jlox.errors.ParseError

class Parser internal constructor(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<Stmt?> {
        val statements: MutableList<Stmt?> = ArrayList()
        while (!isAtEnd) {
            statements.add(declaration())
        }
        return statements
    }

    private fun declaration(): Stmt? {
        return try {
            if (match(TokenType.CLASS)) return classDeclaration()
            if (match(TokenType.FUN)) return function("function");
            if (match(TokenType.VAR)) varDeclaration() else statement()
        } catch (err: ParseError) {
            synchronize()
            null
        }
    }

    private fun classDeclaration(): Stmt? {
        val name = consume(TokenType.IDENTIFIER, "Expected class name")
        var superclass: Expr.Variable? = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expected superclass name")
            superclass = Expr.Variable(previous())
        }
        consume(TokenType.LEFT_BRACE, "Expected '{' before class body")

        val methods = mutableListOf<Stmt.Function>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd) {
            methods.add(function("method"))
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after class body")
        return Stmt.Class(name, superclass, methods)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(TokenType.IDENTIFIER, "Expected $kind name")
        consume(TokenType.LEFT_PAREN, "Expected '(' after $kind name")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RIGHT_PAREN)) do {
            if (parameters.size >= 255) error(peek(), "Cannot have more than 255 parameters")
            parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name"))
        } while (match(TokenType.COMMA))
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters")

        consume(TokenType.LEFT_BRACE, "Expected '{' before $kind body")
        val body = block()
        return Stmt.Function(name, parameters.toList(), body.filterNotNull())
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name")
        var initializer: Expr? = null
        if (match(TokenType.EQUAL)) {
            initializer = expression()
        }
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        if (match(TokenType.FOR)) return forStatement()
        if (match(TokenType.IF)) return ifStatement()
        if (match(TokenType.PRINT)) return printStatement()
        if (match(TokenType.RETURN)) return returnStatement()
        if (match(TokenType.WHILE)) return whileStatement()
        return if (match(TokenType.LEFT_BRACE)) Stmt.Block(block()) else expressionStatement()
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(TokenType.SEMICOLON)) {
            value = expression()
        }

        consume(TokenType.SEMICOLON, "Expected ';' after return value")
        return Stmt.Return(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'")

        val initializer: Stmt? =
            if (match(TokenType.SEMICOLON)) null else if (match(TokenType.VAR)) varDeclaration() else expressionStatement()

        var condition: Expr? = null
        if (!check(TokenType.SEMICOLON)) condition = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after 'for' condition")

        var increment: Expr? = null
        if (!check(TokenType.RIGHT_PAREN)) increment = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after 'for' clauses")

        var body = statement()

        if (increment != null) body = Stmt.Block(
            listOf(body, Stmt.Expression(increment))
        )

        if (condition == null) condition = Expr.Literal(true)
        body = Stmt.While(condition, body)

        if (initializer != null) body = Stmt.Block(listOf(initializer, body))
        return body
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after 'while' condition")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expected ')' after 'if' condition")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(TokenType.ELSE)) {
            elseBranch = statement()
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun block(): List<Stmt?> {
        val statements: MutableList<Stmt?> = ArrayList()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd) {
            statements.add(declaration())
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after block")
        return statements
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after expression")
        return Stmt.Expression(expr)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected ';' after value")
        return Stmt.Print(value)
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private val isAtEnd: Boolean
        get() = peek().type === TokenType.EOF

    private fun peek(): Token {
        return tokens[current]
    }

    private fun check(type: TokenType): Boolean {
        return if (isAtEnd) false else peek().type === type
    }

    private fun advance(): Token {
        if (!isAtEnd) this.current++
        return previous()
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun parseBinary(next: () -> Expr, vararg types: TokenType): Expr {
        var expr = next()
        while (match(*types)) {
            val operator = previous()
            val right = next()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        val expr = or()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()
            if (expr is Expr.Variable) {
                val name = expr.name
                return Assign(name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }
            error(equals, "Invalid assignment target")
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()
        while (match(TokenType.OR)) {
            val op = previous()
            val rhs = and()
            expr = Expr.Logical(expr, op, rhs)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (match(TokenType.AND)) {
            val op = previous()
            val rhs = equality()
            expr = Expr.Logical(expr, op, rhs)
        }
        return expr
    }

    private fun equality(): Expr {
        return parseBinary({ comparison() }, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)
    }

    private fun comparison(): Expr {
        return parseBinary({ term() }, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)
    }

    private fun term(): Expr {
        return parseBinary({ factor() }, TokenType.PLUS, TokenType.MINUS)
    }

    private fun factor(): Expr {
        return parseBinary({ unary() }, TokenType.SLASH, TokenType.STAR)
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }
        return call()
    }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expected property name after '.'")
                expr = Expr.Get(expr, name)
            } else {
                break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(TokenType.RIGHT_PAREN)) do {
            if (arguments.size >= 255) {
                error(peek(), "Can't have more than 255 arguments")
            }
            arguments.add(expression())
        } while (match(TokenType.COMMA))
        val paren = consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments")
        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        if (match(TokenType.FALSE)) return Expr.Literal(false)
        if (match(TokenType.TRUE)) return Expr.Literal(true)
        if (match(TokenType.NIL)) return Expr.Literal(null)
        if (match(TokenType.NUMBER, TokenType.STRING)) return Expr.Literal(previous().literal!!)
        if (match(TokenType.SUPER)) {
            val keyword = previous()
            consume(TokenType.DOT, "Expected '.' after 'super'")
            val method = consume(TokenType.IDENTIFIER, "Expected superclass method name")
            return Expr.Super(keyword, method)
        }
        if (match(TokenType.THIS)) return Expr.This(previous())
        if (match(TokenType.IDENTIFIER)) return Expr.Variable(previous())
        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression")
            return Expr.Grouping(expr)
        }
        throw error(peek(), "Expected expression")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd) {
            if (previous().type === TokenType.SEMICOLON) return
            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
                else -> {}
            }
            advance()
        }
    }
}
