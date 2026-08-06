package com.geomgang.game

import androidx.compose.ui.Modifier
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geomgang.core.Difficulty
import com.geomgang.core.SaveStore
import com.geomgang.core.WeaponFamily
import com.geomgang.game.feel.HapticEngine
import com.geomgang.game.feel.systemVibrator
import com.geomgang.game.sound.SoundEngine
import com.geomgang.game.ui.AchievementScreen
import com.geomgang.game.ui.CodexScreen
import com.geomgang.game.ui.CraftScreen
import com.geomgang.game.ui.ForgeScreen
import com.geomgang.game.ui.GauntletScreen
import com.geomgang.game.ui.HelpScreen
import com.geomgang.game.ui.HuntScreen
import com.geomgang.game.ui.PetScreen
import com.geomgang.game.ui.QuestScreen
import com.geomgang.game.ui.RecordsMenuScreen
import com.geomgang.game.ui.SettingsScreen
import com.geomgang.game.ui.ShopScreen
import com.geomgang.game.ui.StarScreen
import com.geomgang.game.ui.StatsScreen
import com.geomgang.game.ui.StorageScreen
import com.geomgang.game.ui.TrainingScreen
import com.geomgang.game.ui.SwordForgeTheme

/** 강화 화면 위에 무엇이 올라와 있는지. */
private enum class Overlay {
    None, Hunt, Gauntlet, Storage, Shop, Craft, Training, Star,
    Records, Codex, Quests, Pets, Achievements, Stats, Help, Settings,
}

/** 뒤로 가면 어디로 돌아가는지. 하위 화면은 자기를 연 화면으로 돌아간다. */
private fun Overlay.parent(): Overlay = when (this) {
    Overlay.Codex, Overlay.Pets, Overlay.Achievements, Overlay.Stats,
    Overlay.Help, Overlay.Settings,
    -> Overlay.Records
    // 별 강화는 단련에서 들어간다. 강화 화면으로 튕기면 왔던 길을 잃는다.
    Overlay.Star -> Overlay.Training
    else -> Overlay.None
}

/**
 * 모드는 하나다.
 *
 * 쉬움·일반·지옥을 없애고 상한 없는 무한 모드만 남겼다. 덕분에 모드 선택 화면이
 * 사라져 앱을 켜면 곧바로 강화 화면이다 — "시작까지 길다"는 문제가 같이 풀렸다.
 *
 * [Difficulty] 는 지우지 않았다. 확률표에 배수를 곱하는 장치는 그대로 쓸모가 있고
 * 테스트가 그 계산을 지키고 있다. 다만 게임이 쓰는 것은 [Difficulty.ENDLESS] 하나뿐이다.
 */
private val ONLY_MODE = Difficulty.ENDLESS

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35 부터 **edge-to-edge 가 강제**다 - 앱이 상태바·내비게이션 바
        // 아래까지 그려진다. 끌 수 없으므로 명시적으로 켜 두고, 인셋은 아래에서 뺀다.
        enableEdgeToEdge()
        setContent {
            SwordForgeTheme {
                Surface {
                    // **인셋은 여기 한 곳에서만 뺀다.**
                    //
                    // 화면마다 각자 처리하면 스무 개 넘는 화면 중 하나는 반드시 빠뜨리고,
                    // 그 화면의 맨 아래 버튼이 내비게이션 바에 먹힌다. Surface 한 겹에서
                    // 빼면 모든 화면이 한 번에 안전해진다.
                    //
                    // safeDrawing 은 상태바·내비게이션 바·디스플레이 컷아웃(노치)을 함께 본다.
                    Box(Modifier.safeDrawingPadding()) {
                        App(SaveStore(filesDir))
                    }
                }
            }
        }
    }
}

