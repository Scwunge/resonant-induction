package resonantinduction.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import resonantinduction.ResonantInduction;
import resonantinduction.atomic.Radiation;
import resonantinduction.registry.RIRegistries;
import resonantinduction.resource.MachineRecipes;
import resonantinduction.resource.Materials;

@GameTestHolder(ResonantInduction.MODID)
@PrefixGameTestTemplate(false)
public class AtomicGameTests {
    static final String TEMPLATE = TeslaGameTests.TEMPLATE;

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void radiationSickensAndHurts(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 1, 3));
        Radiation.expose(cow, 2);
        helper.assertTrue(cow.hasEffect(RIRegistries.RADIATION), "no radiation effect");
        float health = cow.getHealth();
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(cow.getHealth() < health, "radiation did no harm");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fullHazmatSuitKeepsRadiationOff(GameTestHelper helper) {
        ArmorStand suited = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 1, 3));
        ArmorStand partly = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(4, 1, 3));
        suited.setItemSlot(EquipmentSlot.HEAD, new ItemStack(RIRegistries.HAZMAT_MASK.get()));
        suited.setItemSlot(EquipmentSlot.CHEST, new ItemStack(RIRegistries.HAZMAT_BODY.get()));
        suited.setItemSlot(EquipmentSlot.LEGS, new ItemStack(RIRegistries.HAZMAT_LEGGINGS.get()));
        suited.setItemSlot(EquipmentSlot.FEET, new ItemStack(RIRegistries.HAZMAT_BOOTS.get()));
        partly.setItemSlot(EquipmentSlot.HEAD, new ItemStack(RIRegistries.HAZMAT_MASK.get()));
        Radiation.expose(suited, 3);
        Radiation.expose(partly, 3);
        helper.assertTrue(!suited.hasEffect(RIRegistries.RADIATION), "a full suit let radiation through");
        helper.assertTrue(suited.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 1, "the suit did not wear");
        helper.assertTrue(partly.hasEffect(RIRegistries.RADIATION), "a mask alone kept radiation off");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void toxicWasteWithersAndIrradiates(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        SmeltingGameTests.wallIn(helper, pos, null);
        helper.setBlock(pos, RIRegistries.TOXIC_WASTE_BLOCK.get());
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, pos);
        helper.succeedWhen(() -> {
            helper.assertTrue(cow.hasEffect(RIRegistries.RADIATION), "no radiation from toxic waste");
            helper.assertTrue(cow.getHealth() < cow.getMaxHealth(), "toxic waste did no harm");
        });
    }

    /** Dropped antimatter annihilates when it expires: it blows a hole and irradiates what's near. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void droppedAntimatterAnnihilates(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos.east(), Blocks.DIRT);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(6, 1, 6));
        BlockPos abs = helper.absolutePos(pos);
        ItemEntity cell = new ItemEntity(helper.getLevel(), abs.getX() + 0.5, abs.getY() + 0.2, abs.getZ() + 0.5, new ItemStack(RIRegistries.ANTIMATTER.get()));
        cell.lifespan = 10;
        helper.getLevel().addFreshEntity(cell);
        helper.succeedWhen(() -> {
            helper.assertTrue(!cell.isAlive(), "cell still there");
            helper.assertBlockPresent(Blocks.AIR, pos.east());
            helper.assertTrue(!cow.isAlive() || cow.hasEffect(RIRegistries.RADIATION), "no radiation from the annihilation");
        });
    }

    /** Uranium has its own processing: it stays out of the rubble and dust chain, as in the original. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void uraniumStaysOutOfTheDustChain(GameTestHelper helper) {
        helper.assertTrue(!Materials.all().contains("uranium"), "uranium is an ore chain metal");
        helper.assertTrue(!MachineRecipes.canCrush(new ItemStack(RIRegistries.URANIUM_ORE.get())), "uranium ore crushes into rubble");
        helper.assertTrue(new ItemStack(RIRegistries.URANIUM_ORE.get()).is(Materials.oreItemTag("uranium")), "uranium ore lacks c:ores/uranium");
        helper.assertTrue(MachineRecipes.canCrush(new ItemStack(Items.IRON_ORE)), "iron ore no longer crushes");
        helper.succeed();
    }
}
