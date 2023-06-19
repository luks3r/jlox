package tech.lukser.jlox

import tech.lukser.jlox.common.Expr
import tech.lukser.jlox.common.Expr.Assign
import tech.lukser.jlox.common.Expr.Logical
import tech.lukser.jlox.common.Token
import tech.lukser.jlox.common.TokenType

object AstPrinter : Expr.Visitor<String?> {
    fun print(expr: Expr): String? {
        return expr.accept(this)
    }

    private fun parenthesize(name: String, vararg expressions: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in expressions) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String? {
        return parenthesize(expr.op.lexeme, expr.lhs, expr.rhs)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String? {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String? {
        return if (expr.value == null) "nil" else expr.value.toString()
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String? {
        return parenthesize(expr.op.lexeme, expr.expression)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String? {
        return parenthesize("var", expr)
    }

    override fun visitAssignExpr(expr: Assign): String? {
        return null
    }

    override fun visitLogicalExpr(expr: Logical): String? {
        return null
    }

    override fun visitCallExpr(expr: Expr.Call): String? {
        return null
    }

    override fun visitGetExpr(expr: Expr.Get): String? {
        return null
    }

    override fun visitSetExpr(expr: Expr.Set): String? {
        return null
    }

    override fun visitThisExpr(expr: Expr.This): String? {
        return null
    }

    override fun visitSuperExpr(expr: Expr.Super): String? {
        return null
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val expr: Expr = Expr.Binary(
            Expr.Unary(Token(TokenType.MINUS, "-", null, 1), Expr.Literal(123)), Token(
                TokenType.STAR, "*", null, 1
            ), Expr.Grouping(Expr.Literal(45.67))
        )
        println(print(expr))
    }
}
