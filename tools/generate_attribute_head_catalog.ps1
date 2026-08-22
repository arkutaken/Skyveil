param(
    [Parameter(Mandatory = $true)][string]$NeuArchive,
    [Parameter(Mandatory = $true)][string]$Revision
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$abilityCatalog = Join-Path $projectRoot 'src/client/resources/assets/skyveil/data/attribute_ability_names.properties'
$outputCatalog = Join-Path $projectRoot 'src/client/resources/assets/skyveil/data/attribute_shard_heads.properties'
$headFallbacks = @{
    # Four official shard items use blocks. Use source-specific head textures so the overlay remains visually consistent.
    EXPERIENCE = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzY1NGEwZjhhODVkMTMxYWM5ZTYzMzc3NjgzN2Q5MzFmMGE5NTJkY2YxNzRjMjg5NzI3ODhmZWYzNDM5MTdlZCJ9fX0='
    ICE_ESSENCE = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTEzZjkxNTY5OThiODdiNGViMDIyYzIyOWE4MDA3YjdiOTAxMmY4YTVjYTdiOTE4MGU4MDk4YjZhODZmZTg1MCJ9fX0='
    ROTTEN_PICKAXE = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNThiZTA1Y2ZhZTJjNmE3ZDQ3ZGEyY2U4OGIzZTAwYzcyYTE0NWNjMzIxOGYwNDFiM2RkNWJkNWZhNWNhODI3In19fQ=='
    SPEED = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGRlNzE5YjcyOTA5ZWZhMDk3ODE1YTYzMzgwZjQ0NTZhZjllNGFmZWJkZDg5NGU1YjU4YjdjOWUwNTY3NTU3NyJ9fX0='
    UNDEAD = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTZiOGQ4NzQ1ZjZmYzdhMGE3NzM1NGNlMWE5ZjMwNDY4MTdmNjZkMmQzYWZkMWJjZGFjNmQyZDEwZjM3OSJ9fX0='
    WITHER_ESSENCE = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNkYjMyMWEwMjgwNmRkZjE4MjE3MjIyYWZhODBlZDhmNDdjYzg5MGJlMzIzNWVmMWQ5MGNjOGQ3OTA3MzNhOCJ9fX0='
    # Current upstream data reuses five textures for two unrelated sources; source-themed replacements keep every row unique.
    INFECTION = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmY1OTE0NjlhZmE1NTBiYWNlZWI4MTlmYTc1NWM5MTVmNmZjOTg2Mjk0MWQ3MjhjMDQ3NTg3OTRkNjJkOTcxIn19fQ=='
    HUMANOID_RULER_NEW = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmE4YWMxNWYwZjNhMTA4ZWE2N2Q0YzE3ZWM0NjI3YTBkNGMyNWVhOTFkNmRlYWJmYzE3YjI4MDY4NjM1OGU3MSJ9fX0='
    LOTUS_TROPHY = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWVmNjIwNDg4ZGMyNzNjMjZkNDJmNThlNzcyZTJjZmE0M2ZkZWVmOGVmNTgwYTk1ZjFhOTk1MThhZDMwYzRlZiJ9fX0='
    EXTREME_PRESSURE = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTM4YTQyYTgwYzcwYjI0MzY0M2VlNTAxNWU5ZDUxNDlhZjU0YWRlODNmZjU1YzIxNzlmOGE4YjNiMTA4MDVmNiJ9fX0='
    SPECIAL_UPGRADES = 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQzNDZjNWU0NDAzOWE1MDVlNWIxNmRiYTVlYzg0YjNhMzc5NzA5ZmIzMmI4NDI3ZDI5MzY2YTVlYTAzNmZiIn19fQ=='
}

$subtypes = Get-Content -LiteralPath $abilityCatalog |
    Where-Object { $_ -match '^([A-Z0-9_]+)=' } |
    ForEach-Object { $Matches[1] } |
    Sort-Object

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $NeuArchive))
try {
    $entries = @{}
    foreach ($entry in $zip.Entries) {
        if ($entry.Name -match '^ATTRIBUTE_SHARD_([A-Z0-9_]+);1\.json$') {
            $entries[$Matches[1]] = $entry
        }
    }

    $lines = [Collections.Generic.List[string]]::new()
    $lines.Add('# UUID and signed Mojang texture value for each consumable Attribute Shard source head')
    $lines.Add('# Generated from NotEnoughUpdates/NotEnoughUpdates-REPO items/ATTRIBUTE_SHARD_*;1.json')
    $lines.Add('# Eleven missing/duplicated profiles use source-specific custom heads from MCHeads/Minecraft-Heads')
    $lines.Add("# Revision: $Revision (MIT), verified 2026-08-21")
    $textures = [Collections.Generic.HashSet[string]]::new()
    foreach ($subtype in $subtypes) {
        $entry = $entries[$subtype]
        if ($null -eq $entry) { throw "Missing NEU item for $subtype" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $item = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
        if ($headFallbacks.ContainsKey($subtype)) {
            $texture = $headFallbacks[$subtype]
            $md5 = [Security.Cryptography.MD5]::Create()
            try { $uuid = [Guid]::new($md5.ComputeHash([Text.Encoding]::UTF8.GetBytes("skyveil:$subtype"))).ToString() } finally { $md5.Dispose() }
        } elseif ($item.itemid -eq 'minecraft:skull' -and $item.nbttag -match 'SkullOwner:\{Id:\"([^\"]+)\".*?Value:\"([^\"]+)\"') {
            $uuid = $Matches[1]
            $texture = $Matches[2]
        } else {
            throw "No player-head fallback for $subtype ($($item.itemid))"
        }
        [void]$textures.Add($texture)
        $lines.Add("$subtype=$uuid|$texture")
    }
    if ($lines.Count -ne ($subtypes.Count + 4)) { throw 'Generated catalog size is incorrect' }
    if ($textures.Count -ne $subtypes.Count) { throw "Expected $($subtypes.Count) unique textures, found $($textures.Count)" }
    [IO.File]::WriteAllLines($outputCatalog, $lines, [Text.UTF8Encoding]::new($false))
    Write-Output "Generated $($subtypes.Count) unique Attribute Shard heads at $outputCatalog"
} finally {
    $zip.Dispose()
}
