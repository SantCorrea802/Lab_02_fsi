# Laboratorio 2: reglas de una aerolínea

Aplicación REST de Java 17, Spring Boot 3.2.5 y Drools 7.74.1.Final para evaluar las diez reglas del ejercicio de la página 29 de `material/lab02.pdf`. El ejemplo bancario del material no forma parte de esta aplicación. No usa base de datos, servicios externos ni inteligencia artificial.

## Requisitos y ejecución en Windows

- JDK 17 (el script usa `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`; ajuste la ruta si su instalación difiere).
- Maven 3.6.3 o superior. La versión instalada durante el desarrollo es 3.9.16.
- Acceso a Maven Central para la primera descarga de dependencias.

Desde PowerShell, en la raíz del proyecto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

Los scripts seleccionan JDK 17 solo para su proceso y usan `.m2/repository` dentro del proyecto. `-ExecutionPolicy Bypass` se aplica solo al proceso de PowerShell que ejecuta el script y no modifica la política global de Windows. El log de `scripts/test.ps1` confirma Java 17; un `mvn -version` ejecutado después en otra consola puede seguir mostrando el JDK global. El segundo comando deja el servidor en `http://localhost:8080` hasta presionar Ctrl+C.

## API

| Método y ruta | Contenido | Resultado |
|---|---|---|
| `POST /api/evaluations/passenger` | `flight`, `passenger`, `seats` | Resultado de un pasajero y estado de los asientos |
| `POST /api/evaluations/flight` | `flight`, `passengers`, `seats` | Resultados del vuelo y estado de los asientos |

