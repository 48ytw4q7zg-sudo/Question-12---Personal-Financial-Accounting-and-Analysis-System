param(
    [Parameter(Mandatory = $true)]
    [string]$DocumentPath,

    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory,

    [int]$Dpi = 144
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$documentFullPath = [System.IO.Path]::GetFullPath($DocumentPath)
$outputFullPath = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($outputFullPath) | Out-Null

$word = $null
$document = $null

try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0

    $document = $word.Documents.Open($documentFullPath, $false, $true)
    $document.Repaginate()
    $window = $document.Windows.Item(1)
    $window.View.Type = 3
    $pageCount = $document.ComputeStatistics(2)

    for ($pageNumber = 1; $pageNumber -le $pageCount; $pageNumber++) {
        $page = $window.Panes.Item(1).Pages.Item($pageNumber)
        $emfBytes = $page.EnhMetaFileBits
        $emfPath = Join-Path $outputFullPath ("page-{0:D3}.emf" -f $pageNumber)
        $pngPath = Join-Path $outputFullPath ("page-{0:D3}.png" -f $pageNumber)
        [System.IO.File]::WriteAllBytes($emfPath, $emfBytes)

        $source = [System.Drawing.Image]::FromFile($emfPath)
        try {
            $sourceDpi = if ($source.HorizontalResolution -gt 0) { $source.HorizontalResolution } else { 96 }
            $width = [Math]::Max(1, [int][Math]::Round($source.Width * $Dpi / $sourceDpi))
            $height = [Math]::Max(1, [int][Math]::Round($source.Height * $Dpi / $sourceDpi))
            $bitmap = New-Object System.Drawing.Bitmap($width, $height)
            try {
                $bitmap.SetResolution($Dpi, $Dpi)
                $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
                try {
                    $graphics.Clear([System.Drawing.Color]::White)
                    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                    $graphics.DrawImage($source, 0, 0, $width, $height)
                }
                finally {
                    $graphics.Dispose()
                }
                $bitmap.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
            }
            finally {
                $bitmap.Dispose()
            }
        }
        finally {
            $source.Dispose()
        }
    }

    Write-Output ("page_count={0}" -f $pageCount)
    Write-Output ("output_directory={0}" -f $outputFullPath)
}
finally {
    if ($document -ne $null) {
        $document.Close($false)
    }
    if ($word -ne $null) {
        $word.Quit()
    }
    if ($document -ne $null) {
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($document)
    }
    if ($word -ne $null) {
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
