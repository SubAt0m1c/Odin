package com.github.stivais.ui

import me.odinmain.OdinMain.display
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display

open class UIScreen(val ui: UI) : GuiScreen() {

    private var previousWidth: Int = 0
    private var previousHeight: Int = 0

    override fun initGui() {
        ui.initialize(Display.getWidth(), Display.getHeight())
    }

    override fun onGuiClosed() {
        ui.cleanup()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        ui.measureMetrics {
            val w = mc.framebuffer.framebufferWidth
            val h = mc.framebuffer.framebufferHeight
            if (w != previousWidth || h != previousHeight) {
                ui.resize(w, h)
                previousWidth = w
                previousHeight = h
            }

            ui.eventManager?.apply {
                val mx = Mouse.getX().toFloat()
                val my = previousHeight - Mouse.getY() - 1f

                if (this.mouseX != mx || this.mouseY != my) {
                    onMouseMove(mx, my)
                }

                val scroll = Mouse.getEventDWheel()
                if (scroll != 0) {
                    onMouseScroll(scroll.toFloat())
                }
            }
            GlStateManager.pushMatrix()
            ui.render()
            GlStateManager.popMatrix()
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, button: Int) {
        ui.eventManager?.onMouseClick(button)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, button: Int) {
        ui.eventManager?.onMouseRelease(button)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (ui.eventManager?.onKeyType(typedChar) == true) return
        if (ui.eventManager?.onKeycodePressed(keyCode) == true) return
        super.keyTyped(typedChar, keyCode)
    }

//    no key released because 1.8.9 doesn't have it and I don't want to manually recreate it
//    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
//        if (ui.eventManager?.onKeyReleased(keyCode) == true) {
//            return true
//        }
//        return super.keyPressed(keyCode, scanCode, modifiers)
//    }

    override fun doesGuiPauseGame(): Boolean = false

    companion object {
        fun open(ui: UI) {
            display = UIScreen(ui)
        }

        @JvmName("openUI")
        fun UI.open() {
            open(this)
        }
    }
}