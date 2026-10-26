package com.armsx2.gamepad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Shared backend actions: preserve the emulator's callbacks and state, match NS pixel chrome. */
@Composable
fun pixelButton(
    onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=ButtonDefaults.shape,colors:ButtonColors=ButtonDefaults.buttonColors(),
    elevation:ButtonElevation?=ButtonDefaults.buttonElevation(),border:BorderStroke?=null,
    contentPadding:PaddingValues=ButtonDefaults.ContentPadding,interactionSource:MutableInteractionSource?=null,
    content:@Composable RowScope.()->Unit,
) {
    Button(onClick=onClick,modifier=modifier,enabled=enabled,
        shape=if(PsProduct.active)PsPixel.shape(0.dp)else shape,
        colors=colors,elevation=if(PsProduct.active)null else elevation,
        border=if(PsProduct.active)BorderStroke(2.dp,MaterialTheme.colorScheme.outline)else border,
        contentPadding=contentPadding,interactionSource=interactionSource,content=content)
}
@Composable
fun pixelOutlinedButton(
    onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=ButtonDefaults.outlinedShape,colors:ButtonColors=ButtonDefaults.outlinedButtonColors(),
    elevation:ButtonElevation?=null,border:BorderStroke?=null,
    contentPadding:PaddingValues=ButtonDefaults.ContentPadding,interactionSource:MutableInteractionSource?=null,
    content:@Composable RowScope.()->Unit,
) {
    OutlinedButton(onClick=onClick,modifier=modifier,enabled=enabled,
        shape=if(PsProduct.active)PsPixel.shape(0.dp)else shape,
        colors=if(PsProduct.active)ButtonDefaults.outlinedButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)else colors,
        elevation=elevation,border=border ?: BorderStroke(if(PsProduct.active)2.dp else 1.dp,MaterialTheme.colorScheme.outline),
        contentPadding=contentPadding,interactionSource=interactionSource,content=content)
}
@Composable
fun pixelTextButton(
    onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,
    shape:Shape=ButtonDefaults.textShape,colors:ButtonColors=ButtonDefaults.textButtonColors(),
    elevation:ButtonElevation?=null,border:BorderStroke?=null,
    contentPadding:PaddingValues=ButtonDefaults.TextButtonContentPadding,interactionSource:MutableInteractionSource?=null,
    content:@Composable RowScope.()->Unit,
) {
    TextButton(onClick=onClick,modifier=modifier,enabled=enabled,
        shape=if(PsProduct.active)PsPixel.shape(0.dp)else shape,
        colors=if(PsProduct.active)ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface)else colors,
        elevation=elevation,border=if(PsProduct.active)BorderStroke(2.dp,MaterialTheme.colorScheme.outline)else border,
        contentPadding=contentPadding,interactionSource=interactionSource,content=content)
}
