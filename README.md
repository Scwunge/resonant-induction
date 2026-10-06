# Resonant Induction (1.21.1 NeoForge)

A full port of **Resonant Induction**, Calclavia's tech mod, to Minecraft 1.21.1 on NeoForge: electricity and Tesla coils,
mechanical power, ore processing and smelting, fluids, logistics, and the Atomic module (fission, particle physics,
antimatter and fusion). It works on its own and uses Forge Energy (FE), so it connects to Mekanism, Electrodynamics,
Create Crafts & Additions, Applied Energistics 2, Immersive Engineering and any other FE mod.

JEI shows a description of every block and item. Recipes use `c:` tags where they can, so other mods' metals and parts work.

## Features

### Electrical
- **Tesla Coil:** stack coils into a tower (each coil adds 4 blocks of range, up to 50) that beams energy through the air to
  other towers, which push it into the machines beside them. Dye a tower to set its frequency; empty hand toggles receiving,
  redstone dust toggles zapping mobs, a redstone signal stops sending. The **Quantum Entangler** links two towers, even
  across dimensions.
- **Wires** in six metals (copper, tin, iron, aluminium, silver, superconductor), flat on a face or framed through a block.
  Insulate with wool (bare live wires shock) and dye the insulation to keep lines apart.
- **Battery** (three tiers; touching batteries merge into one bank; each face set to input, output or off), **Charger**,
  **Transformer**, **Multimeter** (stored energy, flow, capacity, fluid, torque, speed, temperature, pressure; side by side
  they make one big screen), **Solar Panel** and **Thermopile**.
- **Electromagnetic Levitator:** carries items along a beam between levitators. **Quantum Glyphs** build **Quantum Gates**
  that teleport mobs and items to gates on the same frequency, in any dimension. **Mining Laser** (remove, smelt, damage).

### Mechanical
- **Gears** and **gear shafts** (wood, stone, metal) that mesh and carry torque and speed; the **Hand Crank**.
- **Wind** and **water turbines** in three tiers; neighbours join into bigger ones with a wrench.
- **Electric Motor:** turns power into rotation, or works as a generator.

### Ore processing and smelting
- **Mechanical Piston**, **Grinding Wheel**, **Millstone**: ores into rubble, rubble into dust. **Mixer** and **Filter**:
  dust and water into a mixture, filtered into refined dust. Dusts, rubble and mixtures exist for every metal other mods add.
- **Firebox** (and an electric one) melts dust into molten metal; the **Casting Mold** casts it into ingots; the
  **Hot Plate** smelts on top of a firebox.

### Fluids
- **Gutters**, **Tanks** (touching tanks share their fluid), six **Pipes** (ceramic to fiberglass, dyeable), the **Pump** and
  the **Grate**, on the original's pressure model: fluid flows from high pressure to low, losing one per pipe.

### Workshop and logistics
- **Crates** (wood, iron, steel), the **Engineering Table**, **Hammer**, **Imprinter** and **Imprints**, **Turntable**.
- **Conveyor Belts** (flat, slanted, raised), **Manipulators**, **Detectors**, **Sorters**, **Breakers** and **Placers**.

### Atomic
- **Uranium:** radioactive ore; the **Chemical Extractor** (yellowcake, deuterium, tritium), **Nuclear Boiler** (uranium
  hexafluoride) and **Centrifuge** (Uranium-235 and -238); **cells**, **fuel rods** and the **hazmat suit**. Radiation is a
  status effect that the full suit keeps off.
- **Fission:** the **Reactor Cell** heats up with a fuel rod and boils the water touching it; the steam drives **Electric
  Turbines** (join a 3x3 for a big one) through **Steam Funnels**. **Control Rods** slow it; a hot enough reactor breeds fuel;
  it makes toxic waste, and one kept at 2000 K melts down. The **Thermometer** and **Siren** watch it.
- **Particle physics:** the **Particle Accelerator** fires items down a tunnel of **Electromagnets**; a particle at full speed
  becomes **antimatter**, and fast particles meeting may leave **dark matter**. The **Fulmination Generator** catches the
  energy of antimatter going off; the **Quantum Assembler** copies items with dark matter.
- **Fusion:** the **Plasma Heater** turns deuterium and tritium into plasma, which a reactor cell lets out into a chamber
  walled with electromagnets.
- **Creative Builder** (creative mode only) puts down ready-made structures: an accelerator ring, fission, breeding and
  fusion reactors, and wind and water turbine discs.

## Configuration
- `config/resonantinduction-common.toml`: a switch for each feature group. Turning one off drops its recipes (and, for
  Atomic, uranium ore generation). Blocks stay registered, so existing worlds are safe and client and server need not match.
  Several groups overlap other mods (Electrodynamics wires, batteries, chargers, multimeters and generators; Nuclear Science).
- `world/serverconfig/resonantinduction-server.toml`: the balance numbers (Tesla range and buffer, battery tiers, wire FE per
  amp, grate speed, the atomic energy scales, turbine and boiling multipliers, meltdowns, antimatter explosions, dark matter
  chance, what the Quantum Assembler may copy, and more). The defaults are the original's numbers.

## Differences from the original
- **Forge Energy instead of Universal Electricity.** FE has no voltage, so voltage-only behaviour is gone (the Transformer
  only passes power one way). Wire capacity is the metal's original amp rating times `fePerAmp`. Atomic machines use their
  original joules times `energyScale` (default 1). The Fulmination Generator and Quantum Assembler worked in 10^13 J, far past
  what FE can carry, so they are scaled by `antimatterEnergyScale` (default 10^-6), keeping their balance with each other.
- **Rewritten, not copied:** the parts that came from Calclavia's libraries (Universal Electricity, Resonant Engine) are new
  code: radiation, the heat model under the reactors, the multiblocks, the connected-texture borders, the machine screens
  and the Creative Builder.
- **Bugs fixed:** the Chemical Extractor could not refine uranium (a wrong output-slot check); the reactor's meltdown counter
  reset every tick, so reactors never melted down (now they can; `reactorMeltdowns` turns it off); the Quantum Assembler used
  five of its six dark matter; a few recipes named items that did not exist (casting mold, detector, hazmat suit), now filled
  with sensible parts.
- **Small changes:** a particle's top speed is capped the same way in every direction (the original capped only two). The
  Plasma Heater stops when its plasma tank is full instead of wasting gas. The Quantum Assembler copies up to a full stack, and
  a tag (`resonantinduction:quantum_assembler_blacklist`, bundles by default) keeps items with contents out. Accelerated
  particles are not saved with the world; the accelerator fires a new one.
- **Not ported:** parts that were never in a released Resonant Induction: the Rejector, Reactor Drain, Thermal Laser, Item
  Railing, and the Armbot and Encoder.

## Credits
- **Original mod:** Calclavia (Henry Mao), with Aidan C. Brady (the original Tesla Coil), DarkGuardsman, tgame14 and
  Vexatos. Art by CyanideX and Archadia.
- Original source: [calclavia/Electrodynamics](https://github.com/calclavia/Electrodynamics) (successor repository, which
  holds the full history) and [Vexatos/Resonant-Induction-1](https://github.com/Vexatos/Resonant-Induction-1).
- **1.21.1 port:** Scwunge.

## Licence
MIT, see [LICENSE](LICENSE). Ported with the permission of Calclavia, whose original source is also released under MIT.
Resonant Induction is free and will stay free.

## Building
Java 21. `./gradlew build` produces `build/libs/ResonantInduction-1.21.1-<version>.jar`. `./gradlew runGameTestServer`
runs the GameTests.
