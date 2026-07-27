# Sword sheet 3 composer - one distinct image per enhancement level.
# ASCII-only source (PS 5.1 reads scripts as ANSI).
#
# Cells contract (SwordSheet3.kt trusts this):
#   64px cells, 21 columns x 15 rows.
#   rows 0..13 = WeaponFamily declaration order
#   cols 0..20 = enhancement level +0..+20 (one distinct tile each)
#   row 14     = legendary tiles for +21..+40 (cols 0..19)
#
# Every cell gets a light tier grading (55% original + 45% tier color) so the
# level progression reads as a color arc (rusty brown -> silver -> flame ->
# abyss violet -> nameless white) on top of per-level shape variety.
# Legendary row is NOT graded - those should look distinct and special.
#
# Source safety: only CC0 Dungeon Crawl derived tiles are used. A file counts
# as safe when one of its "__" segments looks like an ASCII tile name and is
# not a bare 8-char hex hash. Ripped commercial-game icons in the collection
# carry Korean-only item names with no ASCII tile segment, so they drop out.
#
# Usage: .\make_sword_sheet3.ps1 -SrcRoot "C:\...\collection" -OutFile ...\sword_sheet3.png
param(
    [Parameter(Mandatory)][string]$SrcRoot,
    [Parameter(Mandatory)][string]$OutFile
)
Add-Type -AssemblyName System.Drawing

$cell = 64
$levels = 21
$familyRows = 14
$rows = 15
$legendaryRow = 14
$legendaryCount = 20
$TARGET = 58

# Row order must match WeaponFamily declaration order.
# fused/void have no folder - they reuse straight tiles with a channel recolor.
$familyFolders = @(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)

$idWords = @(
    'straight', 'curved', 'great', 'rapier', 'twin', 'demon', 'holy', 'dragon',
    'scythe', 'axe', 'spear', 'spirit',
    'rusty', 'steel', 'silver', 'rune', 'flame', 'thunder', 'dawn',
    'black_dragon', 'dragon_scale', 'abyss', 'nameless'
)

$dirs = Get-ChildItem $SrcRoot -Directory | Sort-Object Name
if ($dirs.Count -lt 12) { throw "expected 12 family folders, found $($dirs.Count)" }

function Dir-Of([int]$index1) {
    return ($dirs | Where-Object { $_.Name -like ("{0:00}_*" -f $index1) } | Select-Object -First 1).FullName
}

# A file is usable when it has an ASCII tile-name segment that is not a hash.
function Is-Safe([string]$baseName) {
    foreach ($p in ($baseName -split '__')) {
        if ($p -match '^[A-Za-z][A-Za-z0-9_ ]{2,}$' -and
            $p -notmatch '^[0-9a-f]{8}$' -and
            ($idWords -notcontains $p)) {
            return $true
        }
    }
    return $false
}

function Pool-Of([int]$folderIndex) {
    $dir = Dir-Of $folderIndex
    $files = Get-ChildItem $dir -Filter *.png |
        Where-Object { Is-Safe $_.BaseName } |
        Sort-Object Name
    return @($files | ForEach-Object { $_.FullName })
}

# Evenly spaced picks so we sample the whole pool instead of the first N.
function Pick-Spread([string[]]$pool, [int]$count) {
    if ($pool.Count -eq 0) { throw "empty pool" }
    $out = @()
    for ($i = 0; $i -lt $count; $i++) {
        if ($pool.Count -ge $count) {
            $idx = [int][Math]::Floor($i * $pool.Count / $count)
        } else {
            $idx = $i % $pool.Count
        }
        $out += $pool[$idx]
    }
    return $out
}

# Minimum distinct colors for a tile to count as pixel art.
# The collection mixes DCSS colored tiles with game-icons.net monochrome
# glyphs (a single white or black silhouette). Those glyphs look nothing like
# the rest of the game and black ones vanish on the dark UI, so they drop out.
$MIN_COLORS = 6

# Minimum opaque bounding box side. Filters out effect wisps and hairlines.
$MIN_SIDE = 10

