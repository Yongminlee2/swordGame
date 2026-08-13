# SwordForge UI Design QA

## Visual truth and test state

- Reference: `C:/workAndroid/SwordForge/art-review/ui-reference/selected-design-1.png` (853 x 1844)
- Implementation: `C:/workAndroid/SwordForge/build/visual-qa/final-redesign-20260810/13-main-season2-final.png` (1080 x 2340, 450 dpi)
- Comparison: `C:/workAndroid/SwordForge/build/visual-qa/final-redesign-20260810/16-design-comparison.png`
- Focus comparison: `C:/workAndroid/SwordForge/build/visual-qa/final-redesign-20260810/17-design-comparison-focus.png`
- State: Season II, dragon sword +16, 8,651만 gold, 30 shards, 25 wards. Enhancement stones are live save data (48 in the implementation, 24 in the reference).

The implementation capture was normalized to the reference aspect and inspected side by side. The header hierarchy, resource strip, sword stage, probability panel, costs, toggles, primary enhancement action, hunt strip, and bottom navigation all match the selected direction. The new sword art and live resource values are intentional deviations.

## Interaction and layout verification

- Main forge screen remains a fixed, non-scrolling game screen.
- Bottom navigation and menu routes were exercised on the connected SM-A165N device.
- Long menu bodies scroll independently while title, season badge, back action, and wallet remain fixed.
- Shop fixed-header evidence:
  - `build/visual-qa/final-redesign-20260810/14-shop-fixed-top.png`
  - `build/visual-qa/final-redesign-20260810/15-shop-fixed-scroll.png`
- Sword codex and adjacent-level sprite changes were checked with live app renders.
- Final chroma-clean legend sprite evidence:
  - `build/visual-qa/final-redesign-20260810/19-legend-clean.png`
- Browser and console checks are not applicable to this native Android build. Gradle unit tests, APK assembly, ADB installation, and device rendering were used instead.

## Iteration history

1. Per-level sword art reused five base silhouettes. Replaced with 177 non-empty, edge-safe, byte-unique sprites so every enhancement level has a distinct source silhouette.
2. Generated sword edges retained green chroma spill. Updated the production generator to remap residual green to the cyan family palette and regenerated the sheets.
3. Long menu screens scrolled the exit route and resource header away. Introduced a shared fixed-header/scrolling-body layout and verified it before and after a device swipe.
4. Empty storage did not explain the next action. Added season-aware shop/hunt calls to action.

## Findings

- P0: none
- P1: none
- P2: none after fixes
- P3 accepted: family-specific ornament density varies intentionally to preserve the rough 2D B-grade RPG identity; debug-only adjacent-level previews dim neighboring silhouettes by design.

## 2026-08-13 sword source-fidelity pass

- Source visual truth: `art-review/sword-evolution-v2/raw-generated/curved-grid.png` (1536 x 1024), supplied +20 curved sword in grid cell 20.
- Implementation screenshot: `build/visual-qa/original-quality-swords-20260813/06-curved-native-final.png` (1080 x 2340, SM-A165N at 450 dpi).
- Full rendered state: debug-only sword preview, CURVED +20, 340 dp stress-test render. Production forge uses 184-190 dp and therefore scales the source less.
- Focus comparison: `build/visual-qa/original-quality-swords-20260813/07-curved-source-vs-device.png` (1440 x 1120). The exact source cell and Android render are normalized side by side.
- Additional implementation states:
  - Season I main: `build/visual-qa/original-quality-swords-20260813/03-main-original-quality-final.png`
  - Legend +30: `build/visual-qa/original-quality-swords-20260813/05-legend-original-quality.png`

### Fidelity findings

- Fonts and typography: unchanged and outside the asset-only request; no new wrapping or hierarchy regression.
- Spacing and layout rhythm: sword bounds remain inside both the forge slot and preview slot without clipping.
- Colors and visual tokens: the source blade, metal, leather, gem, and gold colours remain intact. Only the flat green key background is removed; no cyan replacement contour remains.
- Image quality and asset fidelity: the former 58 px logical reduction, 48-colour quantization, binary alpha, and nearest-neighbour 2x enlargement were removed. All 177 images now retain their native cropped source detail, use antialiased transparency, and render with high-quality filtering. Family sheets are separated so the higher fidelity does not require decoding every family on the main screen.
- Copy and content: unchanged.

