package me.odinclient.features.impl.floor7.p3

import me.odinmain.events.impl.GuiEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.impl.floor7.p3.TerminalSolver
import me.odinmain.features.impl.floor7.p3.TerminalTypes
import me.odinmain.features.settings.impl.*
import me.odinmain.utils.clock.Clock
import me.odinmain.utils.skyblock.PlayerUtils
import me.odinmain.utils.skyblock.PlayerUtils.windowClick
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object AutoTerms : Module(
    name = "Auto Terms",
    description = "Automatically solves terminals.",
    category = Category.FLOOR7,
    tag = TagType.RISKY
) {
    private val autoDelay by NumberSetting("Delay", 170L, 130, 300, unit = "ms", description = "Delay between clicks.")
    private val firstClickDelay by NumberSetting("First Click Delay", 350L, 300, 500, unit = "ms", description = "Delay before first click.")
    private val middleClick by DualSetting("Click Type", "Left", "Middle", default = true, description = "What Click type to use.")
    private val breakThreshold by NumberSetting("Break Threshold", 500L, 350L, 1000L, 10L, unit = "ms", description = "Time before breaking the click.")
    private val clickingOrder by SelectorSetting("Clicking order", "from first", arrayListOf("from first", "from last", "random"), description = "The order to click the items in.")
    private val disableMelody by BooleanSetting("Disable Melody", false, description = "Disables melody terminals.")
    private val clock = Clock(autoDelay)
    private var breakClock = Clock(breakThreshold)
    private var clickedThisWindow = false

    @SubscribeEvent
    fun onGuiLoaded(event: GuiEvent.GuiLoadedEvent) {
        clickedThisWindow = false
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (breakClock.hasTimePassed(breakThreshold) && clickedThisWindow) clickedThisWindow = false
        if (
            TerminalSolver.currentTerm.solution.isEmpty() ||
            !clock.hasTimePassed(autoDelay) ||
            System.currentTimeMillis() - TerminalSolver.currentTerm.timeOpened <= firstClickDelay ||
            clickedThisWindow ||
            event.phase != TickEvent.Phase.START ||
            mc.thePlayer?.openContainer !is ContainerChest ||
            (disableMelody && TerminalSolver.currentTerm.type == TerminalTypes.MELODY)
        ) return

        val item =
            if (clickingOrder == 0) TerminalSolver.currentTerm.solution.firstOrNull() ?: return else if (clickingOrder == 1) TerminalSolver.currentTerm.solution.lastOrNull() ?: return else TerminalSolver.currentTerm.solution.random()

        when (TerminalSolver.currentTerm.type) {
            TerminalTypes.RUBIX ->
                windowClick(item, if (TerminalSolver.currentTerm.solution.count { it == item } >= 3) PlayerUtils.ClickType.Right else if (middleClick) PlayerUtils.ClickType.Middle else PlayerUtils.ClickType.Left)

            TerminalTypes.ORDER ->
                windowClick(TerminalSolver.currentTerm.solution.first(), if (middleClick) PlayerUtils.ClickType.Middle else PlayerUtils.ClickType.Left)

            TerminalTypes.MELODY ->
                windowClick(TerminalSolver.currentTerm.solution.find { it % 9 == 7 } ?: return, if (middleClick) PlayerUtils.ClickType.Middle else PlayerUtils.ClickType.Left)

            else -> windowClick(item, if (middleClick) PlayerUtils.ClickType.Middle else PlayerUtils.ClickType.Left)
        }
        clickedThisWindow = true
        clock.update()
        breakClock.update()
    }
}
