package me.odinmain.utils.skyblock

import me.odinmain.OdinMain.mc
import me.odinmain.utils.*
import net.minecraft.block.Block
import net.minecraft.block.state.BlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

object EtherWarpHelper {
    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?, val state: BlockState?) {
        inline val vec: Vec3? get() = pos?.let { Vec3(it) }
        companion object {
            val NONE = EtherPos(false, null, null)
        }
    }
    var etherPos: EtherPos = EtherPos.NONE

    /**
     * Gets the position of an entity in the "ether" based on the player's view direction.
     *
     * @param pos The initial position of the entity.
     * @param yaw The yaw angle representing the player's horizontal viewing direction.
     * @param pitch The pitch angle representing the player's vertical viewing direction.
     * @return An `EtherPos` representing the calculated position in the "ether" or `EtherPos.NONE` if the player is not present.
     */
    fun getEtherPos(pos: Vec3, yaw: Float, pitch: Float, distance: Double = 60.0, etherWarp: Boolean = false, returnEnd: Boolean = false): EtherPos {
        val endPos = getLook(yaw = yaw, pitch = pitch).normalize().multiply(factor = distance).add(pos)

        return traverseVoxels(pos, endPos, etherWarp).takeUnless { it == EtherPos.NONE && returnEnd } ?: EtherPos(true, endPos.toBlockPos(), null)
    }

    fun getEtherPos(positionLook: PositionLook = PositionLook(mc.thePlayer.positionVector, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), distance: Double =  56.0 + mc.thePlayer.heldItem.getTunerBonus, etherWarp: Boolean = false): EtherPos {
        return getEtherPos(getPositionEyes(positionLook.pos), positionLook.yaw, positionLook.pitch, distance, etherWarp)
    }

    /**
     * Traverses voxels from start to end and returns the first non-air block it hits.
     * @author Bloom
     */
    private fun traverseVoxels(start: Vec3, end: Vec3, etherWarp: Boolean): EtherPos {
        val (x0, y0, z0) = start
        val (x1, y1, z1) = end

        var (x, y, z) = start.floorVec()
        val (endX, endY, endZ) = end.floorVec()

        val dirX = x1 - x0
        val dirY = y1 - y0
        val dirZ = z1 - z0

        val stepX = sign(dirX).toInt()
        val stepY = sign(dirY).toInt()
        val stepZ = sign(dirZ).toInt()

        val invDirX = if (dirX != 0.0) 1.0 / dirX else Double.MAX_VALUE
        val invDirY = if (dirY != 0.0) 1.0 / dirY else Double.MAX_VALUE
        val invDirZ = if (dirZ != 0.0) 1.0 / dirZ else Double.MAX_VALUE

        val tDeltaX = abs(invDirX * stepX)
        val tDeltaY = abs(invDirY * stepY)
        val tDeltaZ = abs(invDirZ * stepZ)

        var tMaxX = abs((x + max(stepX, 0) - x0) * invDirX)
        var tMaxY = abs((y + max(stepY, 0) - y0) * invDirY)
        var tMaxZ = abs((z + max(stepZ, 0) - z0) * invDirZ)

        repeat(1000) {
            val chunk = mc.theWorld?.chunkProvider?.provideChunk(x.toInt() shr 4, z.toInt() shr 4) ?: return EtherPos.NONE
            val currentBlock = chunk.getBlock(BlockPos(x, y, z))
            val currentBlockId = Block.getIdFromBlock(currentBlock)

            if ((!validEtherwarpFeetIds.get(currentBlockId) && etherWarp) || (currentBlockId != 0 && !etherWarp)) {
                if (!etherWarp && validEtherwarpFeetIds.get(currentBlockId)) return EtherPos(false, BlockPos(x, y, z), currentBlock.blockState)

                val footBlockId = Block.getIdFromBlock(chunk.getBlock(BlockPos(x, y + 1, z)))
                if (!validEtherwarpFeetIds.get(footBlockId)) return EtherPos(false, BlockPos(x, y, z), currentBlock.blockState)

                val headBlockId = Block.getIdFromBlock(chunk.getBlock(BlockPos(x, y + 2, z)))
                if (!validEtherwarpFeetIds.get(headBlockId)) return EtherPos(false, BlockPos(x, y, z), currentBlock.blockState)

                return EtherPos(true, BlockPos(x, y, z), currentBlock.blockState)
            }

            if (x == endX && y == endY && z == endZ) return EtherPos.NONE

            when {
                tMaxX <= tMaxY && tMaxX <= tMaxZ -> {
                    tMaxX += tDeltaX
                    x += stepX
                }
                tMaxY <= tMaxZ -> {
                    tMaxY += tDeltaY
                    y += stepY
                }
                else -> {
                    tMaxZ += tDeltaZ
                    z += stepZ
                }
            }
        }

        return EtherPos.NONE
    }

    private val validEtherwarpFeetIds = BitSet(176).apply {
        arrayOf(
            Blocks.activator_rail, Blocks.air, Blocks.brown_mushroom, Blocks.carpet,
            Blocks.carrots, Blocks.cocoa, Blocks.deadbush, Blocks.double_plant, Blocks.fire,
            Blocks.flower_pot, Blocks.flowing_lava, Blocks.ladder, Blocks.lava, Blocks.lever,
            Blocks.melon_stem, Blocks.nether_wart, Blocks.piston_extension, Blocks.portal,
            Blocks.potatoes, Blocks.powered_comparator, Blocks.powered_repeater, Blocks.pumpkin_stem,
            Blocks.rail, Blocks.red_flower, Blocks.red_mushroom, Blocks.redstone_torch,
            Blocks.redstone_wire, Blocks.reeds, Blocks.sapling, Blocks.skull, Blocks.snow_layer,
            Blocks.stone_button, Blocks.tallgrass, Blocks.torch, Blocks.tripwire, Blocks.tripwire_hook,
            Blocks.unpowered_comparator, Blocks.unpowered_repeater, Blocks.vine, Blocks.water, Blocks.web,
            Blocks.wheat, Blocks.wooden_button, Blocks.yellow_flower
        ).forEach { set(Block.getIdFromBlock(it)) }
    }
}