### Comparison history

1. P1: every sword was deliberately reduced to a chunky pixel grid, causing visibly broken diagonals and loss of shading. Fixed by removing palette quantization, binary alpha, logical downscaling, and nearest-neighbour enlargement.
2. P2: the first high-resolution pass inherited a cyan edge from the earlier green-spill workaround. Fixed by neutralizing only excess green while preserving the source red/blue edge values.
3. P2: a single combined high-resolution atlas raised unnecessary runtime memory cost. Fixed by splitting the seven 21-level families into separate native-detail sheets; the displayed family is loaded independently.
4. Post-fix evidence: the +20 curved sword's outline, highlights, gem, guard, grip, and proportions match the supplied source cell. Season I and legend renders are also complete and unclipped.

- Remaining P0/P1/P2: none.
- Accepted P3: the source illustrations themselves use a retro pixel-art outline; that intrinsic source style is intentionally preserved rather than smoothed into newly invented artwork.

final result: passed

## 2026-08-14 three-season and legend-protection pass

- Source visual truth: the established selected Design 1 UI in `C:/workAndroid/SwordForge/art-review/ui-reference/selected-design-1.png` and the current fixed-height forge/shop components.
- Physical implementation evidence: `C:/workAndroid/SwordForge/art-review/season3-qa/season1-forge.png`, `season3-forge.png`, `season3-shop-top.png`, and `purchase-state.png` (1080 x 2400 S20 Ultra screenshots).
- State: physical SM-G988N, Korean locale, 420 dpi, three-button navigation visible. The player's live save was backed up before temporary visual/purchase states and restored after testing.

### Fidelity and interaction findings

1. P1: the interface only distinguished Seasons I and II, so Dragon +20 had no visible progression boundary. Added `I 불씨의 시대`, `II 심연의 시대`, and `III 전설의 시대`, with matching smithy names and a persistent Dragon +20 Season III marker. An old save holding Dragon +20 is repaired on launch.
2. P1: sword skills were visible during Season I even though hunting and combat do not exist there. The Season I S20 screenshot shows the sword subtitle flowing directly into the outcome row with no skill label, lock icon, or dead combat promise.
3. P1: the requested Season III protection item had no economic or recovery path. Added a dedicated legend-destruction ticket card with two live purchase controls: 10,000,000 gold or 10,000 shards for one ticket. Physical-device taps verified each currency decreased independently and ticket count increased from 0 to 1 to 2.
4. P1: refined/legend swords survive a destroy result at a lower floor, which made the ordinary "revive a missing sword" ticket unsuitable. The new ticket restores the exact pending Dragon level and preserves stars/unique metadata; ordinary tickets remain limited to fully destroyed ordinary swords.
5. P2: the new Season III card could have introduced button wrapping or navigation overlap. The 1080 x 2400 shop capture shows both prices on one line, equal-width controls, complete panel borders, usable scrolling, and no overlap with the Android navigation bar.

- Automated checks: all debug unit tests and `assembleDebug` passed, including season-boundary, two-currency purchase, ticket separation, and exact recovery tests.
- Runtime checks: the APK installed successfully on SM-G988N; Season I, Season III forge, Season III shop, and both purchase actions were exercised. Android runtime log contains no crash.
- Save restoration: final live state retains Dragon +20, 2,400 shards, 9,869 stones, ordinary inventory, storage, quests, pets, adventure clears, and the new permanent Season III marker. Only the app's intentional idle gold/time advanced after reopening.
- Remaining P0/P1/P2: none.

final result: passed

## 2026-08-14 boss flow, monster edge, bottom menu, and collection pass

