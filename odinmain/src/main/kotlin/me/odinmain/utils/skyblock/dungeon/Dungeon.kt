package me.odinmain.utils.skyblock.dungeon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.odinmain.OdinMain.mc
import me.odinmain.OdinMain.scope
import me.odinmain.events.impl.DungeonEvents.RoomEnterEvent
import me.odinmain.events.impl.PacketReceivedEvent
import me.odinmain.features.impl.dungeon.LeapMenu
import me.odinmain.features.impl.dungeon.LeapMenu.odinSorting
import me.odinmain.features.impl.dungeon.MapInfo.shownTitle
import me.odinmain.features.impl.dungeon.Mimic
import me.odinmain.utils.*
import me.odinmain.utils.skyblock.PlayerUtils.posX
import me.odinmain.utils.skyblock.PlayerUtils.posZ
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getDungeonPuzzles
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getDungeonTeammates
import me.odinmain.utils.skyblock.dungeon.tiles.FullRoom
import net.minecraft.network.play.server.*

// could add some system to look back at previous runs.
class Dungeon(val floor: Floor) {

    var expectingBloodUpdate: Boolean = false

    var paul = false
    val inBoss: Boolean get() = getBoss()
    var dungeonTeammates: List<DungeonPlayer> = emptyList()
    var dungeonTeammatesNoSelf: List<DungeonPlayer> = emptyList()
    var leapTeammates: List<DungeonPlayer> = emptyList()
    var dungeonStats = DungeonStats()
    val currentFullRoom: FullRoom? get() = ScanUtils.currentFullRoom
    val passedRooms: MutableList<FullRoom> get() = ScanUtils.passedRooms
    var puzzles = listOf<Puzzle>()

    private fun getBoss(): Boolean {
        return when (floor.floorNumber) {
            1 -> posX > -71 && posZ > -39
            in 2..4 -> posX > -39 && posZ > -39
            in 5..6 -> posX > -39 && posZ > -7
            7 -> posX > -7 && posZ > -7
            else -> false
        }
    }

    init {
        scope.launch(Dispatchers.IO) {
            paul = hasBonusPaulScore()
        }

        shownTitle = false
    }

    fun enterDungeonRoom(event: RoomEnterEvent) {
        val roomSecrets = ScanUtils.getRoomSecrets(event.fullRoom?.room?.data?.name ?: return)
        dungeonStats.knownSecrets = dungeonStats.knownSecrets?.plus(roomSecrets) ?: roomSecrets
    }

    fun onPacket(event: PacketReceivedEvent) {
        when (event.packet) {
            is S38PacketPlayerListItem -> handleTabListPacket(event.packet)
            is S3EPacketTeams -> handleScoreboardPacket(event.packet)
            is S47PacketPlayerListHeaderFooter -> handleHeaderFooterPacket(event.packet)
            is S02PacketChat -> handleChatPacket(event.packet)
        }
    }

    fun onWorldLoad() {
        dungeonTeammates = emptyList()
        dungeonTeammatesNoSelf = emptyList()
        leapTeammates = emptyList()
        puzzles = emptyList()
        Blessing.entries.forEach { it.current = 0 }
    }

    private fun handleChatPacket(packet: S02PacketChat) {
        val message = packet.chatComponent.unformattedText.noControlCodes
        if (Regex("\\[BOSS] The Watcher: You have proven yourself. You may pass.").matches(message)) dungeonStats.bloodDone = true
        Regex("(?:\\[\\w+] )?(\\w+) opened a (?:WITHER|Blood) door!").find(message)?.let { dungeonStats.doorOpener = it.groupValues[1] }

        val partyMessage = Regex("Party > .*?: (.+)\$").find(message)?.groupValues?.get(1)?.lowercase() ?: return
        if (partyMessage.equalsOneOf("mimic killed", "mimic slain", "mimic killed!", "mimic dead", "mimic dead!", "\$skytils-dungeon-score-mimic\$", Mimic.mimicMessage))
            dungeonStats.mimicKilled = true
        if (partyMessage.equalsOneOf("blaze done!", "blaze done", "blaze puzzle solved!"))  //more completion messages may be necessary.
            puzzles.find { it.name == Puzzle.Blaze.name }.let { it?.status = PuzzleStatus.Completed }
    }

