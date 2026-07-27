# Sword sheet 2 composer. ASCII-only source (PS 5.1 reads scripts as ANSI).
#
# Cells contract (SwordSheet2.kt trusts this):
#   rows 0..13 = WeaponFamily declaration order, cols 0..10 = WeaponTier order (rusty..nameless)
#   row 12 = FUSED  : recolor of straight row (prismatic channel rotation)
#   row 13 = VOID   : recolor of straight row (dark violet)
#   row 14 = uniques: UniqueSwords.RECIPES declaration order, cols 0..9
# Source folders are numbered 01_..12_ and matched by number prefix, so no
# Korean literals are needed here.
# Usage: .\make_sword_sheet2.ps1 -SrcRoot "C:\...\collection" -OutFile ...\sword_sheet2.png
param(
    [Parameter(Mandatory)][string]$SrcRoot,
    [Parameter(Mandatory)][string]$OutFile
)
Add-Type -AssemblyName System.Drawing

$cell = 64; $cols = 11; $rows = 15
$tiers = @("rusty","steel","silver","rune","flame","thunder","dawn","black_dragon","dragon_scale","abyss","nameless")
$familyIds = @("straight","curved","great","rapier","twin","demon","holy","dragon","scythe","axe","spear","spirit")

$dirs = Get-ChildItem $SrcRoot -Directory | Sort-Object Name
if ($dirs.Count -lt 12) { throw "expected 12 family folders, found $($dirs.Count)" }

function Dir-Of([int]$index1) {
    return ($dirs | Where-Object { $_.Name -like ("{0:00}_*" -f $index1) } | Select-Object -First 1).FullName
}

# Unique picks - must match UniqueSwords.RECIPES declaration order:
# abyss_eater, trinity, dragon_fang, phoenix, cleaver, tempest, lucky, origin, glutton, bloom
$uniquePicks = @(
    @(1,  "0029*sword_of_cerebov_02*.png"),
    @(1,  "0036*urand_jihad*.png"),
    @(8,  "0014*urand_wyrmbane*.png"),
    @(1,  "0026*fire_brand*.png"),
    @(10, "0025*executioner_axe*.png"),
    @(1,  "0050*w_sword_roman_lightning*.png"),
    @(1,  "0021*singing_sword*.png"),
    @(1,  "0053*ancient_sword*.png"),
    @(1,  "0054*urand_leech*.png"),
    @(1,  "0038*poison_brand*.png")
)

function Find-Curated([int]$folderIndex, [string]$family, [string]$tier) {
    $dir = Dir-Of $folderIndex
    $hit = Get-ChildItem $dir -Filter "*__${family}__${tier}__*.png" | Select-Object -First 1
    if ($null -eq $hit) { throw "curated missing: $family/$tier" }
    return $hit.FullName
}

function New-Attrs([float[][]]$m) {
    $cm = New-Object System.Drawing.Imaging.ColorMatrix
    for ($i = 0; $i -lt 5; $i++) {
        for ($j = 0; $j -lt 5; $j++) {
            $cm.($("Matrix{0}{1}" -f $i, $j)) = $m[$i][$j]
        }
    }
    $attrs = New-Object System.Drawing.Imaging.ImageAttributes
    $attrs.SetColorMatrix($cm)
    return $attrs
}

# Tier grading. Some families have few distinct source tiles (dragon has ONE for
# all 11 tiers, rapier has 3). Without grading those rows show no progression at
# all. Blend 35% original color with 65% luminance-tinted tier color.
$tierRgb = @(
    @(0.62, 0.56, 0.45),  # rusty
    @(0.75, 0.79, 0.84),  # steel
    @(0.88, 0.92, 0.98),  # silver
    @(0.56, 0.82, 0.90),  # rune
    @(0.95, 0.55, 0.28),  # flame
    @(0.94, 0.86, 0.34),  # thunder
    @(1.00, 0.97, 0.85),  # dawn
    @(0.62, 0.35, 0.90),  # black_dragon
    @(0.92, 0.35, 0.55),  # dragon_scale
    @(0.35, 0.42, 0.95),  # abyss
    @(1.00, 1.00, 1.00)   # nameless
)
function New-TierAttrs([int]$t) {
    $c = $tierRgb[$t]
    $keep = 0.35; $tint = 0.65
    # Luminance weights per input channel, spread onto the tier color.
    $lw = @(0.299, 0.587, 0.114)
    $m = @()
    for ($i = 0; $i -lt 3; $i++) {
        $row = @()
        for ($j = 0; $j -lt 3; $j++) {
            $v = $lw[$i] * $c[$j] * $tint
            if ($i -eq $j) { $v += $keep }
            $row += [float]$v
        }
        $m += , ($row + @([float]0.0, [float]0.0))
    }
    $m += , @([float]0.0, [float]0.0, [float]0.0, [float]1.0, [float]0.0)
    $m += , @([float]0.0, [float]0.0, [float]0.0, [float]0.0, [float]1.0)
    return New-Attrs $m
}
$tierAttrs = @()
for ($t = 0; $t -lt $tiers.Count; $t++) { $tierAttrs += (New-TierAttrs $t) }

