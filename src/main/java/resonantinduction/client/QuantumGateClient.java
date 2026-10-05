package resonantinduction.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import resonantinduction.quantum.QuantumGateBlockEntity;

/** Client ticker for gates: complete gates crackle with small arcs, as in the original. */
public final class QuantumGateClient {
    private QuantumGateClient() {}

    public static void tick(Level level, BlockPos pos, BlockState state, QuantumGateBlockEntity gate) {
        if (gate.frequency() == -1 || level.random.nextInt(3) != 0) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 to = center.add(level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5, level.random.nextDouble() - 0.5);
        ElectricBolts.add(center, to, 0xFFB0E0FF);
    }
}
