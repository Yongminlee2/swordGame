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

function Draw-Cell([string]$path, [int]$col, [int]$row, $attrs) {
    $src = [System.Drawing.Bitmap]::FromFile($path)
    $dest = New-Object System.Drawing.Rectangle(($col * $cell), ($row * $cell), $cell, $cell)
    if ($null -eq $attrs) {
        $g.DrawImage($src, $dest)
    } else {
        $g.DrawImage($src, $dest, 0, 0, $src.Width, $src.Height,
            [System.Drawing.GraphicsUnit]::Pixel, $attrs)
    }
    $src.Dispose()
}

for ($f = 0; $f -lt $familyIds.Count; $f++) {
    for ($t = 0; $t -lt $tiers.Count; $t++) {
        Draw-Cell (Find-Curated ($f + 1) $familyIds[$f] $tiers[$t]) $t $f $null
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