$fusedAttrs = New-Attrs @(
    @(0.4, 0.0, 0.6, 0.0, 0.0),
    @(0.6, 0.4, 0.0, 0.0, 0.0),
    @(0.0, 0.6, 0.4, 0.0, 0.0),
    @(0.0, 0.0, 0.0, 1.0, 0.0),
    @(0.0, 0.0, 0.0, 0.0, 1.0)
)
$voidAttrs = New-Attrs @(
    @(0.165, 0.105, 0.225, 0.0, 0.0),
    @(0.330, 0.210, 0.450, 0.0, 0.0),
    @(0.055, 0.035, 0.075, 0.0, 0.0),
    @(0.0,   0.0,   0.0,   1.0, 0.0),
    @(0.0,   0.0,   0.0,   0.0, 1.0)
)

$sheet = [System.Drawing.Bitmap]::new($cell * $cols, $cell * $rows)
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

# Content size normalization: source tiles use wildly different portions of
# their canvas (a dagger is tiny, a greatsword fills the tile). Trim to the
# opaque bounding box and scale every sword so its longest side is TARGET px,
# centered in the cell. This is what makes all swords look the same size.
$TARGET = 58

function Get-Bounds([System.Drawing.Bitmap]$bmp) {
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $fmt = [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, $fmt)
    $bytes = New-Object byte[] ($data.Stride * $bmp.Height)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    $bmp.UnlockBits($data)
    $minX = $bmp.Width; $minY = $bmp.Height; $maxX = -1; $maxY = -1
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        $rowOff = $y * $data.Stride
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            if ($bytes[$rowOff + $x * 4 + 3] -gt 16) {
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    if ($maxX -lt 0) { return $null }
    return @($minX, $minY, ($maxX - $minX + 1), ($maxY - $minY + 1))
}

function Draw-Cell([string]$path, [int]$col, [int]$row, $attrs) {
    $src = [System.Drawing.Bitmap]::FromFile($path)
    $b = Get-Bounds $src
    if ($null -eq $b) { $src.Dispose(); throw "empty tile: $path" }
    $bx = $b[0]; $by = $b[1]; $bw = $b[2]; $bh = $b[3]
    # Integer-snap scaling: non-integer nearest-neighbor upscales make ragged,
    # uneven pixels. Upscales use whole multiples (2x, 3x, ...) capped at the
    # cell; downscales use the exact fit (shrinking blends fine).
    $maxDim = [Math]::Max($bw, $bh)
    if ($maxDim -lt $TARGET) {
        $k = [Math]::Floor(($cell - 2) / $maxDim)
        if ($k -lt 1) { $k = 1 }
        $scale = [double]$k
    } else {
        $scale = $TARGET / [double]$maxDim
    }
    $dw = [int][Math]::Round($bw * $scale)
    $dh = [int][Math]::Round($bh * $scale)
    $dx = $col * $cell + [int](($cell - $dw) / 2)
    $dy = $row * $cell + [int](($cell - $dh) / 2)
    $dest = New-Object System.Drawing.Rectangle($dx, $dy, $dw, $dh)
    if ($null -eq $attrs) {
        $g.DrawImage($src, $dest, $bx, $by, $bw, $bh, [System.Drawing.GraphicsUnit]::Pixel)
    } else {
        $g.DrawImage($src, $dest, $bx, $by, $bw, $bh,
            [System.Drawing.GraphicsUnit]::Pixel, $attrs)
    }
    $src.Dispose()
}

for ($f = 0; $f -lt $familyIds.Count; $f++) {
    $paths = @()
    for ($t = 0; $t -lt $tiers.Count; $t++) {
        $paths += (Find-Curated ($f + 1) $familyIds[$f] $tiers[$t])
    }
    # If the family reuses tiles across tiers, grade them so tiers still differ.
    # Compare by TILE NAME, not path: curated names embed the tile name in the
    # 5th "__" segment and end with a per-file hash, so identical tiles have
    # different filenames (dragon reuses one tile for all 11 tiers).
    $tileNames = $paths | ForEach-Object {
        $parts = (Split-Path $_ -Leaf) -split "__"
        if ($parts.Count -ge 5) { $parts[4] } else { $_ }
    }
    $distinct = ($tileNames | Sort-Object -Unique).Count
    $needsGrading = $distinct -lt $tiers.Count
    for ($t = 0; $t -lt $tiers.Count; $t++) {
        $attrs = $null
        if ($needsGrading) { $attrs = $tierAttrs[$t] }
        Draw-Cell $paths[$t] $t $f $attrs
    }
    if ($needsGrading) {
        Write-Host ("graded row {0} ({1}): only {2} distinct tiles" -f $f, $familyIds[$f], $distinct)
    }
}
for ($t = 0; $t -lt $tiers.Count; $t++) {
    $straight = Find-Curated 1 "straight" $tiers[$t]
    Draw-Cell $straight $t 12 $fusedAttrs
    Draw-Cell $straight $t 13 $voidAttrs
}
for ($u = 0; $u -lt $uniquePicks.Count; $u++) {
    $dir = Dir-Of $uniquePicks[$u][0]
    $hit = Get-ChildItem $dir -Filter $uniquePicks[$u][1] | Select-Object -First 1
    if ($null -eq $hit) { throw "unique missing: $($uniquePicks[$u][1])" }
    Draw-Cell $hit.FullName $u 14 $null
}

$g.Dispose()
$sheet.Save($OutFile, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Host "sword_sheet2: $($cell*$cols)x$($cell*$rows) -> $OutFile"
