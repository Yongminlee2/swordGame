package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.geomgang.game.R

/** 기준 시안의 어두운 격자와 불씨를 모든 게임 화면의 공통 무대로 사용한다. */
@Composable
fun ForgeBackdrop(content: @Composable BoxScope.() -> Unit) {
    val deep = LocalForgeDeep.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(
                if (deep) R.drawable.forge_background_season2
                else R.drawable.forge_background_season1,
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (deep) 0.94f else 0.88f,
            filterQuality = FilterQuality.None,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.08f)),
        )
        content()
    }
}

/** 생성한 16비트 UI 자산을 흐림 없이 표시한다. */
@Composable
fun PixelIcon(
    @DrawableRes resource: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Image(
        bitmap = ImageBitmap.imageResource(resource),
        contentDescription = contentDescription,
        modifier = modifier.alpha(alpha),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
    )
}

/** 기준 시안의 모서리가 한 픽셀 잘린 정보 패널. */
@Composable
fun ForgePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = CutCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(content = content)
    }
}

@Composable
fun ForgeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelIcon(
                resource = R.drawable.ui_pixel_bolt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = title,
                modifier = Modifier.padding(start = 5.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp,
            )
        }
        subtitle?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SeasonStamp(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Text(
        text = if (LocalForgeDeep.current) "시즌 II" else "시즌 I",
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        fontSize = if (compact) 10.sp else 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.4.sp,
    )
}

@Composable
fun ThinRule(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    Spacer(
        modifier = modifier
            .height(thickness)
            .background(color),
    )
}

/** 기준 시안의 황금색 픽셀 베벨 강화 버튼. */
@Composable
fun PixelActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = CutCornerShape(6.dp)
    val background = if (enabled) ForgeAmber else MaterialTheme.colorScheme.surfaceVariant
    val border = if (enabled) Color(0xFFFFCF52) else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .border(2.dp, border, shape)
            .background(background, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Spacer(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(if (enabled) Color(0xFFFFD76B) else border),
        )
        Spacer(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
                .background(if (enabled) Color(0xFFC77B13) else border),
        )
        content()
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CutCornerShape(4.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CutCornerShape(4.dp),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}