- Source visual truth: the eight approved transparent monster atlases in `C:/workAndroid/SwordForge/art-review/combat-redesign/transparent-source/` and the established 76 dp bottom-menu component in `ForgeScreen.kt`.
- Physical implementation evidence: `C:/workAndroid/SwordForge/build/visual-qa/final-pass/boss-no-effect.png`, `boss-failed.png`, `main-menu-clear.png`, `codex-full.png`, and `unique-full.png` (1080 x 2400 S20 Ultra screenshots).
- Combined comparison evidence: `bottom-menu-before-after.png` compares the same bottom-menu region before and after vertical centering; `all-144-production-monsters-fixed.png` contains every rebuilt production monster on the same dark background.
- State: physical SM-G988N, Korean locale, 420 dpi, three-button navigation visible. Boss and normal-monster debug previews are deterministic and never read or write player save data.

### Fidelity and interaction findings

1. P1: thin opaque white source-grid strokes survived at the right/bottom edges of several production cells; some bosses also had only 8 px atlas margin. Fixed by clearing a four-pixel perimeter before component isolation, refitting all 144 subjects, and enforcing at least 17 transparent pixels on every production-cell edge. Automated audit reports no unsafe bounds and no pale frame pixels.
2. P1: a previous normal-monster hit could remain visible at the boss gate or the full illustrated hit overlay could cover the active boss. Fixed by suppressing the overlay for boss-ready, boss-active, failed, and cleared states and by disabling boss sprite hit-flash sequencing. The S20 boss capture contains only the boss, HUD, HP, and timer surfaces.
3. P1: boss failure offered a progressively priced immediate retry. Removed the paid action and its state/domain implementation. The modal now has one `확인` action which returns to the hunt list without charging gold.
4. P2: icon/label groups were top-heavy inside their 76 dp navigation cells. Fixed with vertical center arrangement. The same-viewport before/after comparison shows balanced top and bottom space, while the Android navigation bar remains unobstructed.
5. Device-only collection setup: all seven regular family slots through +20 are present (144/144 applicable family entries) and all six unique swords are found. Legend +21..+50 remains intentionally unfilled because it is outside the requested `모든 검 +20` range. Screenshots show `직검 21 / 21`, the other family sections, and `고유검 6 / 6`.

- Automated checks: combat asset generation, 144-cell alpha/frame validation, `testDebugUnitTest`, and `assembleDebug` passed.
- Runtime checks: the final APK installed successfully on SM-G988N; app restart preserved 144 codex entries, six unique ids, the existing Dragon +20, gold, stones, storage, quests, pets, and adventure progress. Android runtime log contains no crash.
- Remaining P0/P1/P2: none.

final result: passed

## 2026-08-14 sword tip and pommel clipping pass

- Source visual truth: the seven approved evolution grids in `C:/workAndroid/SwordForge/art-review/sword-evolution-v2/raw-generated/` (Straight/Curved/Great/Holy 1536 x 1024, Rapier 1566 x 1004, Demon/Dragon 1619 x 971).
- Implementation screenshots: `C:/workAndroid/SwordForge/build/visual-qa/sword-edge-fix/final-GREAT-20.png`, `final-DRAGON-20.png`, and `final-CURVED-20.png` (1080 x 2400 physical screenshots).
- Combined focused comparison: `C:/workAndroid/SwordForge/build/visual-qa/sword-edge-fix/source-vs-s20-dragon20.png` (1600 x 1160), showing the isolated source silhouette and its actual S20 Ultra 340 dp preview render together.
- State: physical SM-G988N (S20 Ultra), Korean locale, 420 dpi, deliberately oversized 340 dp debug preview. Suspect source-boundary levels were checked across all seven families: Straight +6, Curved +13/+20, Great +20, Rapier +6, Demon +20, Holy +18, and Dragon +20.

### Fidelity surfaces

- Image bounds: blade tips, guards, grips, and pommels are wholly visible in the oversized preview. Production cells reserve at least 12 source pixels of transparent atlas margin on every side.
- Aspect and detail: each sword is fitted proportionally inside a 328 x 328 production cell with a 300 px maximum art extent and Lanczos premultiplied-alpha resizing; no stretch, quantization, or nearest-neighbour enlargement is applied.
- Layout resilience: the test preview is substantially larger than the 184-190 dp production forge slot, so its intact bounds cover the more constrained live placement.
- Colors and transparency: the approved metal, leather, gem, and fire colours are preserved. Whole-sheet key removal occurs before component isolation, and a 5 px source pad retains antialiased edge pixels.