$analysisCache = @{}

# Bounds + distinct-color count in one pixel pass.
function Analyze-Tile([string]$path) {
    if ($analysisCache.ContainsKey($path)) { return $analysisCache[$path] }
    $bmp = [System.Drawing.Bitmap]::FromFile($path)
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $fmt = [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, $fmt)
    $bytes = New-Object byte[] ($data.Stride * $bmp.Height)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    $stride = $data.Stride
    $w = $bmp.Width; $h = $bmp.Height
    $bmp.UnlockBits($data)
    $bmp.Dispose()

    $minX = $w; $minY = $h; $maxX = -1; $maxY = -1
    $colors = @{}
    for ($y = 0; $y -lt $h; $y++) {
        $rowOff = $y * $stride
        for ($x = 0; $x -lt $w; $x++) {
            $o = $rowOff + $x * 4
            if ($bytes[$o + 3] -le 16) { continue }
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
            # Quantize to 5 bits per channel so anti-aliasing noise does not
            # inflate the count.
            $key = (($bytes[$o + 2] -shr 3) * 1024) + (($bytes[$o + 1] -shr 3) * 32) +
                ($bytes[$o] -shr 3)
            $colors[$key] = 1
        }
    }
    $result = if ($maxX -lt 0) {
        $null
    } else {
        @{
            X = $minX; Y = $minY
            W = ($maxX - $minX + 1); H = ($maxY - $minY + 1)
            Colors = $colors.Count
        }
    }
    $analysisCache[$path] = $result
    return $result
}

function Is-PixelArt([string]$path) {
    $a = Analyze-Tile $path
    if ($null -eq $a) { return $false }
    if ($a.Colors -lt $MIN_COLORS) { return $false }
    if ([Math]::Max($a.W, $a.H) -lt $MIN_SIDE) { return $false }
    return $true
}