Ejemplos ejecutables en PowerShell con el servidor activo:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/evaluations/passenger -ContentType 'application/json' -InFile .\examples\passenger.json | ConvertTo-Json -Depth 8
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/evaluations/flight -ContentType 'application/json' -InFile .\examples\flight.json | ConvertTo-Json -Depth 8
```

Con el servidor activo, `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-api.ps1` ejecuta peticiones HTTP reales. Verifica que se observen las diez reglas en `activatedRules`, el conflicto entre ascenso y equipaje, la prioridad familiar, la asignación única de asientos, la repetición de solicitudes, valores límite exactos y cinco entradas inválidas con HTTP 400. El registro de la ejecución real está en [`evidencias/07-api-real.txt`](evidencias/07-api-real.txt).

`durationMinutes` y `delayMinutes` se expresan en minutos; `luggageKg` en kilogramos. `membership` distingue exactamente `Basic`, `Gold` y `Platinum` para las reglas que los nombran; cualquier membresía no vacía distinta de `Basic` satisface la regla 7. `seatPreference: "Any"` representa ausencia de preferencia específica. Los únicos asientos gestionados por estas reglas son `FAMILY` y `EMERGENCY_EXIT`. Un asiento enviado con `occupied: true` no se puede asignar. `loyaltyPoints` es opcional y vale cero si se omite.

Una entrada inválida o identificadores duplicados producen HTTP 400 con `message` y `details`. Las respuestas válidas incluyen `activatedRules` por pasajero, beneficios concedidos, el asiento asignado y el inventario final.

## Reglas de Drools

Las diez reglas están en [`src/main/resources/com/udea/airline/rules/airline-rules.drl`](src/main/resources/com/udea/airline/rules/airline-rules.drl). La aplicación falla al iniciar si Drools informa errores al compilarlas.

| Regla | Condición | Acción |
|---|---|---|
| 1. `UpgradeToBusinessClassForFrequentFlyersWithDelays` | Gold o Platinum, retraso > 60 min | Solicita ascenso a ejecutiva |
| 2. `PriorityCheckInForSeniors` | Edad > 65 | Check-in prioritario |
| 3. `DiscountForLightLuggage` | Equipaje < 10 kg | Descuento del 10 % |
| 4. `DenyUpgradeForOverweightLuggage` | Equipaje > 23 kg | No elegible para ascenso |
| 5. `AssignEmergencyExitSeatToYoungAdults` | Edad 18 a 40 inclusive, `Any`, asiento de emergencia disponible | Reserva el asiento y lo ocupa |
| 6. `CompensationForExtremeDelays` | Retraso > 180 min | Compensación de $200 |
| 7. `ExtraLoyaltyPointsForLongFlights` | Membresía distinta de Basic, duración > 300 min | 500 puntos adicionales |
| 8. `RestrictLuggageOnShortFlights` | Equipaje > 15 kg, duración < 120 min | Equipaje no permitido |
| 9. `VipLoungeAccessForPlatinumMembers` | Membresía Platinum | Acceso VIP |
| 10. `PreferentialSeatForFamilies` | Viaja con niños y tiene preferencia `Any` | Reserva asiento familiar |

La compensación conserva el símbolo `$` del enunciado: el PDF no define una moneda. El descuento se devuelve como porcentaje, pues el enunciado no proporciona tarifa base. Las reglas 1 y 4 pueden activarse juntas: la solicitud de ascenso se registra, pero el resultado final exige que el pasajero siga siendo elegible; el equipaje > 23 kg veta el ascenso. Esta combinación se evalúa con propiedades independientes, por lo que no depende del orden de reglas.

La prioridad de asientos es **familiar antes que emergencia**. Drools la establece con `salience` explícita; dentro de cada clase se atiende el orden de pasajeros de la petición. El inventario compartido durante una evaluación impide asignar un asiento o un pasajero dos veces. Si no queda asiento apropiado, `assignedSeatId` es `null` y `seatMessage` explica la situación. Un adulto que viaja con niños puede usar un asiento de emergencia si no hay uno familiar y cumple la regla 5.

Tras ocupar un asiento, las reglas actualizan explícitamente la decisión y el inventario en la sesión KIE. Así se retiran las activaciones pendientes cuya disponibilidad dejó de cumplirse; los indicadores de procesamiento evitan que la misma regla vuelva a reservar otro asiento. Esta política de asignación está en el DRL y no depende del orden en que aparecen las reglas.

Cada petición crea hechos, decisiones, inventario y sesión nuevos. Las reglas asignan beneficios únicos (500 puntos y $200), no suman repetidamente sobre una respuesta previa. Repetir una petición idéntica da el mismo resultado sin guardar estado entre solicitudes.

## Pruebas y evidencias

`scripts/test.ps1` ejecuta `mvn clean verify` con JDK 17. `BusinessRulesTest` comprueba las diez reglas, límites, conflictos, ocupación única y repetición. `EvaluationApiTest` comprueba los endpoints y errores HTTP mediante MockMvc. Los resultados de compilación y ejecución reales se documentan en [`evidencias/REGISTRO.md`](evidencias/REGISTRO.md); las capturas incluidas en `evidencias/` proceden de la pantalla del usuario.

En la primera ejecución se observó la advertencia de JVM `Sharing is only supported for boot loader classes because bootstrap classpath has been appended`. Se emitió durante las pruebas y no impidió su éxito ni la creación del JAR. Es compatible con la instrumentación de Mockito/Byte Buddy incluida en el entorno de pruebas ([incidencia del proyecto Mockito](https://github.com/mockito/mockito/issues/3111)); se documenta sin cambiar la configuración de la aplicación.

## Estructura

```text
pom.xml
src/main/java/com/udea/airline/api/       DTO, controlador y errores
src/main/java/com/udea/airline/config/    Carga de Drools
src/main/java/com/udea/airline/rules/     Hechos, decisiones e inventario
src/main/java/com/udea/airline/service/   Orquestación de la sesión
src/main/resources/com/udea/airline/rules/ Diez reglas DRL
src/test/java/com/udea/airline/           Pruebas automatizadas
examples/                                  Solicitudes JSON
evidencias/                                Capturas y bitácora técnica
```

El directorio `material/` está excluido de Git, como también los artefactos de compilación, el repositorio Maven local y archivos de configuración local o secretos.