### Comparison history

1. P1: fixed mathematical grid rectangles were cropped before transparent padding was added. Several approved illustrations cross those ideal rectangles, so valid blade-tip or pommel pixels were irreversibly amputated even though the final atlas cell appeared to have space. Fixed by locating all substantial connected sword silhouettes on the full keyed source sheet, assigning them to their nearest visual grid slot, and cropping each complete component with source padding.
2. P2: large ornate silhouettes had only a small final-cell reserve. Fixed by limiting art extent to 300 px inside each 328 px cell and validating a strict 12 px minimum on all four sides.
3. Automated post-fix validation: all 177 production sprites are non-empty, byte-unique, native-detail, and edge-safe. Any future sprite that violates the margin now fails the asset build.
4. Physical-device evidence: source Dragon +20 and the S20 340 dp render retain the same complete dragon head, blade arc, guard wings, grip, and pointed pommel. The other boundary-heavy family examples are likewise intact.

- Runtime checks: `testDebugUnitTest` and `assembleDebug` passed; the rebuilt APK installed successfully on the connected S20 Ultra and produced no Android runtime crash.
- Remaining P0/P1/P2: none.

final result: passed

## 2026-08-14 family names, copy density, and navigation-safe-area pass

- Source visual truth: `C:/workAndroid/SwordForge/art-review/ui-reference/selected-design-1.png` (853 x 1844) plus the seven source evolution grids in `art-review/sword-evolution-v2/raw-generated/` (1536 x 1024 each).
- Implementation screenshot: `C:/workAndroid/SwordForge/build/visual-qa/SM-G988N-dragon1-main.png` (1080 x 2400 physical screenshot; app content 1080 x 2200 above a 200 px Android three-button navigation bar).
- Additional evidence: `SM-G988N-shop.png`, `SM-G988N-craft.png`, `SM-G988N-storage.png`, `SM-G988N-training.png`, `SM-G988N-codex.png`, `SM-G988N-final-hunt-2.png`, and `SM-G988N-final-records.png` in the same folder.
- State: physical SM-G988N (S20 Ultra), Season II, Dragon +1, Korean locale, 420 dpi physical/1080 x 2400 override, three-button system navigation visible.
- Density normalization: source and implementation were compared by visible composition and content region; the source aspect differs from the native device. Focused sword matching used the source Dragon grid cell for +1 and the device-rendered +1 sprite.

### Full-view and focused comparison evidence

- Full view: the implementation preserves the selected design's fixed forge hierarchy and dark teal B-grade 2D treatment while ending all persistent game controls at y=2200. Android Back/Home/Recents occupy y=2200..2400 with no overlap.
- Focused region: the displayed +1 dragon illustration is the second Dragon source cell and is named `어린 용의 이빨`; the small red-scale blade shape and name agree.
- Fonts and typography: main sword name remains the strongest content label; effect descriptions are now compact numeric phrases and do not wrap on the tested device.
- Spacing and layout rhythm: main forge remains non-scrolling; persistent navigation and all long-screen scroll bodies respect the global navigation inset.
- Colors and tokens: Season II cyan, dark panels, amber action, semantic green/red states, and low-contrast secondary copy remain consistent.
- Image quality and asset fidelity: native-detail sword sprites remain unchanged; no placeholder, CSS/SVG approximation, or extra generated asset was introduced.
- Copy and content: seven visible families now have 147 distinct +0..+20 names. Skills, forge traits, combat traits, shop, craft, main forge, and selected menu descriptions were shortened.

### Comparison history

1. P1: Android Back/Home/Recents could transiently cover the bottom game navigation. Fixed by keeping the navigation bar visible, applying a global `navigationBarsPadding`, and reserving 6 dp breathing room above it.
2. P2: all families shared the same level-only sword name. Fixed with source-art-aware per-family name tables for the seven visible families and unique fallback naming for every hidden family.
3. P2: skill, trait, shop, craft, and forge guidance was too sentence-heavy. Fixed by reducing those strings to action labels and numeric effects; S20 screenshots show no new wrapping or clipping.
4. Post-fix evidence: main, shop, craft, storage, training, codex, hunt, and records were opened on the connected S20 Ultra. Back navigation worked, scrollable menu content remained usable, and Android runtime logs contained no app crash.

