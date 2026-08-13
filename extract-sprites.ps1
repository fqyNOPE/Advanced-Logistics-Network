# Extracts the vanilla unit-cargo-loader / unit-cargo-unload-point textures
# from the game atlas and writes them as the mod's block textures.
# Run from anywhere; paths are resolved from this script's folder.
Add-Type -AssemblyName System.Drawing

$root = $PSScriptRoot
$src = Join-Path $root "tmp_atlas\sprites\sprites.png"
$outDir = Join-Path $root "assets\sprites\blocks"

$bmp = [System.Drawing.Bitmap]::FromFile($src)

function Crop($x, $y, $w, $h, $out){
    $rect = New-Object System.Drawing.Rectangle($x, $y, $w, $h)
    $crop = $bmp.Clone($rect, $bmp.PixelFormat)
    $crop.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $crop.Dispose()
    Write-Output "saved $out"
}

# unit-cargo-loader 96x96 @ (2765,1814) -> 3x3 base
Crop 2765 1814 96 96 (Join-Path $outDir "logistics-base.png")

# unit-cargo-unload-point 64x64 @ (2497,324) -> 2x2 blue box
Crop 2497 324 64 64 (Join-Path $outDir "logistics-request-point.png")

# unit-cargo-unload-point-top 18x18 @ (3490,494), orig 64x64 -> center it on a 64x64 canvas
$rect = New-Object System.Drawing.Rectangle(3490, 494, 18, 18)
$top = $bmp.Clone($rect, $bmp.PixelFormat)
$canvas = New-Object System.Drawing.Bitmap(64, 64)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear([System.Drawing.Color]::Transparent)
$g.DrawImage($top, 23, 23, 18, 18)
$canvas.Save((Join-Path $outDir "logistics-request-point-top.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $canvas.Dispose(); $top.Dispose()

$bmp.Dispose()
Write-Output "done"
