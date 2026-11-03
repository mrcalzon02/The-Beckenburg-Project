param([string]$Site='school')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$rebuildRoot=$PSScriptRoot
$sourcePath=Join-Path $rebuildRoot "$Site/rough-layout.json"
$data=Get-Content -LiteralPath $sourcePath -Raw | ConvertFrom-Json
$levels=@($data.zones.y | Sort-Object -Unique)
$columns=3
$wide=($data.site_bounds[2]-$data.site_bounds[0]) -gt 60
$canvasWidth=if($wide){2100}else{1500}
$columnWidth=($canvasWidth-15)/3
$panelHeight=if($wide){720}else{530}
$rows=[Math]::Ceiling($levels.Count/$columns)
$legendRows=[Math]::Ceiling($data.zones.Count/3)
$height=[int](100+$rows*$panelHeight+80+$legendRows*22)
$bitmap=New-Object System.Drawing.Bitmap($canvasWidth,$height)
$g=[System.Drawing.Graphics]::FromImage($bitmap)
$g.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::FromArgb(245,244,239))
$ink=New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(28,42,55))
$font=New-Object System.Drawing.Font('Arial',11)
$small=New-Object System.Drawing.Font('Arial',9)
$title=New-Object System.Drawing.Font('Arial',20,[System.Drawing.FontStyle]::Bold)
$line=New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(83,100,119),1)
$route=New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(48,126,153),1.5)
$stair=New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(168,101,22),2)
$stair.DashStyle=[System.Drawing.Drawing2D.DashStyle]::Dash
$colors=@{inside=[System.Drawing.Color]::FromArgb(208,216,226);outside=[System.Drawing.Color]::FromArgb(197,214,190);circulation=[System.Drawing.Color]::FromArgb(178,203,212);staging=[System.Drawing.Color]::FromArgb(175,206,207)}
$g.DrawString($data.title,$title,$ink,25,20)
$g.DrawString('S02 revised design study | metres | source coordinates only | no runtime or physical acceptance',$font,$ink,25,58)
$g.DrawString('Numbered rooms map to the key below. Cyan lines are connection diagrams; amber lines reserve real stair runs.',$small,$ink,25,82)
for($i=0;$i -lt $levels.Count;$i++){
    $y=$levels[$i];$ox=25+($i%3)*$columnWidth;$oy=120+[Math]::Floor($i/3)*$panelHeight
    $scale=[Math]::Min(8.0,[Math]::Min([double](($columnWidth-40)/($data.site_bounds[2]-$data.site_bounds[0])),[double](($panelHeight-60)/($data.site_bounds[3]-$data.site_bounds[1]))))
    $g.DrawString("Standing Y $y m",$font,$ink,[single]$ox,[single]$oy)
    $baseX=$ox+10;$baseZ=$oy+30
    $ordered=@($data.zones | Sort-Object @{Expression={if($_.kind -eq 'outside'){0}elseif($_.kind -eq 'staging'){1}else{2}}})
    foreach($z in $ordered){
        if($z.y -ne $y){continue}
        $bounds=$z.bounds;$x=$baseX+($bounds[0]-$data.site_bounds[0])*$scale;$zz=$baseZ+($bounds[1]-$data.site_bounds[1])*$scale
        $w=($bounds[2]-$bounds[0])*$scale;$d=($bounds[3]-$bounds[1])*$scale
        $brush=New-Object System.Drawing.SolidBrush($colors[$z.kind])
        $g.FillRectangle($brush,[single]$x,[single]$zz,[single]$w,[single]$d);$brush.Dispose()
        $g.DrawRectangle($line,[single]$x,[single]$zz,[single]$w,[single]$d)
    }
    foreach($link in $data.links){
        if(-not @($link.points | Where-Object {$_[1] -eq $y}).Count){continue}
        $pen=if($link.kind -eq 'stairs'){$stair}else{$route}
        for($j=0;$j -lt $link.points.Count-1;$j++){
            $a=$link.points[$j];$b=$link.points[$j+1]
            $g.DrawLine($pen,[single]($baseX+($a[0]-$data.site_bounds[0])*$scale),[single]($baseZ+($a[2]-$data.site_bounds[1])*$scale),[single]($baseX+($b[0]-$data.site_bounds[0])*$scale),[single]($baseZ+($b[2]-$data.site_bounds[1])*$scale))
        }
    }
    for($j=0;$j -lt $data.zones.Count;$j++){
        $z=$data.zones[$j];if($z.y -ne $y){continue};$b=$z.bounds
        $x=$baseX+(($b[0]+$b[2])/2-$data.site_bounds[0])*$scale;$zz=$baseZ+(($b[1]+$b[3])/2-$data.site_bounds[1])*$scale
        $g.FillEllipse([System.Drawing.Brushes]::White,[single]($x-9),[single]($zz-9),18,18)
        $g.DrawString(($j+1).ToString(),$small,$ink,[single]($x-7),[single]($zz-7))
    }
    $caption='N up | 10 m = '+([Math]::Round($scale*10,1))+' pixels | plan envelopes include unshaped exterior'
    $g.DrawString($caption,$small,$ink,[single]$ox,[single]($oy+$panelHeight-25))
}
$legendTop=120+$rows*$panelHeight
for($j=0;$j -lt $data.zones.Count;$j++){
    $z=$data.zones[$j];$text="$($j+1). $($z.name) [owner $($z.owner)]"
    $g.DrawString($text,$small,$ink,[single](25+($j%3)*$columnWidth),[single]($legendTop+[Math]::Floor($j/3)*22))
}
$destination=Join-Path $rebuildRoot "previews/$Site-revised-rough-plan.png"
$bitmap.Save($destination,[System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$bitmap.Dispose();$ink.Dispose();$font.Dispose();$small.Dispose();$title.Dispose();$line.Dispose();$route.Dispose();$stair.Dispose()
$report=[ordered]@{status='PLANNING_DIAGRAM_NOT_RUNTIME_CAPTURE';source_sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $sourcePath).Hash.ToLower();png_sha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $destination).Hash.ToLower();source="development/levels/rebuild/$Site/rough-layout.json";png="development/levels/rebuild/previews/$Site-revised-rough-plan.png"} | ConvertTo-Json
[System.IO.File]::WriteAllText((Join-Path $rebuildRoot "$Site/rough-render-verification.json"),$report.Replace("`r`n","`n")+"`n",(New-Object System.Text.UTF8Encoding($false)))
Write-Output "Rendered measured planning diagram: $destination"
