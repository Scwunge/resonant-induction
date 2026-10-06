package resonantinduction.atomic.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import resonantinduction.battery.BatteryBlock;
import resonantinduction.registry.RIRegistries;

/** Electric Turbine block, drawn by its renderer. A wrench joins or splits a 3x3 big turbine. */
public class ElectricTurbineBlock extends BaseEntityBlock {
    public static final MapCodec<ElectricTurbineBlock> CODEC = simpleCodec(ElectricTurbineBlock::new);

    public ElectricTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricTurbineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != RIRegistries.ELECTRIC_TURBINE_BE.get()) {
            return null;
        }
        return level.isClientSide ? (l, p, s, be) -> ElectricTurbineBlockEntity.clientTick(l, p, s, (ElectricTurbineBlockEntity) be)
                : (l, p, s, be) -> ElectricTurbineBlockEntity.serverTick(l, p, s, (ElectricTurbineBlockEntity) be);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(BatteryBlock.WRENCHES) || !(level.getBlockEntity(pos) instanceof ElectricTurbineBlockEntity turbine)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            boolean was = turbine.master().formed();
            if (turbine.toggleMultiblock()) {
                player.displayClientMessage(Component.translatable(was ? "message.resonantinduction.turbine.split" : "message.resonantinduction.turbine.joined"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.resonantinduction.turbine.need_square"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