# Walk the pool in spread order and keep the first [count] tiles that look like
# pixel art. Falls back to cycling what we found when the pool runs dry.
function Pick-Usable([string[]]$pool, [int]$count) {
    $order = Pick-Spread $pool ([Math]::Min($pool.Count, [Math]::Max($count * 4, $count)))
    $seen = @{}
    $good = @()
    foreach ($p in $order) {
        if ($good.Count -ge $count) { break }
        if ($seen.ContainsKey($p)) { continue }
        $seen[$p] = 1
        if (Is-PixelArt $p) { $good += $p }
    }
    # Still short? sweep the whole pool.
    if ($good.Count -lt $count) {
        foreach ($p in $pool) {
            if ($good.Count -ge $count) { break }
            if ($seen.ContainsKey($p)) { continue }
            $seen[$p] = 1
            if (Is-PixelArt $p) { $good += $p }
        }
    }
    if ($good.Count -eq 0) { throw "no usable tiles in pool" }
    $out = @()
    for ($i = 0; $i -lt $count; $i++) { $out += $good[$i % $good.Count] }
    return @{ Picks = $out; Distinct = $good.Count }
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

# Level -> tier color arc. 21 levels mapped onto the 11 named tiers.
$tierRgb = @(
    @(0.62, 0.56, 0.45), @(0.62, 0.56, 0.45), @(0.62, 0.56, 0.45),   # +0..2  rusty
    @(0.75, 0.79, 0.84), @(0.75, 0.79, 0.84), @(0.75, 0.79, 0.84),   # +3..5  steel
    @(0.88, 0.92, 0.98), @(0.88, 0.92, 0.98), @(0.88, 0.92, 0.98),   # +6..8  silver
    @(0.56, 0.82, 0.90), @(0.56, 0.82, 0.90), @(0.56, 0.82, 0.90),   # +9..11 rune
    @(0.95, 0.55, 0.28), @(0.95, 0.55, 0.28), @(0.95, 0.55, 0.28),   # +12..14 flame
    @(0.94, 0.86, 0.34), @(0.94, 0.86, 0.34),                        # +15..16 thunder
    @(1.00, 0.97, 0.85), @(1.00, 0.97, 0.85),                        # +17..18 dawn
    @(0.62, 0.35, 0.90), @(0.62, 0.35, 0.90)                         # +19..20 black dragon
)

function New-LevelAttrs([int]$level, [double]$keep, [double]$tint) {
    $c = $tierRgb[$level]
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

# Light grading keeps the base art readable; heavy grading is for families whose
# pool is too small and must reuse tiles across levels.
$levelAttrs = @()
$levelAttrsStrong = @()
for ($l = 0; $l -lt $levels; $l++) {
    $levelAttrs += (New-LevelAttrs $l 0.55 0.45)
    $levelAttrsStrong += (New-LevelAttrs $l 0.30 0.70)
}

# fused = prismatic channel rotation, void = dark violet. Both on straight tiles.
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
    @(0.0, 0.0, 0.0, 1.0, 0.0),
    @(0.0, 0.0, 0.0, 0.0, 1.0)
)

$sheet = [System.Drawing.Bitmap]::new($cell * $levels, $cell * $rows)
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

function Draw-Cell([string]$path, [int]$col, [int]$row, $attrs) {
    $a = Analyze-Tile $path
    if ($null -eq $a) { throw "empty tile: $path" }
    $src = [System.Drawing.Bitmap]::FromFile($path)
    $bx = $a.X; $by = $a.Y; $bw = $a.W; $bh = $a.H
    # Integer-snap upscales: non-integer nearest-neighbor makes ragged pixels.
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

# --- rows 0..11: the twelve folder-backed families ---
$straightPicks = $null
for ($f = 0; $f -lt $familyFolders.Count; $f++) {
    $pool = Pool-Of $familyFolders[$f]
    $chosen = Pick-Usable $pool $levels
    $picks = $chosen.Picks
    if ($f -eq 0) { $straightPicks = $picks }
    # Fewer than 21 usable tiles means repeats - grade harder so levels differ.
    $strong = $chosen.Distinct -lt $levels
    for ($l = 0; $l -lt $levels; $l++) {
        $attrs = if ($strong) { $levelAttrsStrong[$l] } else { $levelAttrs[$l] }
        Draw-Cell $picks[$l] $l $f $attrs
    }
    Write-Host ("row {0}: pool={1} usable={2}{3}" -f
        $f, $pool.Count, $chosen.Distinct, $(if ($strong) { " (graded hard)" } else { "" }))
}

# --- rows 12,13: fused / void, recolored straight tiles ---
for ($l = 0; $l -lt $levels; $l++) {
    Draw-Cell $straightPicks[$l] $l 12 $fusedAttrs
    Draw-Cell $straightPicks[$l] $l 13 $voidAttrs
}

# --- row 14: legendary tiles for +21..+40 ---
$artifactPattern = 'urand_|sword_of_|spwpn_|scepter_of_|staff_of_|_brand|singing_sword|freezing_aura|ancient_sword|blessed_blade|demon_blade|_of_power'
$artifacts = @()
foreach ($i in $familyFolders) {
    $artifacts += (Get-ChildItem (Dir-Of $i) -Filter *.png |
        Where-Object { (Is-Safe $_.BaseName) -and ($_.BaseName -match $artifactPattern) } |
        ForEach-Object { $_.FullName })
}
$artifacts = @($artifacts | Sort-Object -Unique)
if ($artifacts.Count -lt $legendaryCount) {
    throw "not enough artifact tiles: $($artifacts.Count)"
}
$legendChosen = Pick-Usable $artifacts $legendaryCount
for ($i = 0; $i -lt $legendaryCount; $i++) {
    Draw-Cell $legendChosen.Picks[$i] $i $legendaryRow $null
}
Write-Host ("legendary: {0} candidates, {1} usable" -f $artifacts.Count, $legendChosen.Distinct)

$g.Dispose()
$sheet.Save($OutFile, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Host "sword_sheet3: $($cell*$levels)x$($cell*$rows) -> $OutFile"
