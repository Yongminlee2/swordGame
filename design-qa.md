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

final result: passed
