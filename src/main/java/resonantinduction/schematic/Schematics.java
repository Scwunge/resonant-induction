package resonantinduction.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import resonantinduction.RIFeatures;
import resonantinduction.ResonantInduction;
import resonantinduction.registry.RIRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The structures the Creative Builder can put down, as the original's schematics: a particle accelerator ring, fission and breeding
 * reactors, a fusion reactor, and wind and water turbine discs. Each maps offsets from the builder to blocks; later entries win.
 */
public final class Schematics {
    private Schematics() {}

    public interface Builder {
        void build(Map<BlockPos, BlockState> out, Direction dir, int size);
    }

    public record Schematic(String name, String feature, Builder builder) {
        public String key() {
            return "schematic.resonantinduction." + name;
        }

        public boolean available() {
            return feature == null || RIFeatures.enabled(feature);
        }

        public Map<BlockPos, BlockState> structure(Direction dir, int size) {
            Map<BlockPos, BlockState> out = new LinkedHashMap<>();
            builder.build(out, dir, size);
            return out;
        }
    }

    public static final List<Schematic> ALL = List.of(
            new Schematic("accelerator", "atomic", Schematics::accelerator),
            new Schematic("breeding_reactor", "atomic", Schematics::breedingReactor),
            new Schematic("fission_reactor", "atomic", Schematics::fissionReactor),
            new Schematic("fusion_reactor", "atomic", Schematics::fusionReactor),
            new Schematic("wind_turbine", null, (out, dir, size) -> turbineDisc(out, dir, size, "wind")),
            new Schematic("water_turbine", null, (out, dir, size) -> turbineDisc(out, dir, size, "water")));

    private static BlockState magnet() {
        return RIRegistries.ELECTROMAGNET.get().defaultBlockState();
    }

    private static BlockState magnetGlass() {
        return RIRegistries.ELECTROMAGNET_GLASS.get().defaultBlockState();
    }

