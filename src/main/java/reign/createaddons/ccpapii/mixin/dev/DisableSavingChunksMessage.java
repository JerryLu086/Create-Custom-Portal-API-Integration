package reign.createaddons.ccpapii.mixin.dev;

import net.minecraft.server.MinecraftServer;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public abstract class DisableSavingChunksMessage {
	// I mean, really, stop messing up my logs plz
	// Only available in: Dev Environment™!
	@Redirect(method = "saveAllChunks", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
	private void shut(Logger logger, String format, Object arg1, Object arg2) {
		return;
	}
}
