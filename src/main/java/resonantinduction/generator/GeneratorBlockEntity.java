package resonantinduction.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/** A small generator: fills a buffer each tick and pushes it out of every side, like the original "output on all sides" machines. */
public abstract class GeneratorBlockEntity extends BlockEntity {
    protected int energy;

    private final IEnergyStorage output = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int max, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            int extracted = Math.max(0, Math.min(max, energy));
            if (!simulate && extracted > 0) {
                energy -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract int capacity();

    /** FE made this tick. */
    protected abstract int generate(ServerLevel level);

    @Nullable
    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return output;
    }

    public int energy() {
        return energy;
    }

    public static <T extends GeneratorBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T be) {
        be.tick((ServerLevel) level);
    }

    protected void tick(ServerLevel level) {
        int made = generate(level);
        if (made > 0) {
            energy = Math.min(capacity(), energy + made);
            setChanged();
        }
        for (Direction side : Direction.values()) {
            if (energy <= 0) {
                break;
            }
            BlockPos target = worldPosition.relative(side);
            if (level.getBlockEntity(target) instanceof GeneratorBlockEntity) {
                continue;
            }
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, side.getOpposite());
            if (storage != null && storage.canReceive()) {
                int sent = storage.receiveEnergy(energy, false);
                if (sent > 0) {
                    energy -= sent;
                    setChanged();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getInt("energy");
    }
}
