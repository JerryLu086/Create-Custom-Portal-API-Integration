package reign.createaddons.ccpapii.mixin;

import java.util.Random;

import com.google.common.base.Predicates;
import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;
import com.simibubi.create.content.logistics.trains.TrackPropagator;
import com.simibubi.create.content.logistics.trains.track.TrackBlock;
import com.simibubi.create.content.logistics.trains.track.TrackShape;
import com.simibubi.create.content.logistics.trains.track.TrackTileEntity;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Iterate;

import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.Pair;

import net.kyrptonaught.customportalapi.CustomPortalApiRegistry;
import net.kyrptonaught.customportalapi.CustomPortalBlock;
import net.kyrptonaught.customportalapi.CustomPortalsMod;
import net.kyrptonaught.customportalapi.util.CustomPortalHelper;
import net.kyrptonaught.customportalapi.util.CustomTeleporter;
import net.kyrptonaught.customportalapi.util.PortalLink;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import reign.createaddons.ccpapii.SuperGlueEntityAccessor;

import static com.simibubi.create.content.logistics.trains.track.TrackBlock.HAS_TE;
import static com.simibubi.create.content.logistics.trains.track.TrackBlock.SHAPE;

@Mixin(TrackBlock.class)
public abstract class TrackBlockMixin {
	@Overwrite
	public void tick(BlockState state, ServerLevel level, BlockPos pos, Random p_60465_) {
		TrackPropagator.onRailAdded(level, pos, state);
		if (!state.getValue(SHAPE)
				.isPortal())
			connectToPortal(level, pos, state);
	}

	protected void connectToPortal(ServerLevel level, BlockPos pos, BlockState state) {
		TrackShape shape = state.getValue(SHAPE);
		Direction.Axis portalTest = shape == TrackShape.XO ? Direction.Axis.X : shape == TrackShape.ZO ? Direction.Axis.Z : null;
		if (portalTest == null)
			return;

		boolean pop = false;
		String fail = null;
		BlockPos failPos = null;

		for (Direction d : Iterate.directionsInAxis(portalTest)) {
			BlockPos portalPos = pos.relative(d);
			BlockState portalState = level.getBlockState(portalPos);
			if (!isPortal(portalState.getBlock()))
				continue;

			pop = true;
			Pair<ServerLevel, BlockFace> otherSide = getOtherSide(level, new BlockFace(pos, d));
			if (otherSide == null) {
				fail = "missing";
				continue;
			}

			ServerLevel otherLevel = otherSide.getFirst();
			BlockFace otherTrack = otherSide.getSecond();
			BlockPos otherTrackPos = otherTrack.getPos();
			BlockState existing = otherLevel.getBlockState(otherTrackPos);
			if (!existing.getMaterial()
					.isReplaceable()) {
				fail = "blocked";
				failPos = otherTrackPos;
				continue;
			}

			level.setBlock(pos, state.setValue(SHAPE, TrackShape.asPortal(d))
					.setValue(HAS_TE, true), 3);
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof TrackTileEntity tte)
				tte.bind(otherLevel.dimension(), otherTrackPos);

			otherLevel.setBlock(otherTrackPos, state.setValue(SHAPE, TrackShape.asPortal(otherTrack.getFace()))
					.setValue(HAS_TE, true), 3);
			BlockEntity otherBE = otherLevel.getBlockEntity(otherTrackPos);
			if (otherBE instanceof TrackTileEntity tte)
				tte.bind(level.dimension(), pos);

			pop = false;
		}

		if (!pop)
			return;

		level.destroyBlock(pos, true);

		Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 10, Predicates.alwaysTrue());
		if (player == null)
			return;

		player.displayClientMessage(Components.literal("<!> ").append(Lang.translateDirect("portal_track.failed"))
				.withStyle(ChatFormatting.GOLD), false);
		MutableComponent component = failPos != null
				? Lang.translateDirect("portal_track." + fail, failPos.getX(), failPos.getY(), failPos.getZ())
				: Lang.translateDirect("portal_track." + fail);
		player.displayClientMessage(Components.literal(" - ")
				.withStyle(ChatFormatting.GRAY)
				.append(component.withStyle(st -> st.withColor(0xFFD3B4))), false);
	}

	protected Pair<ServerLevel, BlockFace> getOtherSide(ServerLevel level, BlockFace inboundTrack) {
		BlockPos portalPos = inboundTrack.getConnectedPos();
		BlockState portalState = level.getBlockState(portalPos);

		MinecraftServer minecraftserver = level.getServer();
		ServerLevel otherLevel = null;

		if (portalState.getBlock() instanceof NetherPortalBlock) {
			ResourceKey<Level> resourcekey = level.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
			otherLevel = minecraftserver.getLevel(resourcekey);
		}

		if (portalState.getBlock() instanceof CustomPortalBlock customPortalBlock) {
			PortalLink link = CustomPortalApiRegistry.getPortalLinkFromBase(customPortalBlock.getPortalBase(level, portalPos));
			ResourceKey<Level> resourcekey = level.dimension() == CustomPortalsMod.dims.get(link.dimID) ? CustomPortalsMod.dims.get(link.returnDimID) : CustomPortalsMod.dims.get(link.dimID);;
			otherLevel = minecraftserver.getLevel(resourcekey);
		}

		if (otherLevel == null)
			return null;

		PortalForcer teleporter = otherLevel.getPortalForcer(); // not used on fabric for some reason
		SuperGlueEntity probe = new SuperGlueEntity(level, new AABB(portalPos));
		probe.setYRot(inboundTrack.getFace()
				.toYRot());
		((SuperGlueEntityAccessor) probe).setPortalEntrancePos();

		PortalInfo portalinfo = null;
		if (portalState.getBlock() instanceof NetherPortalBlock) {
			portalinfo = probe.findDimensionEntryPoint(otherLevel);
		}
		if (portalState.getBlock() instanceof CustomPortalBlock customPortalBlock) {
			PortalLink link = CustomPortalApiRegistry.getPortalLinkFromBase(customPortalBlock.getPortalBase(level, portalPos));
			portalinfo = CustomTeleporter.customTPTarget(
					otherLevel, probe, portalPos,
					customPortalBlock.getPortalBase(level, portalPos),
					link.getFrameTester());
		}
		if (portalinfo == null)
			return null;

		BlockPos otherPortalPos = new BlockPos(portalinfo.pos);
		BlockState otherPortalState = otherLevel.getBlockState(otherPortalPos);
		if (otherPortalState.getBlock() != portalState.getBlock())
			return null;

		Direction targetDirection = inboundTrack.getFace();
		if (targetDirection.getAxis() == CustomPortalHelper.getAxisFrom(otherPortalState))
			targetDirection = targetDirection.getClockWise();
		BlockPos otherPos = otherPortalPos.relative(targetDirection);
		return Pair.of(otherLevel, new BlockFace(otherPos, targetDirection.getOpposite()));

	}

	private static boolean isPortal(Block block) {
		return block instanceof NetherPortalBlock || block instanceof CustomPortalBlock;
	}

}
