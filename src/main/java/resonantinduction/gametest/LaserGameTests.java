package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.charger.ChargerBlockEntity;
import resonantinduction.laser.MiningLaserItem;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class LaserGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    @GameTest(template = TEMPLATE)
    public static void chargerFillsAnItem(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, RIRegistries.CHARGER.get());
        ChargerBlockEntity charger = helper.getBlockEntity(pos);
        charger.setItem(new ItemStack(RIRegistries.MINING_LASER.get()));
        IEnergyStorage port = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.DOWN);
        helper.assertTrue(port != null, "charger has no energy port");
        int accepted = port.receiveEnergy(3000, false);
        IEnergyStorage item = charger.getItem().getCapability(Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(accepted == 3000 && item.getEnergyStored() == 3000, "laser holds " + item.getEnergyStored() + " FE");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), Direction.UP) == null,
                "charger should not take power through its face");
        helper.succeed();
    }

    /** A survival player with a charged laser, looking straight at the middle of {@code target}. */
    @SuppressWarnings("removal")
    static ServerPlayer aim(GameTestHelper helper, BlockPos stand, BlockPos target, int mode) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(stand));
        player.moveTo(feet.x, feet.y, feet.z);
        Vec3 look = Vec3.atCenterOf(helper.absolutePos(target)).subtract(player.getEyePosition());
        float yaw = (float) (Math.toDegrees(Math.atan2(-look.x, look.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z))));
        player.moveTo(feet.x, feet.y, feet.z, yaw, pitch);
        player.setYHeadRot(yaw);
        ItemStack laser = new ItemStack(RIRegistries.MINING_LASER.get());
        laser.set(RIRegistries.ENERGY.get(), 100000);
        laser.set(RIRegistries.LASER_MODE.get(), mode);
        player.setItemInHand(InteractionHand.MAIN_HAND, laser);
        return player;
    }

    static void fire(GameTestHelper helper, ServerPlayer player, int ticks) {
        ItemStack laser = player.getMainHandItem();
        int duration = laser.getUseDuration(player);
        for (int t = 0; t < ticks; t++) {
            laser.getItem().onUseTick(helper.getLevel(), player, laser, duration - t);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void removeModeCutsOutBlocks(GameTestHelper helper) {
        BlockPos target = new BlockPos(6, 1, 3);
        helper.setBlock(target, Blocks.STONE);
        ServerPlayer player = aim(helper, new BlockPos(1, 1, 3), target, MiningLaserItem.MODE_REMOVE);
        fire(helper, player, 30);
        helper.assertBlockPresent(Blocks.AIR, target);
        AABB around = new AABB(helper.absolutePos(target)).inflate(1);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(Items.COBBLESTONE)).isEmpty(),
                "stone cut by the laser dropped no cobblestone");
        int left = player.getMainHandItem().getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored();
        helper.assertTrue(left < 100000, "firing used no energy");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void smeltModeTurnsSandToGlass(GameTestHelper helper) {
        BlockPos target = new BlockPos(6, 1, 3);
        helper.setBlock(target, Blocks.SAND);
        helper.setBlock(target.below(), Blocks.STONE);
        ServerPlayer player = aim(helper, new BlockPos(1, 1, 3), target, MiningLaserItem.MODE_SMELT);
        fire(helper, player, 10);
        helper.assertBlockPresent(Blocks.GLASS, target);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void emptyLaserStopsFiring(GameTestHelper helper) {
        BlockPos target = new BlockPos(6, 1, 3);
        helper.setBlock(target, Blocks.STONE);
        ServerPlayer player = aim(helper, new BlockPos(1, 1, 3), target, MiningLaserItem.MODE_REMOVE);
        player.getMainHandItem().set(RIRegistries.ENERGY.get(), 0);
        fire(helper, player, 30);
        helper.assertBlockPresent(Blocks.STONE, target);
        helper.succeed();
    }
}
