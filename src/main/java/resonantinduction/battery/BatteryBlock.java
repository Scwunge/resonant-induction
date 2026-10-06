package resonantinduction.battery;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.List;
import java.util.Map;

/**
 * Battery cell, three tiers. Neighbouring batteries join visually (frames merge) and share energy. LEVEL (0-8) is how many
 * of its coils glow.
 */
public class BatteryBlock extends BaseEntityBlock {
    public static final MapCodec<BatteryBlock> CODEC = simpleCodec(BatteryBlock::new);
    public static final Map<Direction, BooleanProperty> CONNECTED = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 2);
    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public BatteryBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(LEVEL, 0).setValue(TIER, 0);
        for (BooleanProperty p : CONNECTED.values()) {
            state = state.setValue(p, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
        builder.add(LEVEL, TIER);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    private BlockState withConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = state.setValue(CONNECTED.get(d), level.getBlockState(pos.relative(d)).is(this));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int tier = context.getItemInHand().getOrDefault(RIRegistries.BATTERY_TIER.get(), 0);
        return withConnections(defaultBlockState().setValue(TIER, Math.max(0, Math.min(2, tier))), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTED.get(direction), neighborState.is(this));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            battery.setEnergy(stack.getOrDefault(RIRegistries.ENERGY.get(), 0));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, RIRegistries.BATTERY_BE.get(), BatteryBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(WRENCHES)) {
            return cycle(level, pos, player, hit.getDirection()) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            return cycle(level, pos, player, hit.getDirection()) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BatteryBlockEntity battery) {
            long energy = 0;
            long capacity = 0;
            List<BatteryBlockEntity> group = battery.group();
            for (BatteryBlockEntity b : group) {
                energy += b.energy();
                capacity += b.capacity();
            }
            player.displayClientMessage(Component.translatable("message.resonantinduction.battery.status", energy, capacity, group.size()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean cycle(Level level, BlockPos pos, Player player, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof BatteryBlockEntity battery)) {
            return false;
        }
        if (!level.isClientSide) {
            byte mode = battery.cycleIo(side);
            String key = mode == BatteryBlockEntity.IO_INPUT ? "input" : mode == BatteryBlockEntity.IO_OUTPUT ? "output" : "none";
            player.displayClientMessage(Component.translatable("message.resonantinduction.battery.io",
                    Component.translatable("direction.resonantinduction." + side.getSerializedName()), Component.translatable("message.resonantinduction.battery.io." + key)), true);
        }
        return true;
    }

    /** Drops itself with its tier and charge. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        stack.set(RIRegistries.BATTERY_TIER.get(), state.getValue(TIER));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BatteryBlockEntity battery && battery.energy() > 0) {
            stack.set(RIRegistries.ENERGY.get(), battery.energy());
        }
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        stack.set(RIRegistries.BATTERY_TIER.get(), state.getValue(TIER));
        return stack;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return Math.round(state.getValue(LEVEL) * 15f / 8f);
    }
}
