# Resonant Induction (1.21.1 NeoForge)

A port of **Resonant Induction**, Calclavia's electrical tech mod, to Minecraft 1.21.1 on NeoForge. It works on its own
and uses Forge Energy (FE), so it connects to Mekanism, Electrodynamics, Create Crafts & Additions, Applied Energistics 2,
Immersive Engineering and any other FE mod.

## Features

### Tesla Coil
- Stack Tesla Coils into a tower. Each coil above the first adds 4 blocks of range, up to 50.
- Power the bottom coil from any side except the top. The tower beams energy through the air to every other tower of two
  or more coils in range (the nearest 10), and those towers push it into the machines next to their bottom coil.
- **Frequencies:** right-click a tower with a dye. Towers only talk to towers of the same colour; light blue (the default)
  talks to every colour.
- **Empty hand:** toggles whether the tower receives. **Redstone dust:** toggles zapping mobs caught in the arcs.
  **Redstone signal** on the bottom coil stops the tower sending. **Sneak + empty hand:** shows the tower's status.
- **Quantum Entangler:** use it on one tower, then on another, to link them. Linked towers beam only to each other, even
  across dimensions (the far tower's chunk has to be loaded). Sneak-use a tower to unlink it.

All numbers (buffer, range, damage, sounds, cross-dimension links) are in the server config,
`world/serverconfig/resonantinduction-server.toml`.

## Recipes
With Electrodynamics installed, the Tesla Coil uses its copper wire, battery and steel plate, as in the original. Without
it, it uses copper ingots, a redstone block and iron ingots. JEI shows a description for every block.

## Credits
- **Original mod:** Calclavia (Henry Mao), with Aidan C. Brady (the original Tesla Coil), DarkGuardsman, tgame14 and
  Vexatos. Art by CyanideX and Archadia.
- Original source: [calclavia/Electrodynamics](https://github.com/calclavia/Electrodynamics) (successor repository, which
  holds the full history) and [Vexatos/Resonant-Induction-1](https://github.com/Vexatos/Resonant-Induction-1).
- **1.21.1 port:** Scwunge.

## Licence
MIT, see [LICENSE](LICENSE) and [PERMISSION.md](PERMISSION.md).

## Building
Java 21. `./gradlew build` produces `build/libs/ResonantInduction-1.21.1-<version>.jar`. `./gradlew runGameTestServer`
runs the GameTests.
