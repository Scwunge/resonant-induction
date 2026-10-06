package resonantinduction.transformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

/**
 * Transformer: energy entering its input face goes straight out of the opposite face, as in the original. Forge Energy has no
 * voltage, so stepping up or down (by 2x-4x, set with a wrench) only changes what it reports; the flow passes through unchanged.
 */
public class TransformerBlockEntity extends BlockEntity {
    private boolean stepUp = true;

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.TRANSFORMER_BE.get(), pos, state);
    }

    public Direction input() {
        return getBlockState().getValue(TransformerBlock.FACING);
    }

    public boolean stepUp() {
        return stepUp;
    }

    public boolean toggleStep() {
        stepUp = !stepUp;
        setChanged();
        return stepUp;
    }

    /** Voltage ratio, as the original computed it: (multiplier + 2) up or down. */
    public int ratio() {
        return getBlockState().getValue(TransformerBlock.MULTIPLIER) + 2;
    }

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        if (side != input()) {
            return null;
        }
        return new IEnergyStorage() {
            @Nullable
            private IEnergyStorage out() {
                Direction output = input().getOpposite();
                return level == null ? null : level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(output), output.getOpposite());
            }

            @Override
            public int receiveEnergy(int max, boolean simulate) {
                IEnergyStorage out = out();
                return out == null || !out.canReceive() ? 0 : out.receiveEnergy(max, simulate);
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                IEnergyStorage out = out();
                return out == null ? 0 : out.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("stepUp", stepUp);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stepUp = !tag.contains("stepUp") || tag.getBoolean("stepUp");
    }
}
