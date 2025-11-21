param(
  [string[]]$Files
)

$months = @{
  # Polish
  'STYCZEŃ'=1; 'STYCZEN'=1; 'LUTY'=2; 'MARZEC'=3; 'KWIECIEŃ'=4; 'KWIECIEN'=4; 'MAJ'=5; 'CZERWIEC'=6;
  'LIPIEC'=7; 'SIERPIEŃ'=8; 'SIERPIEN'=8; 'WRZESIEŃ'=9; 'WRZESIEN'=9; 'PAŹDZIERNIK'=10; 'PAZDZIERNIK'=10;
  'LISTOPAD'=11; 'GRUDZIEŃ'=12; 'GRUDZIEN'=12;
  # French
  'JANVIER'=1; 'FÉVRIER'=2; 'FEVRIER'=2; 'MARS'=3; 'AVRIL'=4; 'MAI'=5; 'JUIN'=6; 'JUILLET'=7;
  'AOÛT'=8; 'AOUT'=8; 'SEPTEMBRE'=9; 'OCTOBRE'=10; 'NOVEMBRE'=11; 'DÉCEMBRE'=12; 'DECEMBRE'=12;
  # Spanish
  'ENERO'=1; 'FEBRERO'=2; 'MARZO'=3; 'ABRIL'=4; 'MAYO'=5; 'JUNIO'=6; 'JULIO'=7;
  'AGOSTO'=8; 'SEPTIEMBRE'=9; 'OCTUBRE'=10; 'NOVIEMBRE'=11; 'DICIEMBRE'=12;
  # Italian
  'GENNAIO'=1; 'FEBBRAIO'=2; 'APRILE'=4; 'MAGGIO'=5; 'GIUGNO'=6; 'LUGLIO'=7;
  'SETTEMBRE'=9; 'OTTOBRE'=10; 'DICEMBRE'=12
}

function Reformat-QuotesFile {
  param([string]$Path)

  $orig = Get-Content -Raw -LiteralPath $Path
  Copy-Item -LiteralPath $Path -Destination ($Path + '.bak') -Force
  $lines = $orig -split "`r?`n"
  $out = New-Object System.Collections.Generic.List[string]
  $currentMonth = $null
  $currentDay = $null
  $currentQuote = New-Object System.Text.StringBuilder

  function Flush-Quote {
    if ($currentMonth -ne $null -and $currentDay -ne $null -and $currentQuote -ne $null -and $currentQuote.Length -gt 0) {
      $dayPadded = '{0:00}' -f [int]$currentDay
      $prefix = "$dayPadded/$currentMonth. "
      $text = $currentQuote.ToString().Trim()
      $out.Add($prefix + $text)
    }
    $currentDay = $null
    if ($currentQuote -ne $null) { $currentQuote.Clear() | Out-Null }
  }

  foreach ($rawLine in $lines) {
    $line = $rawLine.TrimEnd()
    if ([string]::IsNullOrWhiteSpace($line)) { continue }

    # Month header detection (allow trailing digits like 'Gennaio1')
    $head = ($line -replace '\s+',' ').Trim()
    $headKey = $head.ToUpperInvariant() -replace '\d+$',''
    if ($months.ContainsKey($headKey)) {
      Flush-Quote
      $currentMonth = $months[$headKey]
      continue
    }

    # Italian editorial note skip
    if ($Path.ToUpperInvariant().EndsWith('QUOTES_IT.TXT') -and $line -match '^\s*\d+Il presente lavoro') {
      continue
    }

    # New day line: "1." or "01." or "1 " or "01 "
    if ($line -match '^\s*(\d{1,2})(?:\.|\s)\s*(.*)$') {
      Flush-Quote
      $currentDay = [int]$Matches[1]
      $rest = $Matches[2]
      if ($rest.Length -gt 0) { [void]$currentQuote.Append($rest) }
      continue
    }

    # Continuation line
    if ($currentQuote.Length -gt 0) { [void]$currentQuote.Append(' ') }
    [void]$currentQuote.Append($line)
  }

  Flush-Quote
  # Allow up to 366 lines; trim only if more than 366
  if ($out.Count -gt 366) {
    $out = [System.Collections.Generic.List[string]]($out | Select-Object -First 366)
  }
  Set-Content -LiteralPath $Path -Value ($out.ToArray()) -Encoding UTF8
}

foreach ($f in $Files) {
  Reformat-QuotesFile -Path $f
}
Write-Host 'Done. Backups saved as .bak'
