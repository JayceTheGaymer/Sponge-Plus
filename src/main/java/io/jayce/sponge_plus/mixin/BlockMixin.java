package io.jayce.sponge_plus.mixin;

/*? if fabric {*/
/*import io.jayce.sponge_plus.SpongeDryingHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
*/
/*?}*/
/*? if fabric && >=26.2 {*/
/*import net.minecraft.world.level.redstone.Orientation;
*/
/*?}*/
/*? if fabric {*/
/*import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fabric has no equivalent of NeoForge's NeighborNotifyEvent, so hook the vanilla callback instead.
// The fifth parameter became an Orientation in 26.2, and is unused either way.
@Mixin(BlockBehaviour.class)
public abstract class BlockMixin {
*/
/*?}*/

    /*? if fabric && <26.2 {*/
    /*@Inject(method = "neighborChanged", at = @At("HEAD"))
    private void sponge_plus$onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            SpongeDryingHandler.handleNeighborChange(serverLevel, pos, state);
        }
    }*/
    /*?}*/

    /*? if fabric && >=26.2 {*/
    /*@Inject(method = "neighborChanged", at = @At("HEAD"))
    private void sponge_plus$onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            SpongeDryingHandler.handleNeighborChange(serverLevel, pos, state);
        }
    }*/
    /*?}*/

/*? if fabric {*/
/*}*/
/*?}*/
