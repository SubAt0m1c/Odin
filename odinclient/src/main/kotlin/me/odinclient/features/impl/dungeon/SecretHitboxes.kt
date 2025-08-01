package me.odinclient.features.impl.dungeon

import me.odinmain.clickgui.settings.impl.BooleanSetting
import me.odinmain.features.Module
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.WITHER_ESSENCE_ID
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import java.util.*

/**
 * @see me.odinclient.mixin.mixins.block
 */
object SecretHitboxes : Module(
    name = "Secret Hitboxes",
    description = "Extends the hitboxes of secret blocks to a full block."
) {
    val lever by BooleanSetting("Lever", false, desc = "Extends the lever hitbox.")
    val button by BooleanSetting("Button", false, desc = "Extends the button hitbox.")
    val essence by BooleanSetting("Essence", false, desc = "Extends the essence hitbox.")
    val chests by BooleanSetting("Chests", false, desc = "Extends the chest hitbox.")

    private val mostSignificantBits = UUID.fromString(WITHER_ESSENCE_ID).mostSignificantBits

    fun isEssence(blockPos: BlockPos): Boolean {
        return essence && (mc.theWorld?.getTileEntity(blockPos) as? TileEntitySkull)?.playerProfile?.id?.mostSignificantBits == mostSignificantBits
    }
}