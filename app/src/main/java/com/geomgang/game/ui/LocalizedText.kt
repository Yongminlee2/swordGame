package com.geomgang.game.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.geomgang.game.i18n.GameTranslator

val LocalGameTranslator = staticCompositionLocalOf { GameTranslator.korean }

/** 텍스트가 아닌 접근성 설명·토스트에서 공통 번역을 사용한다. */
@Composable
fun gameText(source: String): String = LocalGameTranslator.current.translate(source)

/** Material3 [MaterialText]와 같은 String 인자를 받는 게임 공통 텍스트. */
@Composable
fun LText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val translated = gameText(text)
    // 독일어·프랑스어처럼 같은 의미가 길어지는 언어도 한 줄 버튼 안에서 잘리지 않게 한다.
    val fittedFontSize = if (
        maxLines == 1 &&
        fontSize != TextUnit.Unspecified &&
        '\n' !in text &&
        translated.length > text.length
    ) {
        fontSize * (text.length.coerceAtLeast(4).toFloat() / translated.length)
            .coerceIn(0.72f, 1f)
    } else {
        fontSize
    }
    MaterialText(
        text = translated,
        modifier = modifier,
        color = color,
        fontSize = fittedFontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}
