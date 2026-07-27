# 낱장 PNG 를 8열 32px 시트로 합친다. 행 수는 목록 길이에서 나온다.
# 사용: .\make_monster_sheet.ps1 -ListFile cells.txt -OutFile ..\app\src\main\res\drawable\monster_sheet.png
# cells.txt: 한 줄에 낱장 파일 경로 하나, 순서 = 칸 번호(0부터).
# 칸 순서 계약: 잡몹(Zone 순서 x 몬스터 5) -> 보스(Zone 순서) -> 펫(PetKind 순서).
#               MonsterSheet.kt 가 이 순서를 그대로 믿고 MonsterSheetTest 가 지킨다.
param(
    [Parameter(Mandatory)][string]$ListFile,
    [Parameter(Mandatory)][string]$OutFile
)
Add-Type -AssemblyName System.Drawing
# GDI+ 의 Save 는 .NET 의 현재 디렉터리를 기준으로 상대경로를 푼다. 그건 PowerShell 의
# 위치와 다를 수 있어서, 상대경로를 그대로 넘기면 시트가 조용히 엉뚱한 폴더에 떨어진다.
# 실제로 그렇게 만들어진 시트를 며칠 동안 못 보고 지나쳤다. 여기서 절대경로로 굳힌다.
$OutFile = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutFile))
$count = (Get-Content $ListFile | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count
$cell = 32; $cols = 8; $rows = [math]::Ceiling($count / $cols)
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
