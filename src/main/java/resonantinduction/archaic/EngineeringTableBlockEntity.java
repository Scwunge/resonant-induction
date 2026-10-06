package resonantinduction.archaic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import resonantinduction.registry.RIRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Engineering Table, as the original: a crafting grid on top of the table. The grid is also a pattern: crafting takes the
 * ingredients from chests beside the table and from the crafter's inventory first, and only from the grid when they run out. An
 * Imprint in the centre (with no recipe laid out) crafts whatever is on it from those same stores.
 */
public class EngineeringTableBlockEntity extends BlockEntity {
    public static final int CENTER = 4;

    private final NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
    private boolean searchInventories = true;

    public EngineeringTableBlockEntity(BlockPos pos, BlockState state) {
        super(RIRegistries.ENGINEERING_TABLE_BE.get(), pos, state);
    }

    public NonNullList<ItemStack> grid() {
        return grid;
    }

    public boolean searchInventories() {
        return searchInventories;
    }

    public void toggleSearch() {
        searchInventories = !searchInventories;
        changed();
    }

    public void setSlot(int slot, ItemStack stack) {
        grid.set(slot, stack);
        changed();
    }

    void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private Optional<RecipeHolder<CraftingRecipe>> gridRecipe() {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    /** The item a craft would make now, without the crafter's inventory (what's shown on the sides). */
    public ItemStack output() {
        if (level == null) {
            return ItemStack.EMPTY;
        }
        Optional<RecipeHolder<CraftingRecipe>> recipe = gridRecipe();
        if (recipe.isPresent()) {
            return recipe.get().value().assemble(CraftingInput.of(3, 3, grid), level.registryAccess());
        }
        ImprintCraft plan = imprintCraft(null);
        return plan == null ? ItemStack.EMPTY : plan.result;
    }

    /** The stores a craft may take from, in the original's order: neighbouring inventories, then the crafter. The grid comes last. */
    private List<IItemHandler> stores(@Nullable Player player) {
        List<IItemHandler> out = new ArrayList<>();
        if (searchInventories) {
            for (Direction d : Direction.values()) {
                IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
                if (h != null) {
                    out.add(h);
                }
            }
            if (player != null) {
                out.add(new PlayerMainInvWrapper(player.getInventory()));
            }
        }
        return out;
    }

    private record ImprintCraft(ItemStack result, List<Predicate<ItemStack>> ingredients) {}

    /** A recipe for something on the centre imprint whose ingredients the stores and the rest of the grid can supply. */
    @Nullable
    private ImprintCraft imprintCraft(@Nullable Player player) {
        ItemStack imprint = grid.get(CENTER);
        if (!(imprint.getItem() instanceof ImprintItem)) {
            return null;
        }
        for (ItemStack wanted : ImprintItem.filters(imprint)) {
            for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                ItemStack result = holder.value().getResultItem(level.registryAccess());
                if (!ItemStack.isSameItem(result, wanted)) {
                    continue;
                }
                List<Predicate<ItemStack>> needed = new ArrayList<>();
                for (Ingredient i : holder.value().getIngredients()) {
                    if (!i.isEmpty()) {
                        needed.add(i);
                    }
                }
                if (!needed.isEmpty() && take(needed, player, true)) {
                    return new ImprintCraft(result.copy(), needed);
                }
            }
        }
        return null;
    }

    /**
     * Finds one item for each ingredient, from the stores and then the grid (never the centre imprint); takes them unless
     * simulating. Returns false (taking nothing) if any is missing.
     */
    private boolean take(List<Predicate<ItemStack>> needed, @Nullable Player player, boolean simulate) {
        return take(needed, null, player, simulate);
    }

    /** As above; {@code home}, if given, is the one grid square each ingredient may fall back to (a laid-out pattern). */
    private boolean take(List<Predicate<ItemStack>> needed, @Nullable List<Integer> home, @Nullable Player player, boolean simulate) {
        List<IItemHandler> stores = stores(player);
        Map<Long, Integer> used = new HashMap<>();
        List<long[]> picks = new ArrayList<>();
        outer:
        for (int k = 0; k < needed.size(); k++) {
            Predicate<ItemStack> want = needed.get(k);
            for (int s = 0; s < stores.size(); s++) {
                IItemHandler h = stores.get(s);
                for (int slot = 0; slot < h.getSlots(); slot++) {
                    long key = ((long) s << 32) | slot;
                    int n = used.getOrDefault(key, 0);
                    ItemStack in = h.getStackInSlot(slot);
                    if (!in.isEmpty() && want.test(in) && h.extractItem(slot, n + 1, true).getCount() > n) {
                        used.put(key, n + 1);
                        picks.add(new long[] {s, slot});
                        continue outer;
                    }
                }
            }
            for (int g = 0; g < 9; g++) {
                if ((g == CENTER && grid.get(CENTER).getItem() instanceof ImprintItem) || (home != null && home.get(k) != g)) {
                    continue;
                }
                long key = -1 - g;
                int n = used.getOrDefault(key, 0);
                ItemStack in = grid.get(g);
                if (in.getCount() > n && want.test(in)) {
                    used.put(key, n + 1);
                    picks.add(new long[] {-1, g});
                    continue outer;
                }
            }
            return false;
        }
        if (!simulate) {
            for (long[] pick : picks) {
                ItemStack taken = pick[0] < 0 ? grid.get((int) pick[1]).split(1) : stores.get((int) pick[0]).extractItem((int) pick[1], 1, false);
                giveRemainder(taken, player);
            }
        }
        return true;
    }

    /** Buckets and the like come back from what's used up. */
    private void giveRemainder(ItemStack used, @Nullable Player player) {
        ItemStack rest = used.getCraftingRemainingItem();
        if (rest.isEmpty()) {
            return;
        }
        if (player == null || !player.addItem(rest)) {
            Block.popResource(level, worldPosition.above(), rest);
        }
    }

    /** Crafts once for {@code player}; returns the result (empty if nothing could be made). */
    public ItemStack craft(@Nullable Player player) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = gridRecipe();
        ItemStack result;
        List<Predicate<ItemStack>> needed = new ArrayList<>();
        List<Integer> home = null;
        if (recipe.isPresent()) {
            result = recipe.get().value().assemble(CraftingInput.of(3, 3, grid), level.registryAccess());
            // The grid is the pattern: each of its items is taken from the stores if they have it, else from its own square.
            home = new ArrayList<>();
            for (int g = 0; g < 9; g++) {
                ItemStack want = grid.get(g).copy();
                if (!want.isEmpty()) {
                    needed.add(in -> ItemStack.isSameItemSameComponents(in, want));
                    home.add(g);
                }
            }
        } else {
            ImprintCraft plan = imprintCraft(player);
            if (plan == null) {
                return ItemStack.EMPTY;
            }
            result = plan.result;
            needed = plan.ingredients;
        }
        if (result.isEmpty() || !take(needed, home, player, false)) {
            return ItemStack.EMPTY;
        }
        changed();
        return result;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(grid);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(grid));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Items");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, grid, registries);
        tag.putBoolean("searchInventories", searchInventories);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 9; i++) {
            grid.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, grid, registries);
        searchInventories = !tag.contains("searchInventories") || tag.getBoolean("searchInventories");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
