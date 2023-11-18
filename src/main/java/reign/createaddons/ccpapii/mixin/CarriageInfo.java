package reign.createaddons.ccpapii.mixin;

import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.kyrptonaught.customportalapi.interfaces.EntityInCustomPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import reign.createaddons.ccpapii.CCPAPII;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(DimensionalCarriageEntity.class)
public abstract class CarriageInfo {
	@Inject(method = "updatePassengerLoadout",
			at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;addSittingPassenger(Lnet/minecraft/world/entity/Entity;I)V", ordinal = 0),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void cLogAdd(CallbackInfo ci, Entity entity, CarriageContraptionEntity cce, ServerLevel sLevel, Set<Integer> loadedPassengers, int min, int max, Iterator ite, Map.Entry<Integer,
			CompoundTag> entry, Integer seatId, List<BlockPos> seats, BlockPos localPos, CompoundTag tag, Entity passenger, ResourceKey<Level> passengerDimension) {
		CCPAPII.LOGGER.info("ADDING PASSENGER: " + passenger.getName());
	}

	@Inject(method = "dismountPlayer", at = @At(value = "HEAD"))
	private void cLog(ServerLevel sLevel, ServerPlayer sp, Integer seat, boolean capture, CallbackInfo ci) {
		CCPAPII.LOGGER.info("DISMOUNTED, CAPTURE = " + capture);
	}

	/*@Inject(method = "dismountPlayer",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", ordinal = 0),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void logThings(ServerLevel sLevel, ServerPlayer sp, Integer seat, boolean capture, CallbackInfo ci,
						   CompoundTag tag, Iterator ite, Map.Entry<ResourceKey<Level>, DimensionalCarriageEntity> other,
						   DimensionalCarriageEntity otherDce, Vec3 loc, ServerLevel level) {
		CCPAPII.LOGGER.info("TELEPORTING PLAYER " + sp.getName() + " TO " + other.getKey());
	}*/

	@Inject(method = "dismountPlayer",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setPortalCooldown()V"),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void setCustomPortalCooldown(ServerLevel sLevel, ServerPlayer sp, Integer seat, boolean capture, CallbackInfo ci,
						   CompoundTag tag, Iterator ite, Map.Entry<ResourceKey<Level>, DimensionalCarriageEntity> other,
						   DimensionalCarriageEntity otherDce, Vec3 loc, ServerLevel level) {
		((EntityInCustomPortal) sp).setDidTP(false);
	}

	@Redirect(
			method = "dismountPlayer",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", ordinal = 0)
	)
	private void teleportTo(ServerPlayer sp, ServerLevel newLevel, double x, double y, double z, float xRot, float yRot) {
		FabricDimensions.teleport(sp, newLevel, new PortalInfo(new Vec3(x, y, z), sp.getDeltaMovement(), xRot, yRot));
		CCPAPII.LOGGER.info("TELEPORTING PLAYER " + sp.getName().getString(256));
		return;
	}

	@Inject(method = "dismountPlayer", at = @At(value = "JUMP", opcode = Opcodes.GOTO, ordinal = 0))
	private void goto0(ServerLevel sLevel, ServerPlayer sp, Integer seat, boolean capture, CallbackInfo ci) {
		CCPAPII.LOGGER.info("GOTO line: \"otherDce == this\"");
	}

	@Inject(method = "dismountPlayer", at = @At(value = "JUMP", opcode = Opcodes.GOTO, ordinal = 1))
	private void goto1(ServerLevel sLevel, ServerPlayer sp, Integer seat, boolean capture, CallbackInfo ci) {
		CCPAPII.LOGGER.info("GOTO line: Same worlds");
	}

	@Inject(method = "dismountPlayer", at = @At(value = "JUMP", opcode = Opcodes.GOTO, ordinal = 3))
	private void goto3(ServerLevel sLevel, ServerPlayer sp, Integer seat, boolean capture, CallbackInfo ci) {
		CCPAPII.LOGGER.info("GOTO line: \"loc == null\"");
	}
}
