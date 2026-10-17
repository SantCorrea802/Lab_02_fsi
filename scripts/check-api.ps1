$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$baseUri = 'http://localhost:8080/api/evaluations'

function Assert-True($condition, [string]$message) {
    if (-not $condition) { throw $message }
}

function Invoke-Ok([string]$uri, [string]$body) {
    $response = Invoke-WebRequest -UseBasicParsing -Method Post -Uri $uri -ContentType 'application/json' -Body $body
    Assert-True ($response.StatusCode -eq 200) "Se esperaba HTTP 200, pero se recibió HTTP $($response.StatusCode)."
    return $response.Content | ConvertFrom-Json
}

function Assert-BadRequest([string]$uri, [string]$body) {
    try {
        Invoke-WebRequest -UseBasicParsing -Method Post -Uri $uri -ContentType 'application/json' -Body $body | Out-Null
        throw 'Se esperaba HTTP 400, pero la petición fue aceptada.'
    } catch [System.Net.WebException] {
        $status = [int]$_.Exception.Response.StatusCode
        Assert-True ($status -eq 400) "Se esperaba HTTP 400, pero se recibió HTTP $status."
    }
}

$passengerFile = Join-Path $projectRoot 'examples\passenger.json'
$flightFile = Join-Path $projectRoot 'examples\flight.json'

$first = Invoke-Ok "$baseUri/passenger" (Get-Content -LiteralPath $passengerFile -Raw)
$second = Invoke-Ok "$baseUri/passenger" (Get-Content -LiteralPath $passengerFile -Raw)
$passenger = $first.passengers[0]
Assert-True (-not $passenger.upgradedToBusinessClass -and -not $passenger.upgradeEligible) 'El exceso de equipaje no vetó el ascenso.'
Assert-True ($passenger.compensationAmount -eq 200 -and $passenger.loyaltyPointsAwarded -eq 500 -and $passenger.totalLoyaltyPoints -eq 600) 'La compensación o los puntos son incorrectos.'
Assert-True ($passenger.vipLoungeAccess -and $passenger.assignedSeatId -eq 'E1') 'El acceso VIP o asiento de emergencia son incorrectos.'
Assert-True (($first | ConvertTo-Json -Depth 10 -Compress) -eq ($second | ConvertTo-Json -Depth 10 -Compress)) 'Repetir la petición cambió el resultado.'
Write-Output 'PASAJERO: HTTP 200; conflicto de ascenso resuelto; $200, 500 puntos, VIP y E1; repetición idéntica.'
Write-Output ($first | ConvertTo-Json -Depth 10)

$flight = Invoke-Ok "$baseUri/flight" (Get-Content -LiteralPath $flightFile -Raw)
Assert-True ($flight.passengers.Count -eq 2) 'El vuelo no devolvió dos pasajeros.'
Assert-True ($flight.passengers[0].assignedSeatId -eq 'F1' -and $flight.passengers[1].assignedSeatId -eq 'E1') 'La prioridad familiar o el asiento de emergencia fallaron.'
Assert-True (@($flight.seats | Where-Object { $_.assignedPassengerId -eq 'FAMILIA' }).Count -eq 1) 'Se duplicó el asiento de la familia.'
Assert-True (@($flight.seats | Where-Object { $_.assignedPassengerId -eq 'INDIVIDUAL' }).Count -eq 1) 'Se duplicó el asiento del pasajero individual.'
Write-Output 'VUELO: HTTP 200; F1 para familia y E1 para pasajero individual; sin asignación doble.'
Write-Output ($flight | ConvertTo-Json -Depth 10)

$invalid = Get-Content -LiteralPath $passengerFile -Raw | ConvertFrom-Json
$invalid.flight.delayMinutes = -1
Assert-BadRequest "$baseUri/passenger" ($invalid | ConvertTo-Json -Depth 10)
Write-Output 'VALIDACIÓN: HTTP 400 para retraso negativo.'

$duplicate = Get-Content -LiteralPath $flightFile -Raw | ConvertFrom-Json
$duplicate.passengers[1].id = $duplicate.passengers[0].id
Assert-BadRequest "$baseUri/flight" ($duplicate | ConvertTo-Json -Depth 10)
Write-Output 'VALIDACIÓN: HTTP 400 para identificadores de pasajero duplicados.'

