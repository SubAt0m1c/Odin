package com.github.stivais.ui.renderer

import com.github.stivais.ui.UI
import com.github.stivais.ui.color.Color

interface Renderer {

    fun beginFrame(width: Float, height: Float)

    fun endFrame()

    fun supportsFramebuffers(): Boolean = false

    fun createFramebuffer(w: Float, h: Float): Framebuffer = Framebuffer(w, h)

    fun destroyFramebuffer(fbo: Framebuffer) {}

    fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float) {}

    fun bindFramebuffer(fbo: Framebuffer) {}

    fun unbindFramebuffer() {}

    fun push()

    fun pop()

    fun scale(x: Float, y: Float)

    fun translate(x: Float, y: Float)

    fun pushScissor(x: Float, y: Float, w: Float, h: Float)

    fun popScissor()

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int)

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, tl: Float, bl: Float, br: Float, tr: Float)

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) {
        rect(x, y, w, h, color, radius, radius, radius, radius)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, tl: Float, bl: Float, br: Float, tr: Float)

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float = 0f) {
        hollowRect(x, y, w, h, thickness, color, radius, radius, radius, radius)
    }

    fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Int, color2: Int, direction: GradientDirection)

    fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Int, color2: Int, radius: Float, direction: GradientDirection)

    // todo: add fonts

    fun text(text: String, x: Float, y: Float, size: Float, color: Int = Color.WHITE.rgba, font: Font = UI.defaultFont)

    fun textWidth(text: String, size: Float, font: Font = UI.defaultFont): Float

}