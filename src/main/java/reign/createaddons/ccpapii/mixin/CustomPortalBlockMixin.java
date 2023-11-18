package reign.createaddons.ccpapii.mixin;

import net.kyrptonaught.customportalapi.CustomPortalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomPortalBlock.class)
public abstract class CustomPortalBlockMixin extends Block {
	public CustomPortalBlockMixin(Properties properties) {
		super(properties);
	}

	// This somehow fixed the portal messing up the position of passengers when teleporting from the other side
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
		if (entity.isPassenger() || entity.isVehicle() || !entity.canChangeDimensions())
			ci.cancel();
	}
}
