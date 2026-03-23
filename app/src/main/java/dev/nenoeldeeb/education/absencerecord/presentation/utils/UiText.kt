package dev.nenoeldeeb.education.absencerecord.presentation.utils

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastJoinToString

sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class StringResource(
        @param:StringRes val resId: Int,
        vararg val args: Any
    ) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StringResource

            if (resId != other.resId) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    class PluralResource(
        @param:PluralsRes val resId: Int,
        val quantity: Int,
        vararg val args: Any
    ) : UiText {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PluralResource

            if (resId != other.resId) return false
            if (quantity != other.quantity) return false
            if (!args.contentEquals(other.args)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + quantity
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    data class Joined(
        val parts: List<UiText>,
        val separator: String = ""
    ) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                val resolvedArgs =
                    args.map { arg ->
                        if (arg is UiText) arg.asString() else arg
                    }.toTypedArray()
                stringResource(resId, *resolvedArgs)
            }

            is PluralResource -> {
                val resolvedArgs =
                    args.map { arg ->
                        if (arg is UiText) arg.asString() else arg
                    }.toTypedArray()
                pluralStringResource(resId, quantity, *resolvedArgs)
            }

            is Joined -> parts.map { it.asString() }.fastJoinToString(separator)
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                val resolvedArgs =
                    args.map { arg ->
                        if (arg is UiText) arg.asString(context) else arg
                    }.toTypedArray()
                context.getString(resId, *resolvedArgs)
            }

            is PluralResource -> {
                val resolvedArgs =
                    args.map { arg ->
                        if (arg is UiText) arg.asString(context) else arg
                    }.toTypedArray()
                context.resources.getQuantityString(resId, quantity, *resolvedArgs)
            }

            is Joined -> parts.joinToString(separator) { it.asString(context) }
        }
    }
}