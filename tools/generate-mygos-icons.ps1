# Empaqueta el logo aprobado; no altera el archivo fuente ni inventa otra marca.
param([string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = [IO.Path]::GetFullPath($ProjectRoot)
$resources = Join-Path $root 'app/src/main/res'
$sourcePath = Join-Path $resources 'drawable/logo.png'
Copy-Item -LiteralPath $sourcePath -Destination (Join-Path $resources 'drawable/mygos_logo.png') -Force
$source = [Drawing.Image]::FromFile($sourcePath)
function New-Canvas([int]$size, [bool]$background) {
    $bitmap = [Drawing.Bitmap]::new($size, $size, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([Drawing.Color]::Transparent)
    if ($background) { $graphics.Clear([Drawing.ColorTranslator]::FromHtml('#F7F5FF')) }
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [Drawing.Drawing2D.CompositingQuality]::HighQuality
    return @($bitmap, $graphics)
}
function Save-Icon([int]$size, [double]$fraction, [string]$path, [bool]$background, [bool]$round) {
    $canvas = New-Canvas $size $background
    $bitmap = $canvas[0]; $graphics = $canvas[1]
    if ($round) {
        $shape = [Drawing.Drawing2D.GraphicsPath]::new()
        $shape.AddEllipse(0, 0, $size, $size)
        $graphics.SetClip($shape)
        $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#F7F5FF'))
        $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.FillEllipse($brush, 0, 0, $size, $size)
        $brush.Dispose()
    }
    $width = [int][Math]::Floor($size * $fraction)
    $offset = [int][Math]::Floor(($size - $width) / 2)
    $graphics.DrawImage($source, $offset, $offset, $width, $width)
    $graphics.Dispose()
    $bitmap.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
    if ($round) { $shape.Dispose() }
}
try {
    $nodpi = Join-Path $resources 'drawable-nodpi'
    [IO.Directory]::CreateDirectory($nodpi) | Out-Null
    # Una copia de presentación sin escalado por densidad evita decodificar el original grande al iniciar.
    Save-Icon 256 1.0 (Join-Path $nodpi 'mygos_brand_mark.png') $false $false
    # Todo el PNG queda dentro del círculo seguro de 66 dp del lienzo adaptativo de 108 dp.
    Save-Icon 432 (46.0 / 108.0) (Join-Path $nodpi 'mygos_icon_foreground.png') $false $false
    $foreground = [Drawing.Bitmap]::new((Join-Path $nodpi 'mygos_icon_foreground.png'))
    $mono = [Drawing.Bitmap]::new(432, 432, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 432; $y++) {
        for ($x = 0; $x -lt 432; $x++) {
            $pixel = $foreground.GetPixel($x, $y)
            $range = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B)) - [Math]::Min($pixel.R, [Math]::Min($pixel.G, $pixel.B))
            if ($pixel.A -gt 0 -and $range -gt 25 -and $pixel.B -gt $pixel.R) {
                $mono.SetPixel($x, $y, [Drawing.Color]::FromArgb($pixel.A, 0, 0, 0))
            }
        }
    }
    $mono.Save((Join-Path $nodpi 'mygos_icon_monochrome.png'), [Drawing.Imaging.ImageFormat]::Png)
    $mono.Dispose(); $foreground.Dispose()
    $densities = @{ 'mdpi' = 48; 'hdpi' = 72; 'xhdpi' = 96; 'xxhdpi' = 144; 'xxxhdpi' = 192 }
    foreach ($density in $densities.GetEnumerator()) {
        $directory = Join-Path $resources "mipmap-$($density.Key)"
        Save-Icon $density.Value 0.64 (Join-Path $directory 'ic_launcher.png') $true $false
        Save-Icon $density.Value 0.64 (Join-Path $directory 'ic_launcher_round.png') $false $true
        foreach ($name in @('ic_launcher.webp', 'ic_launcher_round.webp')) {
            $old = [IO.Path]::GetFullPath((Join-Path $directory $name))
            if (-not $old.StartsWith($resources + [IO.Path]::DirectorySeparatorChar)) { throw 'Recurso fuera del proyecto.' }
            if (Test-Path -LiteralPath $old) { Remove-Item -LiteralPath $old }
        }
    }
} finally { $source.Dispose() }
Write-Output 'Iconos generados a partir del logo aprobado; original conservado.'