- Remaining P0/P1/P2: none.
- Accepted P3: hidden recipe hints remain sentence-like because their wording is the discovery mechanic, not routine helper copy.

final result: passed

## 2026-08-13 combat illustration and impact-feedback pass

- Source visual truth: `C:/workAndroid/SwordForge/art-review/combat-redesign/selected-skill-direction.png` (853 x 1844).
- Implementation screenshot: `C:/workAndroid/SwordForge/build/visual-qa/combat-redesign-20260813-18-skill-final.png` (1080 x 2340, SM-A165N at 450 dpi).
- Full-view normalized comparison: `C:/workAndroid/SwordForge/build/visual-qa/combat-redesign-reference-vs-implementation.png` (1706 x 1844). Both sides were normalized to 853 x 1844; the Android status/navigation bars and the debug-only compact header are expected framing differences.
- State: Season II, Dragon +20, meadow boss, Dragon Breath, 23,584 damage.
- Focus comparison: the full-view comparison is sufficient because the monster edge, dragon flame edge, cyan secondary ring, banner ornament, and outlined damage digits remain clearly readable at the normalized size.

### Fidelity surfaces

- Fonts and typography: the skill banner uses the selected mock's centered heavy Korean display hierarchy; orange outlined damage is legible over both dark fur and bright fire. The live game continues to use its existing compact sans-serif HUD system.
- Spacing and layout rhythm: banner, monster, effect, damage, and HP bar form the same top-to-bottom order as the source. The effect stays inside the battle panel and leaves the HP bar readable.
- Colors and visual tokens: crimson/black/gold banner, red-orange flame, cyan secondary ring, and orange damage map directly to the selected direction. Existing early/deep arena tokens remain intact.
- Image quality and asset fidelity: 144 combat monsters are routed through 24 high-resolution zone atlases and rendered with high-quality filtering. Sixteen real illustrated VFX assets replace text-only impacts. Chroma spill was removed without deleting the cyan ring. No code-drawn or placeholder monster/effect asset is used.
- Copy and content: dynamic skill names, damage, hit multiplier, critical label, and kill gold remain driven by the real combat state.
- Accessibility and resilience: outlined text keeps contrast over the brightest effect core; the fixed battle panel retains the existing tap target and does not introduce page scrolling.

### Comparison history

1. P1: the original 32 px monster sheet became visibly blocky at 148-184 dp. Fixed with 24 zone atlases covering 5 regular monsters and one boss per zone, plus high-quality rendering.
2. P1: normal, critical, and skill hits were text-only and had no visual distinction. Fixed with separate normal/critical art plus one illustrated asset for each of the 14 skill ids.
3. P2: generated keyed VFX retained small green chroma reflections. Fixed with a green-dominance matte pass while retaining yellow flame and cyan energy.
4. P2: a transient animation could miss the first rendered hit state on preview entry. Fixed by rendering the current hit snapshot immediately and using a timed visibility window keyed by `hitSeq`.
5. Post-fix evidence: the device implementation reproduces the selected banner, red dragon arc, cyan secondary trajectory, outlined damage, sharp monster illustration, and unobstructed HP bar. No actionable layout or image-quality mismatch remains.

- Primary interactions tested: zone list entry, boss-ready state, combat artwork render, Dragon Breath feedback, APK reinstall, process restart, and user-save restoration. The restored save retained sword family/level, best level, price band, zone, and kills; only the app's intentional idle-reward/time fields advanced after launch.
- Runtime errors checked: Android runtime log contained no crash after the final build; 24 monster atlases and 16 effect assets passed size/alpha validation.
- Remaining P0/P1/P2: none.
- Accepted P3: the debug comparison uses the existing red boss arena while the selected mock shows a teal normal arena; production retains the correct zone/boss semantic color.

final result: passed
