# 🌟 AEoneblockents (OneBlock Dynamic Events)

**AEoneblockents** es un plugin nativo de alto rendimiento para servidores Minecraft **Paper 1.21.x** (Java 21), diseñado específicamente para enriquecer la experiencia en modalidades **OneBlock** e islas mediante eventos periódicos climáticos, celestiales, agrícolas y de minería con integración nativa a **BentoBox**, **WorldGuard** y **GriefPrevention**.

---

## ⚡ Novedades de la Actualización (v1.1.0)

- **🧪 Lluvia Ácida en Segundos y Bypass de Armadura:**
  - Cadencia de daño totalmente configurable en **lenguaje de segundos** (`damage-interval-seconds`), eliminando la ambigüedad de ticks.
  - Daño por defecto calibrado a **1.0 (medio corazón por segundo)**.
  - Implementación de daño ambiental verdadero (`bypass-armor: true`) vía `DamageType.MAGIC`, evitando que armaduras de Netherite/Diamante absorban el 100% del daño o se degraden erráticamente.
  - `protect-in-claims: false` habilitado por defecto para garantizar que la lluvia ácida desafíe a los jugadores dentro de sus islas de OneBlock si no buscan techo.
  - Algoritmo de detección de techo corregido con `loc.getBlockY() >= highestY`.

- **🩸 Luna de Sangre con Daño de Mobs 100% Configurable:**
  - Multiplicador de daño cuerpo a cuerpo (`damage-multiplier`) aplicado a los atributos y al combate de zombies, arañas, etc.
  - Daño máximo de flechas de esqueleto configurable (`skeleton-max-arrow-damage`) para evitar knockback excesivo al vacío.
  - Probabilidad de phantoms aéreos (`phantom-chance`) configurable desde el YAML.

- **🌙 Luna de Suerte y Cosechas con Probabilidades Dinámicas:**
  - `trigger-chance`: Probabilidad de activación al picar minerales o cosechar cultivos maduros.
  - Probabilidades de diamantes y cultivos extra configurables porcentualmente (`bonus-diamonds-chance`, `bonus-crop-chance`).

---

## 📋 Catálogo de Eventos

| Evento | Tipo | Descripción |
| :--- | :--- | :--- |
| **🌧️ Lluvia Ácida** | Ambiental | Daña a los jugadores a la intemperie sin techo a razón de medio corazón cada N segundos. |
| **🩸 Luna de Sangre** | Invasión | Invasión de criaturas agresivas potenciadas en la isla cada 10 días de juego. Anti-grief en creepers. |
| **🌙 Luna de Suerte** | Minería | Multiplicador de drops (x4) y bonus de diamantes apilable con encantamiento Fortuna. |
| **🌾 Cosecha con Suerte**| Agricultura | Multiplicador en cultivos maduros (x3), probabilidad de cosechas extra y auto-replantado inteligente. |

---

## ⚙️ Archivo de Configuración (`config.yml`)

```yaml
settings:
  auto-scheduler-enabled: true
  interval-minutes: 45
  min-players-required: 1
  disabled-worlds:
    - "spawn"
    - "lobby"

events:
  acid_rain:
    display-name: "🌧️ Lluvia Ácida"
    duration-seconds: 180
    damage: 1.0                    # Daño aplicado (1.0 = medio corazón)
    damage-interval-seconds: 1     # Frecuencia en SEGUNDOS
    bypass-armor: true             # Ignora reducción de armadura física
    protect-in-claims: false       # Daña en islas si están al aire libre

  blood_moon:
    display-name: "🩸 Luna de Sangre"
    trigger-mode: "DAY_INTERVAL"
    day-interval: 10
    duration-seconds: 300
    prevent-sleep: true
    mob-spawning:
      enabled: true
      interval-seconds: 10
      min-radius: 4
      max-radius: 12
      max-mobs-per-player: 4
      creeper-anti-grief: true
      allow-phantoms: true
      phantom-chance: 0.25
    mob-buffs:
      speed-amplifier: 1
      strength-amplifier: 0
      health-multiplier: 1.5
      damage-multiplier: 1.5       # Daño de ataque aumentado
      skeleton-max-arrow-damage: 4.5

  lucky_mining:
    display-name: "🌙 Luna de Suerte"
    duration-seconds: 300
    trigger-chance: 1.0
    multiplier: 4
    bonus-diamonds: 2
    bonus-diamonds-chance: 0.50
    anti-exploit:
      anti-silk-touch: true
      track-placed-blocks: true

  lucky_harvest:
    display-name: "🌾 Cosecha con Suerte"
    duration-seconds: 300
    trigger-chance: 1.0
    multiplier: 3
    auto-replant: true
    bonus-crop-chance: 0.35
    bonus-crop-amount: 2
```

---

## 🚀 Requisitos de Compilación
- **Java:** JDK 21
- **Motor:** Paper / Purpur 1.21.x
- **Gestor:** Apache Maven 3.9+
