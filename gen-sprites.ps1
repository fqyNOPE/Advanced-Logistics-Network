Add-Type -AssemblyName System.Drawing

$dir = "D:\Users\Administrator\.openclaw\workspace\_mods_src\LogisticsNetwork\assets\sprites"
$blocks = "$dir\blocks"
$units = "$dir\units"
$root = "D:\Users\Administrator\.openclaw\workspace\_mods_src\LogisticsNetwork"
New-Item -ItemType Directory -Force -Path $blocks, $units | Out-Null

function New-Bitmap([int]$w, [int]$h) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    return @($bmp, $g)
}

function Save-Png($ctx, [string]$path) {
    $ctx[1].Dispose()
    $ctx[0].Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $ctx[0].Dispose()
}

function Add-RoundedRect($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, $brush, $pen) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    if ($brush) { $g.FillPath($brush, $path) }
    if ($pen) { $g.DrawPath($pen, $path) }
    $path.Dispose()
}

function Add-Chest([string]$path, $fill, $dark, $light, [string]$accent) {
    $ctx = New-Bitmap 32 32
    $g = $ctx[1]
    $body = New-Object System.Drawing.SolidBrush($fill)
    $darkPen = New-Object System.Drawing.Pen($dark, 2.5)
    $lightPen = New-Object System.Drawing.Pen($light, 1.6)
    Add-RoundedRect $g 4 7 24 21 3 $body $darkPen
    # lid line
    $g.DrawLine($lightPen, 6, 13, 26, 13)
    # latch
    $latch = New-Object System.Drawing.SolidBrush($dark)
    $g.FillRectangle($latch, 13.5, 17, 5, 6)
    # corner bolts
    $bolt = New-Object System.Drawing.SolidBrush($light)
    foreach ($pt in @(@(7,10), @(23,10), @(7,25), @(23,25))) {
        $g.FillEllipse($bolt, $pt[0]-1.5, $pt[1]-1.5, 3, 3)
    }
    $bolt.Dispose(); $latch.Dispose(); $lightPen.Dispose(); $darkPen.Dispose(); $body.Dispose()
    Save-Png $ctx $path
}

# --- supply point: red chest ---
Add-Chest "$blocks\logistics-supply-point.png" ([System.Drawing.Color]::FromArgb(255, 205, 70, 55)) ([System.Drawing.Color]::FromArgb(255, 120, 30, 25)) ([System.Drawing.Color]::FromArgb(255, 255, 170, 150)) $null

# --- request point: blue chest ---
Add-Chest "$blocks\logistics-request-point.png" ([System.Drawing.Color]::FromArgb(255, 62, 120, 200)) ([System.Drawing.Color]::FromArgb(255, 30, 60, 120)) ([System.Drawing.Color]::FromArgb(255, 150, 190, 255)) $null

# --- storage point: yellow chest ---
Add-Chest "$blocks\logistics-storage-point.png" ([System.Drawing.Color]::FromArgb(255, 215, 170, 60)) ([System.Drawing.Color]::FromArgb(255, 130, 95, 25)) ([System.Drawing.Color]::FromArgb(255, 255, 225, 140)) $null

# --- logistics base: 2x2 dark panel with antenna + radar dish ---
$ctx = New-Bitmap 64 64
$g = $ctx[1]
$panel = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 48, 62, 84))
$edge = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 24, 32, 46), 4)
Add-RoundedRect $g 6 8 52 50 5 $panel $edge
# inner panel
$inner = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 90, 115, 150), 2)
Add-RoundedRect $g 12 14 40 38 3 $null $inner
# radar dish: circle + sweep line
$dish = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 120, 190, 255))
$g.FillEllipse($dish, 24, 26, 16, 16)
$sweep = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 220, 240, 255), 2)
$g.DrawLine($sweep, 32, 34, 42, 24)
# antenna pole
$pole = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 200, 210, 225), 3)
$g.DrawLine($pole, 32, 8, 32, 2)
$tip = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 90, 90))
$g.FillEllipse($tip, 29.5, -1, 5, 5)
# corner bolts
$bolt = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 160, 175, 195))
foreach ($pt in @(@(10,12), @(52,12), @(10,52), @(52,52))) {
    $g.FillEllipse($bolt, $pt[0]-2.5, $pt[1]-2.5, 5, 5)
}
$bolt.Dispose(); $tip.Dispose(); $pole.Dispose(); $sweep.Dispose(); $dish.Dispose(); $inner.Dispose(); $edge.Dispose(); $panel.Dispose()
Save-Png $ctx "$blocks\logistics-base.png"

# --- logistics bot: small quadcopter drone ---
$ctx = New-Bitmap 32 32
$g = $ctx[1]
# rotor arms (4 diagonal)
$arm = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 120, 130, 150), 2)
$g.DrawLine($arm, 16, 16, 8, 8)
$g.DrawLine($arm, 16, 16, 24, 8)
$g.DrawLine($arm, 16, 16, 8, 24)
$g.DrawLine($arm, 16, 16, 24, 24)
# rotors
$rotor = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 200, 210, 230))
foreach ($pt in @(@(8,8), @(24,8), @(8,24), @(24,24))) {
    $g.FillEllipse($rotor, $pt[0]-3, $pt[1]-3, 6, 6)
}
# body
$bodyBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 235, 240, 248))
$bodyPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 70, 80, 100), 1.5)
$g.FillEllipse($bodyBrush, 9, 11, 14, 12)
$g.DrawEllipse($bodyPen, 9, 11, 14, 12)
# eye / cargo light
$eye = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 90, 180, 255))
$g.FillEllipse($eye, 12, 14, 4, 4)
$eye2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 200, 60))
$g.FillEllipse($eye2, 17, 15, 3, 3)
$eye2.Dispose(); $eye.Dispose(); $bodyPen.Dispose(); $bodyBrush.Dispose(); $rotor.Dispose(); $arm.Dispose()
Save-Png $ctx "$units\logistics-bot.png"

# --- mod icon ---
$ctx = New-Bitmap 64 64
$g = $ctx[1]
$bg = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 40, 52, 72))
$g.FillRectangle($bg, 0, 0, 64, 64)
# three chests
Add-RoundedRect $g 4 8 16 24 3 (New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 205, 70, 55))) $null
Add-RoundedRect $g 24 8 16 24 3 (New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 62, 120, 200))) $null
Add-RoundedRect $g 44 8 16 24 3 (New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 215, 170, 60))) $null
# bot below
$bot = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 235, 240, 248))
$g.FillEllipse($bot, 22, 38, 20, 18)
$arm = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 120, 130, 150), 2)
$g.DrawLine($arm, 32, 44, 24, 36)
$g.DrawLine($arm, 32, 44, 40, 36)
$arm.Dispose(); $bot.Dispose(); $bg.Dispose()
Save-Png $ctx "$root\icon.png"

Write-Output "sprites done"
Get-ChildItem -Recurse $dir, "$root\icon.png" -File | Select-Object -ExpandProperty FullName
