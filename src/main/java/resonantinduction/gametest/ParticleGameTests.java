package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.Edges;
import resonantinduction.atomic.ThermalGrid;
import resonantinduction.atomic.particle.AcceleratorBlock;
import resonantinduction.atomic.particle.AcceleratorBlockEntity;
import resonantinduction.atomic.particle.FulminationBlock;
import resonantinduction.atomic.particle.ParticleEntity;
import resonantinduction.atomic.particle.QuantumAssemblerBlockEntity;
import resonantinduction.registry.RIRegistries;

import java.util.List;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class ParticleGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;
    static final BlockPos ACCELERATOR = new BlockPos(1, 2, 3);

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electromagnetsShedHeat(GameTestHelper helper) {
        BlockPos magnet = new BlockPos(3, 2, 3);
        BlockPos stone = new BlockPos(10, 2, 3);
        helper.setBlock(magnet, RIRegistries.ELECTROMAGNET.get());
        helper.setBlock(stone, Blocks.STONE);
        ThermalGrid.addTemperature(helper.getLevel(), helper.absolutePos(magnet), 1000);
        ThermalGrid.addTemperature(helper.getLevel(), helper.absolutePos(stone), 1000);
        helper.runAfterDelay(10, () -> {
            float m = ReactorGameTests.temperature(helper, magnet) - ThermalGrid.AMBIENT;
            float s = ReactorGameTests.temperature(helper, stone) - ThermalGrid.AMBIENT;
            helper.assertTrue(m < s / 2, "electromagnet " + m + " K over ambient, stone " + s);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void electromagnetsShareABorder(GameTestHelper helper) {
        BlockPos a = new BlockPos(3, 2, 3);
        helper.setBlock(a, RIRegistries.ELECTROMAGNET.get());
        helper.setBlock(a.east(), RIRegistries.ELECTROMAGNET_GLASS.get());
        helper.setBlock(a.west(), Blocks.STONE);
        helper.assertBlockProperty(a, Edges.property(Direction.EAST), true);
        helper.assertBlockProperty(a, Edges.property(Direction.WEST), false);
        helper.setBlock(a.east(), Blocks.AIR);
        helper.assertBlockProperty(a, Edges.property(Direction.EAST), false);
        helper.succeed();
    }

    /** An accelerator at x 1 facing west, firing east down a tunnel of electromagnets {@code length} long, powered by redstone. */
    static AcceleratorBlockEntity accelerator(GameTestHelper helper, int length) {
        helper.setBlock(ACCELERATOR, RIRegistries.ACCELERATOR.get().defaultBlockState().setValue(AcceleratorBlock.FACING, Direction.WEST));
        helper.setBlock(ACCELERATOR.west(), Blocks.REDSTONE_BLOCK);
        for (int x = 2; x < 2 + length; x++) {
            for (BlockPos wall : List.of(new BlockPos(x, 1, 3), new BlockPos(x, 3, 3), new BlockPos(x, 2, 2), new BlockPos(x, 2, 4))) {
                helper.setBlock(wall, RIRegistries.ELECTROMAGNET.get());
            }
        }
        helper.setBlock(new BlockPos(2 + length, 2, 3), RIRegistries.ELECTROMAGNET.get());
        return helper.getBlockEntity(ACCELERATOR);
    }

    /** Charges the accelerator (it needs something to fire) and keeps its buffer topped up. */
    static void power(GameTestHelper helper, AcceleratorBlockEntity accelerator) {
        IEnergyStorage in = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(ACCELERATOR), Direction.UP);
        in.receiveEnergy(Integer.MAX_VALUE, false);
        helper.onEachTick(() -> in.receiveEnergy(Integer.MAX_VALUE, false));
    }

    static List<ParticleEntity> particles(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ParticleEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(24));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void acceleratorFiresAParticle(GameTestHelper helper) {
        AcceleratorBlockEntity accelerator = accelerator(helper, 16);
        accelerator.inventory().setStackInSlot(AcceleratorBlockEntity.ITEM, new ItemStack(Items.IRON_INGOT, 2));
        power(helper, accelerator);
        helper.succeedWhen(() -> {
            List<ParticleEntity> found = particles(helper);
            helper.assertTrue(found.size() == 1, found.size() + " particles");
            ParticleEntity p = found.get(0);
            helper.assertTrue(p.getDeltaMovement().x > 0 && p.direction() == Direction.EAST, "not flying east: " + p.getDeltaMovement());
            helper.assertTrue(accelerator.inventory().getStackInSlot(AcceleratorBlockEntity.ITEM).getCount() == 1, "the ingot was not used");
            helper.assertTrue(accelerator.particle() == p, "accelerator lost its particle");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void fullSpeedMakesAntimatter(GameTestHelper helper) {
        AcceleratorBlockEntity accelerator = accelerator(helper, 16);
        // Obsidian is hard: up to 49 mg more each time.
        accelerator.inventory().setStackInSlot(AcceleratorBlockEntity.ITEM, new ItemStack(Items.OBSIDIAN, 4));
        power(helper, accelerator);
        helper.onEachTick(() -> particles(helper).forEach(p -> p.setDeltaMovement(AcceleratorBlockEntity.MAX_VELOCITY, 0, 0)));
        helper.succeedWhen(() -> helper.assertTrue(accelerator.antimatter() >= 5, "no antimatter"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void antimatterFillsCells(GameTestHelper helper) {
        AcceleratorBlockEntity accelerator = accelerator(helper, 4);
        CompoundTag tag = new CompoundTag();
        tag.putInt("antimatter", 130);
        accelerator.loadCustomOnly(tag, helper.getLevel().registryAccess());
        accelerator.inventory().setStackInSlot(AcceleratorBlockEntity.CELL, new ItemStack(RIRegistries.EMPTY_CELL.get(), 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(accelerator.inventory().getStackInSlot(AcceleratorBlockEntity.ANTIMATTER).is(RIRegistries.ANTIMATTER.get()), "no antimatter cell");
            helper.assertTrue(accelerator.antimatter() == 5 && accelerator.inventory().getStackInSlot(AcceleratorBlockEntity.CELL).getCount() == 1, "wrong amounts");
        });
    }

    /** Two fast particles meeting shatter (the accelerator may find dark matter) and leave the tunnel whole. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void particlesCollideAndShatter(GameTestHelper helper) {
        AcceleratorBlockEntity accelerator = accelerator(helper, 4);
        accelerator.inventory().setStackInSlot(AcceleratorBlockEntity.ITEM, new ItemStack(Items.IRON_INGOT));
        power(helper, accelerator);
        ParticleEntity a = new ParticleEntity(helper.getLevel(), helper.absolutePos(new BlockPos(3, 2, 3)), helper.absolutePos(ACCELERATOR), Direction.EAST);
        ParticleEntity b = new ParticleEntity(helper.getLevel(), helper.absolutePos(new BlockPos(5, 2, 3)), helper.absolutePos(ACCELERATOR), Direction.WEST);
        a.setDeltaMovement(0.8, 0, 0);
        b.setDeltaMovement(-0.8, 0, 0);
        helper.getLevel().addFreshEntity(a);
        helper.getLevel().addFreshEntity(b);
        helper.succeedWhen(() -> {
            helper.assertTrue(a.isRemoved() && b.isRemoved(), "particles still flying");
            helper.assertTrue(a.collided() || b.collided(), "neither shattered");
            for (int x = 2; x <= 5; x++) {
                helper.assertBlockPresent(RIRegistries.ELECTROMAGNET.get(), new BlockPos(x, 3, 3));
            }
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void particleNeedsATunnel(GameTestHelper helper) {
        AcceleratorBlockEntity accelerator = accelerator(helper, 4);
        helper.setBlock(new BlockPos(2, 3, 3), Blocks.STONE);
        accelerator.inventory().setStackInSlot(AcceleratorBlockEntity.ITEM, new ItemStack(Items.IRON_INGOT));
        power(helper, accelerator);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(particles(helper).isEmpty(), "fired without a ceiling of electromagnets");
            helper.assertTrue(accelerator.inventory().getStackInSlot(AcceleratorBlockEntity.ITEM).getCount() == 1, "used the ingot");
            helper.succeed();
        });
    }

    static QuantumAssemblerBlockEntity assembler(GameTestHelper helper, BlockPos pos, ItemStack target) {
        helper.setBlock(pos, RIRegistries.QUANTUM_ASSEMBLER.get());
        QuantumAssemblerBlockEntity assembler = helper.getBlockEntity(pos);
        for (int i = QuantumAssemblerBlockEntity.FIRST_DARK_MATTER; i < QuantumAssemblerBlockEntity.TARGET; i++) {
            assembler.inventory().setStackInSlot(i, new ItemStack(RIRegistries.DARK_MATTER.get(), 2));
        }
        assembler.inventory().setStackInSlot(QuantumAssemblerBlockEntity.TARGET, target);
        return assembler;
    }

    /** A milligram of antimatter going off fills a generator in sight of it; one behind a wall gets nothing. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fulminationCatchesAntimatter(GameTestHelper helper) {
        BlockPos open = new BlockPos(6, 2, 3);
        BlockPos hidden = new BlockPos(12, 2, 3);
        helper.setBlock(open, RIRegistries.FULMINATION.get());
        helper.setBlock(hidden, RIRegistries.FULMINATION.get());
        for (int y = 1; y <= 4; y++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(new BlockPos(11, y, z), Blocks.OBSIDIAN);
            }
        }
        FulminationBlock.Tile a = helper.getBlockEntity(open);
        FulminationBlock.Tile b = helper.getBlockEntity(hidden);
        FulminationBlock.absorb(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 2, 3))), 4, 2e15);
        helper.assertTrue(a.energy() == FulminationBlock.Tile.capacity(), "open generator has " + a.energy());
        FulminationBlock.absorb(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(new BlockPos(9, 2, 3))), 4, 2e15);
        helper.assertTrue(b.energy() == 0, "hidden generator has " + b.energy());
        helper.succeed();
    }

    /** A full generator next to a working assembler powers a tick of its job, as in the original. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fulminationPowersTheAssembler(GameTestHelper helper) {
        BlockPos gen = new BlockPos(5, 2, 3);
        helper.setBlock(gen, RIRegistries.FULMINATION.get());
        QuantumAssemblerBlockEntity assembler = assembler(helper, gen.east(), new ItemStack(Items.IRON_INGOT));
        FulminationBlock.absorb(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(gen.west(2))), 4, 2e15);
        helper.succeedWhen(() -> helper.assertTrue(assembler.timer() > 0, "assembler idle"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 2500)
    public static void quantumAssemblerCopies(GameTestHelper helper) {
        BlockPos pos = new BlockPos(5, 2, 3);
        QuantumAssemblerBlockEntity assembler = assembler(helper, pos, new ItemStack(Items.IRON_INGOT));
        IEnergyStorage in = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.onEachTick(() -> in.receiveEnergy(Integer.MAX_VALUE, false));
        helper.succeedWhen(() -> {
            helper.assertTrue(assembler.target().getCount() == 2, "no copy yet (" + assembler.timer() + " ticks left)");
            for (int i = QuantumAssemblerBlockEntity.FIRST_DARK_MATTER; i < QuantumAssemblerBlockEntity.TARGET; i++) {
                helper.assertTrue(assembler.inventory().getStackInSlot(i).getCount() == 1, "slot " + i + " not used");
            }
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void quantumAssemblerCopiesItemsOnly(GameTestHelper helper) {
        helper.assertTrue(QuantumAssemblerBlockEntity.canCopy(new ItemStack(Items.DIAMOND)), "won't copy a diamond");
        helper.assertFalse(QuantumAssemblerBlockEntity.canCopy(new ItemStack(Items.STONE)), "copies blocks");
        helper.assertFalse(QuantumAssemblerBlockEntity.canCopy(new ItemStack(Items.BUNDLE)), "copies bundles");
        QuantumAssemblerBlockEntity assembler = assembler(helper, new BlockPos(5, 2, 3), new ItemStack(Items.STONE));
        IEnergyStorage in = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(new BlockPos(5, 2, 3)), Direction.UP);
        helper.assertTrue(in.receiveEnergy(Integer.MAX_VALUE, true) == 0, "takes power with nothing to do");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(assembler.timer() == 0, "working on stone");
            helper.succeed();
        });
    }
}
