$filePath = "Fabric\\src\\main\\java\\com\\fleettools\\data\\PlayerDataManager.java"
$content = Get-Content $filePath -Raw

# Remove the problematic TypeAdapter class
$content = $content -replace "(?s)// Safe ItemStack TypeAdapter.*?}\s*}\s*(?=\s*private static final Gson)", ""

# Write back the content  
Set-Content $filePath $content
Write-Host "Fixed Gson configuration by removing TypeAdapter"