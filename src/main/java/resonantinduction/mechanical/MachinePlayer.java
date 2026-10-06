package resonantinduction.mechanical;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

/** The fake player machines act as, so claim and protection mods can refuse their block changes. */
public final class MachinePlayer {
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("52694d61-6368-696e-6550-6c6179657200"), "[Resonant Induction]");

    private MachinePlayer() {}

    public static FakePlayer get(ServerLevel level) {
        return FakePlayerFactory.get(level, PROFILE);
    }

    /** Fires a break event for a machine breaking or moving a block. True if it may go ahead. */
    public static boolean mayBreak(ServerLevel level, BlockPos pos, BlockState state) {
        return !CommonHooks.fireBlockBreak(level, GameType.SURVIVAL, get(level), pos, state).isCanceled();
    }
}
