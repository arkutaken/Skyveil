param([Parameter(Mandatory=$true)][string]$Archive,[Parameter(Mandatory=$true)][string]$Items,[Parameter(Mandatory=$true)][string]$Reforges)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$recipes=@{};$tableEnchants=@{}
$zip=[IO.Compression.ZipFile]::OpenRead((Resolve-Path $Archive))
try{
foreach($entry in $zip.Entries){
if($entry.FullName -notmatch '/items/[^/]+\.json$'){continue}
$reader=[IO.StreamReader]::new($entry.Open())
try{$item=$reader.ReadToEnd()|ConvertFrom-Json}finally{$reader.Dispose()}
$options=[Collections.Generic.List[object]]::new()
if($item.recipe){$options.Add($item.recipe)}
foreach($recipe in $item.recipes){if($recipe.type -in @('crafting','forge')){$options.Add($recipe)}}
$converted=[Collections.Generic.List[object]]::new()
foreach($recipe in $options){
$ingredients=@{};$count=1
if($recipe.count){$count=[double]$recipe.count}
$slots=@()
if($recipe.type -eq 'forge'){$slots=@($recipe.inputs);if($recipe.output -match ':(\d+)$'){$count=[double]$matches[1]}}
else{foreach($prop in $recipe.PSObject.Properties){if($prop.Name -match '^[ABC][123]$'){$slots+= $prop.Value}}}
foreach($slot in $slots){if($slot -match '^(.+):(\d+)$'){$id=$matches[1];$amount=[double]$matches[2];if(-not $ingredients.ContainsKey($id)){$ingredients[$id]=0};$ingredients[$id]+=$amount}}
if($ingredients.Count -gt 0){$converted.Add(@{count=$count;ingredients=$ingredients})}
}
if($converted.Count -gt 0){$recipes[$item.internalname]=@($converted.ToArray())}
}
$entry=$zip.Entries|Where-Object FullName -Match '/constants/enchants.json$'|Select-Object -First 1
$reader=[IO.StreamReader]::new($entry.Open())
try{$enchantData=$reader.ReadToEnd()|ConvertFrom-Json}finally{$reader.Dispose()}
foreach($prop in $enchantData.max_xp_table_levels.PSObject.Properties){$tableEnchants[$prop.Name.ToUpperInvariant()]=$prop.Value}
}finally{$zip.Dispose()}
# NEU's max_xp_table_levels omits these basic table enchants.
foreach($entry in @{DRAGON_HUNTER=5;GRAVITY=5;MAGMARIZER=5;FIRE_ASPECT=2;AIMING=5;IMPALING=5;FLAME=1;PIERCING=1;SNIPE=3;KNOCKBACK=2;PUNCH=2}.GetEnumerator()){$tableEnchants[$entry.Key]=$entry.Value}
$definitions=@{};$soulboundItems=@{}
$api=Get-Content -LiteralPath $Items -Raw | ConvertFrom-Json
foreach($item in $api.items){
if($item.soulbound -in @('COOP','SOLO')){$soulboundItems[$item.id]=$item.soulbound}
$definition=@{tier=$item.tier}
foreach($key in @('upgrade_costs','gemstone_slots','dungeon_item_conversion_cost','prestige')){
if($null-ne $item.$key){$definition[$key]=$item.$key}
}
$definitions[$item.id]=$definition
}
$reforgeMap=@{}
$raw=Get-Content -LiteralPath $Reforges -Raw|ConvertFrom-Json
foreach($entry in $raw.PSObject.Properties){
$r=$entry.Value
if($r.reforgeName){$reforgeMap[$r.reforgeName.ToLowerInvariant().Replace(' ','_')]=@{item=$r.internalName;costs=$r.reforgeCosts;type=$r.reforgeType}}
}
$output=Join-Path $PSScriptRoot '../src/client/resources/assets/skyveil/data/craft_cost_catalog.json'
[IO.File]::WriteAllText($output,(@{recipes=$recipes;items=$definitions;soulboundItems=$soulboundItems;reforges=$reforgeMap;tableEnchants=$tableEnchants}|ConvertTo-Json -Depth 32 -Compress))
Write-Host "Generated $($recipes.Count) recipes, $($definitions.Count) item definitions, $($reforgeMap.Count) reforges."