    private fun handleHeaderFooterPacket(packet: S47PacketPlayerListHeaderFooter) {
        Blessing.entries.forEach { blessing ->
            blessing.regex.find(packet.footer.unformattedText.noControlCodes)?.let { match -> blessing.current = romanToInt(match.groupValues[1]) }
        }
    }

    private fun handleScoreboardPacket(packet: S3EPacketTeams) {
        if (packet.action != 2) return

        clearedRegex.find(packet.prefix.plus(packet.suffix))?.groupValues[1]?.toIntOrNull()?.let {
            if (dungeonStats.percentCleared != it && expectingBloodUpdate) dungeonStats.bloodDone = true
            dungeonStats.percentCleared = it
        }
    }

    private fun handleTabListPacket(packet: S38PacketPlayerListItem) {
        if (!packet.action.equalsOneOf(S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, S38PacketPlayerListItem.Action.ADD_PLAYER)) return
        packet.entries.forEach { entry ->
            entry?.displayName?.formattedText?.let { dungeonStats = updateDungeonStats(it, dungeonStats) }
        }

        updateDungeonTeammates(packet.entries)
        puzzles = getDungeonPuzzles(getTabList.map { it.second }) // transfer to packet based
    }

    private val timeRegex = Regex("§r Time: §r§6((?:\\d+h ?)?(?:\\d+m ?)?\\d+s)§r")
    private val clearedRegex = Regex("^Cleared: §[c6a](\\d+)% §8(?:§8)?\\(\\d+\\)$")
    private val secretCountRegex = Regex("^§r Secrets Found: §r§b(\\d+)§r$")
    private val secretPercentRegex = Regex("^§r Secrets Found: §r§[ea]([\\d.]+)%§r$")
    private val cryptRegex = Regex("^§r Crypts: §r§6(\\d+)§r$")
    private val openedRoomsRegex = Regex("^§r Opened Rooms: §r§5(\\d+)§r$")
    private val completedRoomsRegex = Regex("^§r Completed Rooms: §r§d(\\d+)§r$")
    private val deathsRegex = Regex("^§r§a§lTeam Deaths: §r§f(\\d+)§r$")

    data class DungeonStats(
        var secretsFound: Int? = null,
        var secretsPercent: Float? = null,
        var knownSecrets: Int? = null,
        var crypts: Int? = null,
        var openedRooms: Int? = null,
        var completedRooms: Int? = null,
        var deaths: Int? = null,
        var percentCleared: Int? = null,
        var elapsedTime: String? = null,
        var mimicKilled: Boolean = false,
        var doorOpener: String? = null,
        var bloodDone: Boolean = false,
    )

    private fun updateDungeonStats(text: String, currentStats: DungeonStats): DungeonStats {
        when {
            secretCountRegex.matches(text) -> currentStats.secretsFound = secretCountRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

            secretPercentRegex.matches(text) -> currentStats.secretsPercent = secretPercentRegex.find(text)?.groupValues?.get(1)?.toFloatOrNull()

            cryptRegex.matches(text) -> currentStats.crypts = cryptRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

            openedRoomsRegex.matches(text) -> currentStats.openedRooms = openedRoomsRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

            completedRoomsRegex.matches(text) -> currentStats.completedRooms = completedRoomsRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

            deathsRegex.matches(text) -> currentStats.deaths = deathsRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

            timeRegex.matches(text) -> currentStats.elapsedTime = timeRegex.find(text)?.groupValues?.get(1)
        }

        return currentStats
    }

    private fun updateDungeonTeammates(tabList: List<S38PacketPlayerListItem.AddPlayerData>) {
        dungeonTeammates = getDungeonTeammates(dungeonTeammates.toMutableList(), tabList)
        dungeonTeammatesNoSelf = dungeonTeammates.filter { it.entity != mc.thePlayer }

        leapTeammates =
            when (LeapMenu.type) {
                0 -> odinSorting(dungeonTeammatesNoSelf.sortedBy { it.clazz.priority }).toMutableList()
                1 -> dungeonTeammatesNoSelf.sortedWith(compareBy({ it.clazz.ordinal }, { it.name })).toMutableList()
                2 -> dungeonTeammatesNoSelf.sortedBy { it.name }.toMutableList()
                else -> dungeonTeammatesNoSelf.toMutableList()
            }
    }
}