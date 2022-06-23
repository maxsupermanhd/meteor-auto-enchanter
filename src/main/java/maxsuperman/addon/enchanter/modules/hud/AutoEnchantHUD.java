package maxsuperman.addon.enchanter.modules.hud;

import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.hud.modules.DoubleTextHudElement;

public class AutoEnchantHUD extends DoubleTextHudElement {
    public AutoEnchantHUD() {
        super(HUD.get(), "hud-example", "Description", "Static left text: ", false);
    }

    @Override
    protected String getRight() {
        return "Dynamic right text";
    }
}