@Composable
private fun App(store: SaveStore) {
    // 소리를 켤지는 ViewModel 의 설정을 그때그때 읽는다. 설정을 바꾸면 즉시 반영된다.
    val context = LocalContext.current
    val vm = remember {
        lateinit var holder: ForgeViewModel
        val engine = SoundEngine { holder.soundEnabled() }
        val feel = HapticEngine(systemVibrator(context)) { holder.hapticsEnabled() }
        holder = ForgeViewModel(store, ONLY_MODE, sound = engine, haptics = feel)
        holder
    }
    val state by vm.ui.collectAsStateWithLifecycle()
    var overlay by remember { mutableStateOf(Overlay.None) }
    // 도감은 강화 화면과 기록 메뉴 두 곳에서 열린다. 들어온 곳으로 돌아가야 한다.
    var codexOrigin by remember { mutableStateOf(Overlay.Records) }
    // 상점에서 고른 계열. **화면 밖에 둬야** 나갔다 와도 고른 것이 남는다 —
    // 상점 안에 remember 로 두면 화면을 닫는 순간 첫 계열로 되돌아갔다.
    var shopFamily by rememberSaveable { mutableStateOf(WeaponFamily.STRAIGHT.name) }

    BackHandler(enabled = !state.busy) {
        when {
            // 사냥 중이면 먼저 사냥터 목록으로, 거기서 한 번 더 누르면 강화 화면으로
            state.hunt != null -> vm.leaveHunt()
            overlay == Overlay.Codex -> overlay = codexOrigin
            overlay != Overlay.None -> overlay = overlay.parent()
            else -> Unit
        }
    }

    when (overlay) {
        Overlay.Hunt -> HuntScreen(
            state = state,
            adventure = vm.adventure(),
            onEnterGauntlet = {
                vm.leaveHunt()
                vm.enterGauntlet()
                overlay = Overlay.Gauntlet
            },
            onEnterZone = vm::enterZone,
            onTap = vm::tapTarget,
            onChallengeBoss = vm::challengeBoss,
            onTapNugget = vm::tapNugget,
            onBuyMerchant = vm::buyMerchantOffer,
            onRetryBoss = vm::retryBoss,
            onGiveUpBoss = vm::giveUpBoss,
            onStayInZone = vm::stayInZone,
            onNextZone = vm::nextZone,
            onLeave = vm::leaveHunt,
            onBack = {
                vm.leaveHunt()
                overlay = Overlay.None
            },
        )

        // 퀘스트 화면은 v2.1에서 숨겼다. Overlay.Quests 로 오는 길이 없지만
        // enum 은 남겨 둔다 - 되살릴 때 화면과 길만 다시 잇는다.
        Overlay.Quests -> QuestScreen(
            state = state,
            onClaim = vm::claimQuest,
            onBack = { overlay = Overlay.None },
        )

        Overlay.Gauntlet -> GauntletScreen(
            state = state,
            onTap = vm::tapGauntlet,
            onChoose = vm::chooseGauntlet,
            onLeave = {
                vm.leaveGauntlet()
                overlay = Overlay.None
            },
        )

        Overlay.Pets -> PetScreen(
            state = state,
            onEquip = vm::equipPet,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.Storage -> StorageScreen(
            state = state,
            onStore = vm::storeSword,
            onEquip = vm::equipFromStorage,
            onSell = vm::sellFromStorage,
            onScrap = vm::scrapFromStorage,
            onOffer = vm::offerFromStorage,
            onBack = { overlay = Overlay.None },
        )

        Overlay.Shop -> ShopScreen(
            state = state,
            family = WeaponFamily.entries.firstOrNull { it.name == shopFamily }
                ?: state.unlockedFamilies.first(),
            onSelectFamily = { shopFamily = it.name },
            onBuySword = vm::buySword,
            onBuySwordToStorage = vm::buySwordToStorage,
            onBuyStone = vm::buyStone,
            onSellSword = vm::sellSword,
            onBuyItem = vm::buyItem,
            onCraft = { id, count, family -> vm.craft(id, count, family) },
            onBack = { overlay = Overlay.None },
        )

        Overlay.Training -> TrainingScreen(
            state = state,
            onUpgradeSkill = vm::upgradeSkill,
            onOpenStar = { overlay = Overlay.Star },
            onBack = { overlay = Overlay.None },
        )

        Overlay.Star -> StarScreen(
            state = state,
            onStarUp = vm::starUp,
            onBack = { overlay = Overlay.Training },
        )

        Overlay.Craft -> CraftScreen(
            state = state,
            onFuse = vm::fuse,
            onRefine = vm::refine,
            onCraftLegend = vm::craftLegend,
            onRecraftLegend = vm::recraftLegend,
            onBuyWard = vm::buyWardCharm,
            onBack = { overlay = Overlay.None },
        )

        Overlay.Records -> RecordsMenuScreen(
            progress = state.progress,
            ownedPets = state.progress.petsFound.size,
            onOpenCodex = {
                codexOrigin = Overlay.Records
                overlay = Overlay.Codex
            },
            onOpenPets = { overlay = Overlay.Pets },
            onOpenAchievements = { overlay = Overlay.Achievements },
            onOpenHelp = { overlay = Overlay.Help },
            onOpenStats = { overlay = Overlay.Stats },
            onOpenSettings = { overlay = Overlay.Settings },
            onBack = { overlay = Overlay.None },
        )

        Overlay.Codex -> CodexScreen(
            progress = state.progress,
            onBack = { overlay = codexOrigin },
        )

        Overlay.Achievements -> AchievementScreen(
            progress = state.progress,
            onSelectTitle = vm::selectTitle,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.Stats -> StatsScreen(
            difficulty = state.difficulty,
            progress = state.progress,
            deepUnlocked = state.deepUnlocked,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.Help -> HelpScreen(onBack = { overlay = Overlay.Records })

        Overlay.Settings -> SettingsScreen(
            settings = state.settings,
            onAutoPreventChange = vm::setAutoPrevent,
            onSoundChange = vm::setSoundOn,
            onHapticsChange = vm::setHapticsOn,
            onReset = vm::resetProgress,
            onBack = { overlay = Overlay.Records },
        )

        Overlay.None -> ForgeScreen(
            state = state,
            onForge = vm::forge,
            onPrevent = vm::usePrevent,
            onSalvage = vm::salvage,
            onToggleBlessing = vm::toggleBlessing,
            onToggleLuckCharm = vm::toggleLuckCharm,
            onOpenHunt = { overlay = Overlay.Hunt },
            onOpenStorage = { overlay = Overlay.Storage },
            onOpenShop = { overlay = Overlay.Shop },
            onOpenCraft = { overlay = Overlay.Craft },
            onOpenCodex = {
                codexOrigin = Overlay.None
                overlay = Overlay.Codex
            },
            onOpenMenu = { overlay = Overlay.Records },
            onDismissIdle = vm::dismissIdleReward,
            onOpenTraining = { overlay = Overlay.Training },
            onAnimationEnd = vm::onAnimationFinished,
        )
    }
}
