package maxsuperman.addon.enchanter;

import maxsuperman.addon.enchanter.modules.AutoEnchant;
import maxsuperman.addon.enchanter.modules.hud.AutoEnchantHUD;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class EnchanterAddon extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger(EnchanterAddon.class);

	@Override
	public void onInitialize() {
		LOG.info("Initializing auto-enchanter");

		// Required when using @EventHandler
		MeteorClient.EVENT_BUS.registerLambdaFactory("maxsuperman.addon.enchanter", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

		// Modules
		Modules.get().add(new AutoEnchant());

		// HUD
        HUD.get().elements.add(new AutoEnchantHUD());
	}
}
