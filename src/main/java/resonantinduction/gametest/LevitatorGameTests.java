package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.levitator.LevitatorBlock;
import resonantinduction.levitator.LevitatorBlockEntity;
import resonantinduction.registry.RIRegistries;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class LevitatorGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    static LevitatorBlockEntity levitator(GameTestHelper helper, BlockPos pos, Direction facing, boolean push) {
        helper.setBlock(pos, RIRegistries.LEVITATOR.get().defaultBlockState().setValue(LevitatorBlock.FACING, facing));
        LevitatorBlockEntity be = helper.getBlockEntity(pos);
        if (push) {
            be.toggleMode();
        }
        return be;
    }

    static Container chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return helper.getBlockEntity(pos);
    }

    static int count(Container container) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            n += container.getItem(i).getCount();
        }
        return n;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void pushesAlongBeamToFacingLevitator(GameTestHelper helper) {
        Container source = chest(helper, new BlockPos(1, 1, 3));
        source.setItem(0, new ItemStack(Items.DIAMOND, 4));
        levitator(helper, new BlockPos(2, 1, 3), Direction.EAST, true);
        levitator(helper, new BlockPos(9, 1, 3), Direction.WEST, false);
        Container target = chest(helper, new BlockPos(10, 1, 3));
        helper.succeedWhen(() -> helper.assertTrue(count(target) == 4 && count(source) == 0,
                "expected 4 diamonds moved, target has " + count(target)));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void linkedLevitatorsCarryAroundCorners(GameTestHelper helper) {
        Container source = chest(helper, new BlockPos(1, 1, 1));
        source.setItem(0, new ItemStack(Items.EMERALD, 3));
        LevitatorBlockEntity pusher = levitator(helper, new BlockPos(2, 1, 1), Direction.EAST, true);
        // A wall in the way forces the path to bend.
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z <= 3; z++) {
                helper.setBlock(new BlockPos(4, y, z), Blocks.STONE);
            }
        }
        levitator(helper, new BlockPos(7, 1, 2), Direction.NORTH, false);
        Container target = chest(helper, new BlockPos(7, 1, 3));
        helper.assertTrue(pusher.linkTo(GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(7, 1, 2)))), "link failed");
        helper.succeedWhen(() -> helper.assertTrue(count(target) == 3, "expected 3 emeralds delivered, got " + count(target)));
    }

    @GameTest(template = TEMPLATE)
    public static void redstoneTurnsItOff(GameTestHelper helper) {
        Container source = chest(helper, new BlockPos(1, 1, 3));
        source.setItem(0, new ItemStack(Items.DIAMOND, 4));
        levitator(helper, new BlockPos(2, 1, 3), Direction.EAST, true);
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(count(source) == 4, "a powered levitator still pushed items out");
            helper.assertBlockProperty(new BlockPos(2, 1, 3), LevitatorBlock.ACTIVE, false);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void pullModeStoresItems(GameTestHelper helper) {
        Container target = chest(helper, new BlockPos(3, 1, 3));
        levitator(helper, new BlockPos(3, 2, 3), Direction.UP, false);
        helper.spawnItem(Items.GOLD_INGOT, 3.5f, 2.2f, 3.5f);
        helper.succeedWhen(() -> helper.assertTrue(count(target) == 1, "dropped item was not pulled into the chest"));
    }
}
