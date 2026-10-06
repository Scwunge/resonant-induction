package resonantinduction.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;
import resonantinduction.RIConfig;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared logic of framed (centre) and flat (face) wires: connection flags, insulation, switch levers, shocks.
 * Wool insulates a wire in its colour, dye recolours insulation, shears strip it, a lever makes a switch wire (redstone on =
 * conducts) and using a lever again takes it back.
 */
public abstract class WireBlock extends BaseEntityBlock {
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final BooleanProperty INSULATED = BooleanProperty.create("insulated");

    private final WireMaterial material;

    protected WireBlock(WireMaterial material, Properties properties) {
        super(properties);
        this.material = material;
        BlockState state = stateDefinition.any().setValue(INSULATED, false);
        for (BooleanProperty side : SIDES.values()) {
            state = state.setValue(side, false);
        }
        registerDefaultState(state);
    }

    public WireMaterial material() {
        return material;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        SIDES.values().forEach(builder::add);
        builder.add(INSULATED);
    }

    /** Whether this wire can reach out in {@code dir} at all (all six ways for framed, the plane and its support for flat). */
    public abstract boolean reaches(BlockState state, Direction dir);

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WireBlockEntity(pos, state);
    }

    // ---- connections ----

    /** Wire-to-wire link from {@code pos} towards {@code dir}. */
    public static boolean wiresConnect(Level level, BlockPos pos, Direction dir) {
        if (!(level.getBlockEntity(pos) instanceof WireBlockEntity a) || !(level.getBlockEntity(pos.relative(dir)) instanceof WireBlockEntity b)) {
            return false;
        }
        BlockState sa = a.getBlockState();
        BlockState sb = b.getBlockState();
        return sa.getBlock() instanceof WireBlock wa && sb.getBlock() instanceof WireBlock wb
                && wa.reaches(sa, dir) && wb.reaches(sb, dir.getOpposite())
                && a.compatibleWith(b) && a.conducts() && b.conducts();
    }

    /** Wire-to-machine link: a non-wire block that handles FE on the facing side. */
    public static boolean connectsToMachine(Level level, BlockPos pos, Direction dir) {
        if (!(level.getBlockEntity(pos) instanceof WireBlockEntity wire) || !(wire.getBlockState().getBlock() instanceof WireBlock block)
                || !block.reaches(wire.getBlockState(), dir) || !wire.conducts()) {
            return false;
        }
        BlockPos other = pos.relative(dir);
        if (level.getBlockEntity(other) instanceof WireBlockEntity || !level.isLoaded(other)) {
            return false;
        }
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, other, dir.getOpposite()) != null;
    }

    /** Recomputes the connection flags of the wire at {@code pos} (if it is one) and drops cached networks. */
    public static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WireBlock) || !(level.getBlockEntity(pos) instanceof WireBlockEntity wire)) {
            return;
        }
        BlockState updated = state.setValue(INSULATED, wire.isInsulated());
        for (Direction d : Direction.values()) {
            updated = updated.setValue(SIDES.get(d), wiresConnect(level, pos, d) || connectsToMachine(level, pos, d));
        }
        WireNetwork.invalidate(level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            level.invalidateCapabilities(pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            refresh(level, pos);
            for (Direction d : Direction.values()) {
                refresh(level, pos.relative(d));
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide) {
            refresh(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            WireNetwork.invalidate(level, pos);
            for (Direction d : Direction.values()) {
                refresh(level, pos.relative(d));
            }
        }
    }

    // ---- interaction ----

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WireBlockEntity wire)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        DyeColor wool = woolColor(stack);
        if (wool != null && !wire.isInsulated()) {
            if (!level.isClientSide) {
                wire.setInsulation(true, wool);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 1f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        DyeColor dye = DyeColor.getColor(stack);
        if (dye != null && wool == null && wire.isInsulated()) {
            if (!level.isClientSide) {
                wire.setInsulation(true, dye);
                stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof ShearsItem && wire.isInsulated()) {
            if (!level.isClientSide) {
                ItemStack insulation = woolOf(wire.color());
                wire.setInsulation(false, WireBlockEntity.DEFAULT_COLOR);
                if (!player.getAbilities().instabuild) {
                    popResource(level, pos, insulation);
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1f, 1f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.LEVER)) {
            if (!level.isClientSide) {
                if (wire.isSwitched()) {
                    wire.setSwitched(false);
                    if (!player.getAbilities().instabuild) {
                        popResource(level, pos, new ItemStack(Items.LEVER));
                    }
                } else {
                    wire.setSwitched(true);
                    stack.consume(1, player);
                }
                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    private static DyeColor woolColor(ItemStack stack) {
        if (!stack.is(ItemTags.WOOL)) {
            return null;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_wool") ? DyeColor.byName(path.substring(0, path.length() - 5), DyeColor.WHITE) : DyeColor.WHITE;
    }

    private static ItemStack woolOf(DyeColor color) {
        return new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(color.getName() + "_wool")));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof WireBlockEntity wire) {
            if (wire.isInsulated()) {
                drops.add(woolOf(wire.color()));
            }
            if (wire.isSwitched()) {
                drops.add(new ItemStack(Items.LEVER));
            }
        }
        return drops;
    }

    /** Bare wires carrying power shock whatever touches them. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(INSULATED) || !(level instanceof ServerLevel server) || !(entity instanceof LivingEntity living)
                || !RIConfig.get(RIConfig.WIRE_SHOCK) || material.damage <= 0) {
            return;
        }
        if (WireNetwork.get(level, pos).isLive(level)) {
            DamageSource source = new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RIRegistries.ELECTROCUTION));
            living.hurt(source, material.damage);
        }
    }
}
