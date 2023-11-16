package reign.createaddons.ccpapii.mixin;

import com.simibubi.create.content.contraptions.components.structureMovement.glue.SuperGlueEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import reign.createaddons.ccpapii.SuperGlueEntityAccessor;

@Mixin(SuperGlueEntity.class)
public abstract class SuperGlueEntityMixin extends Entity implements SuperGlueEntityAccessor {

	@Override
	public void setPortalEntrancePos() {
		this.portalEntrancePos = blockPosition();
	}

	public SuperGlueEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Overwrite
	public PortalInfo findDimensionEntryPoint(ServerLevel target) {
		return super.findDimensionEntryPoint(target);
	}

}