$short = Get-Content -LiteralPath $passengerFile -Raw | ConvertFrom-Json
$short.flight.id = 'AV-303'
$short.flight.delayMinutes = 0
$short.flight.durationMinutes = 119
$short.passenger.age = 66
$short.passenger.membership = 'Basic'
$short.passenger.luggageKg = 16
$short.passenger.seatPreference = 'Window'
$short.seats = @()
$senior = Invoke-Ok "$baseUri/passenger" ($short | ConvertTo-Json -Depth 10)
Assert-True ($senior.passengers[0].priorityCheckIn -and -not $senior.passengers[0].luggageAllowed) 'Fallaron la prioridad para mayores de 65 años o la restricción de equipaje en vuelo corto.'
Assert-True ($senior.passengers[0].activatedRules.Count -eq 2) 'El vuelo corto activó reglas inesperadas.'
Write-Output 'MAYOR Y VUELO CORTO: HTTP 200; check-in prioritario y equipaje no permitido.'
Write-Output ($senior | ConvertTo-Json -Depth 10)

$scarce = Get-Content -LiteralPath $flightFile -Raw | ConvertFrom-Json
$scarce.seats = @($scarce.seats | Where-Object { $_.id -eq 'E1' })
$scarceResult = Invoke-Ok "$baseUri/flight" ($scarce | ConvertTo-Json -Depth 10)
Assert-True ($scarceResult.passengers[0].assignedSeatId -eq 'E1' -and $null -eq $scarceResult.passengers[1].assignedSeatId) 'La prioridad familiar con un solo asiento falló.'
Assert-True ($scarceResult.passengers[1].seatMessage -match 'No hay asiento') 'No se informó la falta de asiento.'
Assert-True ($scarceResult.seats[0].assignedPassengerId -eq 'FAMILIA') 'El asiento escaso se asignó al pasajero equivocado.'
Write-Output 'ASIENTO ESCASO: HTTP 200; la familia recibió E1 y al segundo pasajero se le informó que no hay asiento.'
Write-Output ($scarceResult | ConvertTo-Json -Depth 10)

$boundary = Get-Content -LiteralPath $passengerFile -Raw | ConvertFrom-Json
$boundary.flight.delayMinutes = 60
$boundary.flight.durationMinutes = 300
$boundary.passenger.age = 65
$boundary.passenger.membership = 'Gold'
$boundary.passenger.luggageKg = 23
$boundary.passenger.seatPreference = 'Window'
$boundary.seats = @()
$limits = Invoke-Ok "$baseUri/passenger" ($boundary | ConvertTo-Json -Depth 10)
Assert-True ($limits.passengers[0].activatedRules.Count -eq 0) 'Un valor límite exacto activó una regla estricta.'
Assert-True ($limits.passengers[0].upgradeEligible -and $limits.passengers[0].totalLoyaltyPoints -eq 100) 'Los límites alteraron elegibilidad o puntos.'
Write-Output 'LÍMITES EXACTOS: HTTP 200; 60 min de retraso, 300 min de vuelo, 65 años y 23 kg sin activaciones.'

$activated = @($first.passengers[0].activatedRules) + @($flight.passengers | ForEach-Object { $_.activatedRules }) + @($senior.passengers[0].activatedRules) + @($scarceResult.passengers | ForEach-Object { $_.activatedRules })
$expectedRules = @(
    'UpgradeToBusinessClassForFrequentFlyersWithDelays',
    'PriorityCheckInForSeniors',
    'DiscountForLightLuggage',
    'DenyUpgradeForOverweightLuggage',
    'AssignEmergencyExitSeatToYoungAdults',
    'CompensationForExtremeDelays',
    'ExtraLoyaltyPointsForLongFlights',
    'RestrictLuggageOnShortFlights',
    'VipLoungeAccessForPlatinumMembers',
    'PreferentialSeatForFamilies'
)
foreach ($rule in $expectedRules) { Assert-True ($activated -contains $rule) "No se observó por HTTP la regla $rule." }
Write-Output 'COBERTURA HTTP: se observaron las diez reglas Drools en respuestas reales.'

$duplicateSeats = Get-Content -LiteralPath $flightFile -Raw | ConvertFrom-Json
$duplicateSeats.seats[1].id = $duplicateSeats.seats[0].id
Assert-BadRequest "$baseUri/flight" ($duplicateSeats | ConvertTo-Json -Depth 10)
Write-Output 'VALIDACIÓN: HTTP 400 para identificadores de asiento duplicados.'

$blank = Get-Content -LiteralPath $passengerFile -Raw | ConvertFrom-Json
$blank.passenger.membership = ''
Assert-BadRequest "$baseUri/passenger" ($blank | ConvertTo-Json -Depth 10)
Write-Output 'VALIDACIÓN: HTTP 400 para membresía vacía.'

Assert-BadRequest "$baseUri/passenger" '{'
Write-Output 'VALIDACIÓN: HTTP 400 para JSON malformado.'

Write-Output 'API REAL: todos los escenarios comprobados.'
