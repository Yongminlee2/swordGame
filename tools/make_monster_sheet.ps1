# 낱장 PNG 40장을 8열x7행 32px 시트로 합친다.
# 사용: .\make_monster_sheet.ps1 -ListFile cells.txt -OutFile ..\app\src\main\res\drawable\monster_sheet.png
# cells.txt: 한 줄에 낱장 파일 경로 하나, 순서 = 칸 번호(0부터).
# 칸 순서 계약: 0~29 잡몹(Zone 순서 x 몬스터 3), 30~39 보스(Zone 순서).
#               MonsterSheet.kt 가 이 순서를 그대로 믿는다.
param(
    [Parameter(Mandatory)][string]$ListFile,
    [Parameter(Mandatory)][string]$OutFile
)
Add-Type -AssemblyName System.Drawing
$cell = 32; $cols = 8; $rows = 7
$sheet = [System.Drawing.Bitmap]::new($cell * $cols, $cell * $rows)
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$i = 0
foreach ($line in Get-Content $ListFile) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $src = [System.Drawing.Bitmap]::FromFile($line.Trim())
    $x = ($i % $cols) * $cell; $y = [math]::Floor($i / $cols) * $cell
    # 원본이 32px이 아니면 니어리스트로 칸에 맞춘다 (픽셀아트 보존)
    $g.DrawImage($src, (New-Object System.Drawing.Rectangle($x, $y, $cell, $cell)))
    $src.Dispose(); $i++
}
$g.Dispose()
$sheet.Save($OutFile, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Host "cells=$i -> $OutFile"
