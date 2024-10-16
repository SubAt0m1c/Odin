package me.odinclient.features.impl.render

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.utils.addVec
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.RenderUtils.renderVec
import me.odinmain.utils.render.Renderer
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.monster.EntityCreeper

object Ghosts : Module(
    name = "Ghosts",
    description = "Diverse QOL for ghosts in the Dwarven Mines.",
    category = Category.SKYBLOCK
) {
    private var showGhostNametag by BooleanSetting(name = "Show Ghost Nametag", description = "Show the ghost's name tag.")
    private var showGhosts by BooleanSetting(name = "Hide Ghosts", description = "Hide ghosts.")
    private var hideChargedLayer by BooleanSetting(name = "Hide Charged Layer", description = "Hide the charged layer of the ghost.")

    init {
        execute(500) {
            mc.theWorld?.loadedEntityList?.forEach { entity ->
                if (entity !is EntityCreeper || entity.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue < 1000000) return@forEach
                entity.isInvisible = showGhosts
                entity.dataWatcher.updateObject(17, (if (hideChargedLayer) 0 else 1).toByte())

                if (showGhostNametag) drawGhostNameTag(entity)
            }
        }
    }

    private fun drawGhostNameTag(creeper: EntityCreeper) {
        val currentHealth = creeper.health
        val maxHealth = creeper.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue
        val isRunic = maxHealth == 4000000.0
        val bracketsColor = if (isRunic) "&5" else "&8"
        val lvlColor = if (isRunic) "&d" else "&7"
        val nameColor = if (isRunic) "&5" else "&c"
        val currentHealthColor = if (isRunic) "&d" else if (currentHealth < maxHealth / 2) "&e" else "&a"
        val maxHealthColor = if (isRunic) "&5" else "&a"
        val name = "${bracketsColor}[${lvlColor}Lv250${bracketsColor}] ${nameColor + if (isRunic) "Runic " else ""}Ghost ${currentHealthColor + transformToSuffixedNumber(currentHealth.toDouble()) + "&f"}/${maxHealthColor + transformToSuffixedNumber(maxHealth) + "&c" + "❤"}".replace("&", "§")

        Renderer.drawStringInWorld(name, creeper.renderVec.addVec(y = creeper.height + 0.5), Color.WHITE, depth = false)
    }

    private fun transformToSuffixedNumber(number: Double): String {
        val result: String = if (number >= 1000000) {
            val short = (number / 1000000).toString()
            val shortSplit = short.split(".")
            if (shortSplit[1] != "0") short else shortSplit[0] + "M"
        } else (number / 1000).toInt().toString() + "k"
        return result
    }
}