    private static BlockState piston(Direction facing) {
        return Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, facing);
    }

    /** Three square rings: electromagnet walls inside and out, and between them a tunnel roofed and floored with glass. */
    static void accelerator(Map<BlockPos, BlockState> out, Direction dir, int size) {
        for (int r : new int[] {size, size - 2}) {
            ring(out, r, (x, y, z) -> magnet());
        }
        ring(out, size - 1, (x, y, z) -> y == 0 ? Blocks.AIR.defaultBlockState() : magnetGlass());
    }

    private interface At {
        BlockState at(int x, int y, int z);
    }

    private static void ring(Map<BlockPos, BlockState> out, int r, At block) {
        for (int x = -r; x < r; x++) {
            for (int z = -r; z < r; z++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == -r || x == r - 1 || z == -r || z == r - 1) {
                        out.put(new BlockPos(x, y, z), block.at(x, y, z));
                    }
                }
            }
        }
    }

    /** A pool with a cross of reactor cells, thermometers wired to sirens below, and control rods on pistons in the corners. */
    static void breedingReactor(Map<BlockPos, BlockState> out, Direction dir, int size) {
        int r = Math.max(size, 2);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                out.put(new BlockPos(x, 0, z), Blocks.WATER.defaultBlockState());
            }
        }
        r--;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (Math.sqrt(x * x + z * z) <= 2) {
                    if (!((x == -r || x == r) && (z == -r || z == r))) {
                        out.put(new BlockPos(x, 0, z), RIRegistries.REACTOR_CELL.get().defaultBlockState());
                        out.put(new BlockPos(x, -1, z), RIRegistries.THERMOMETER.get().defaultBlockState());
                        out.put(new BlockPos(x, -3, z), RIRegistries.SIREN.get().defaultBlockState());
                        out.put(new BlockPos(x, -2, z), Blocks.REDSTONE_WIRE.defaultBlockState());
                    } else {
                        out.put(new BlockPos(x, -1, z), RIRegistries.CONTROL_ROD.get().defaultBlockState());
                        out.put(new BlockPos(x, -2, z), piston(Direction.UP));
                    }
                }
            }
        }
        out.put(new BlockPos(0, -2, 0), Blocks.STONE.defaultBlockState());
        out.put(new BlockPos(0, -3, 0), Blocks.STONE.defaultBlockState());
        out.put(BlockPos.ZERO, RIRegistries.REACTOR_CELL.get().defaultBlockState());
    }

    /**
     * Size 1: a reactor cell in a pool under a square of turbines, control rods on pistons and a thermometer wired to a siren
     * below. Larger: a glass-walled column of water round a column of cells, control rods on pistons in the walls, turbines on top.
     */
    static void fissionReactor(Map<BlockPos, BlockState> out, Direction dir, int size) {
        if (size <= 1) {
            int r = 2;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    out.put(new BlockPos(x, 0, z), Blocks.WATER.defaultBlockState());
                }
            }
            r--;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    out.put(new BlockPos(x, 1, z), RIRegistries.ELECTRIC_TURBINE.get().defaultBlockState());
                    if (!((x == -r || x == r) && (z == -r || z == r)) && Math.sqrt(x * x + z * z) <= 1) {
                        out.put(new BlockPos(x, -1, z), RIRegistries.CONTROL_ROD.get().defaultBlockState());
                        out.put(new BlockPos(x, -2, z), piston(Direction.UP));
                    }
                }
            }
            out.put(new BlockPos(0, -1, 0), RIRegistries.THERMOMETER.get().defaultBlockState());
            out.put(new BlockPos(0, -3, 0), RIRegistries.SIREN.get().defaultBlockState());
            out.put(new BlockPos(0, -2, 0), Blocks.REDSTONE_WIRE.defaultBlockState());
            out.put(BlockPos.ZERO, RIRegistries.REACTOR_CELL.get().defaultBlockState());
            return;
        }
        int r = 2;
        for (int y = 0; y < size; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    double distance = Math.sqrt(x * x + z * z);
                    if (y < size - 1) {
                        if (distance == 2) {
                            out.put(pos, RIRegistries.CONTROL_ROD.get().defaultBlockState());
                            Direction outward = Direction.getNearest(x, 0, z);
                            out.put(pos.relative(outward), piston(outward.getOpposite()));
                        } else if (x == -r || x == r || z == -r || z == r) {
                            out.put(pos, Blocks.GLASS.defaultBlockState());
                        } else if (x == 0 && z == 0) {
                            out.put(pos, RIRegistries.REACTOR_CELL.get().defaultBlockState());
                        } else {
                            out.put(pos, Blocks.WATER.defaultBlockState());
                        }
                    } else if (distance < 2) {
                        out.put(pos, RIRegistries.ELECTRIC_TURBINE.get().defaultBlockState());
                    }
                }
            }
        }
    }

    /** A round chamber of electromagnets with domed glass ends, round a column of reactor cells wrapped in electromagnets. */
    static void fusionReactor(Map<BlockPos, BlockState> out, Direction dir, int size) {
        int radius = size + 2;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= size; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    double magnitude = Math.sqrt(x * x + z * z);
                    out.putIfAbsent(pos, Blocks.AIR.defaultBlockState());
                    if (magnitude > radius) {
                        continue;
                    }
                    if (y == 0 || y == size) {
                        if (magnitude >= 1) {
                            double deviation = (y == 0 ? size / 3 : -size / 3) + (y == 0 ? -1 : 1) * Math.sin(magnitude / radius * Math.PI) * size / 2d;
                            out.put(new BlockPos(x, (int) Math.round(y + deviation), z), magnetGlass());
                        }
                    } else if (magnitude > radius - 1) {
                        out.put(pos, magnet());
                    }
                }
            }
        }
        for (int y = 0; y < size; y++) {
            out.put(new BlockPos(0, y, 0), RIRegistries.REACTOR_CELL.get().defaultBlockState());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                out.put(new BlockPos(0, y, 0).relative(d), magnet());
            }
        }
        out.put(BlockPos.ZERO, RIRegistries.REACTOR_CELL.get().defaultBlockState());
    }

    /** A disc of wooden turbines across the way the player faces, all facing that way. */
    static void turbineDisc(Map<BlockPos, BlockState> out, Direction dir, int size, String kind) {
        Block turbine = BuiltInRegistries.BLOCK.get(ResonantInduction.id(kind + "_turbine_wood"));
        BlockState state = turbine.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, dir);
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                for (int z = -size; z <= size; z++) {
                    if (dir.getStepX() != 0 && x == 0 || dir.getStepY() != 0 && y == 0 || dir.getStepZ() != 0 && z == 0) {
                        out.put(new BlockPos(x, y, z), state);
                    }
                }
            }
        }
    }
}
