param(
    [Parameter(Mandatory=$true)][string]$RepoPath,
    [string]$OutputPath = ''
)

$ErrorActionPreference='Stop'
if([string]::IsNullOrWhiteSpace($OutputPath)){$OutputPath=Join-Path $PSScriptRoot '..\src\client\resources\assets\skyveil\data\item_search_catalog.json'}
$itemsPath=Join-Path $RepoPath 'items'
if(-not (Test-Path -LiteralPath $itemsPath -PathType Container)){throw "NEU items directory not found: $itemsPath"}
$mojibake=([string][char]0x00C2)+([string][char]0x00A7)
$section=[string][char]0x00A7
function Clean-Text([string]$value){if($null -eq $value){return ''};return $value.Replace($mojibake,$section)}
function Pad-Base64([string]$value){if([string]::IsNullOrWhiteSpace($value)){return ''};$value=$value.TrimEnd('=');while(($value.Length%4)-ne 0){$value+='='};return $value}

$catalog=[System.Collections.Generic.List[object]]::new()
foreach($file in Get-ChildItem -LiteralPath $itemsPath -File -Filter '*.json' | Sort-Object Name){
    try{$item=Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8 | ConvertFrom-Json}catch{continue}
    if([string]::IsNullOrWhiteSpace($item.internalname)-or [string]::IsNullOrWhiteSpace($item.displayname)){continue}
    $nbt=[string]$item.nbttag;$model='';$uuid='';$texture=''
    if($nbt -match 'ItemModel:\"([^\"]+)\"'){$model=$matches[1]}
    if($nbt -match 'SkullOwner:\{Id:\"([^\"]+)\"'){$uuid=$matches[1]}
    if($nbt -match 'Value:\"([^\"]+)\"'){$texture=Pad-Base64 $matches[1]}
    $lore=@();if($null-ne $item.lore){$lore=@($item.lore | ForEach-Object {Clean-Text ([string]$_)})}
    $craftable=$null-ne $item.recipe
    if(-not $craftable -and $null-ne $item.recipes){foreach($recipe in @($item.recipes)){$type=[string]$recipe.type;if([string]::IsNullOrWhiteSpace($type)-or $type.Equals('crafting',[System.StringComparison]::OrdinalIgnoreCase)){$craftable=$true;break}}}
    $catalog.Add([ordered]@{i=[string]$item.internalname;n=(Clean-Text ([string]$item.displayname));b=[string]$item.itemid;m=$model;u=$uuid;t=$texture;l=$lore;c=$craftable})
}
if($catalog.Count -lt 8000){throw "Expected at least 8000 NEU items, generated $($catalog.Count)"}
$parent=Split-Path -Parent $OutputPath;New-Item -ItemType Directory -Force -Path $parent | Out-Null
$catalog | ConvertTo-Json -Depth 4 -Compress | Set-Content -LiteralPath $OutputPath -Encoding UTF8
Write-Host "Generated $($catalog.Count) searchable SkyBlock items at $OutputPath"
