param(
    [Parameter(Mandatory = $true)]
    [string]$DocumentPath,

    [Parameter(Mandatory = $true)]
    [string]$PdfPath
)

$ErrorActionPreference = "Stop"
$word = $null
$document = $null

try {
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $document = $word.Documents.Open(
        [System.IO.Path]::GetFullPath($DocumentPath),
        $false,
        $true
    )
    $document.Repaginate()
    $document.ExportAsFixedFormat(
        [System.IO.Path]::GetFullPath($PdfPath),
        17,
        $false,
        0,
        0,
        1,
        1,
        0,
        $true,
        $true,
        1,
        $true,
        $true,
        $false
    )
    Write-Output ("pdf={0}" -f [System.IO.Path]::GetFullPath($PdfPath))
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
