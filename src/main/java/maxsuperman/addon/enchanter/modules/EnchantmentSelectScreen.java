package maxsuperman.addon.enchanter.modules;

import maxsuperman.addon.enchanter.GenericSelectionCallback;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.registry.Registry;

public class EnchantmentSelectScreen extends WindowScreen {
    private final GuiTheme theme;
    private final GenericSelectionCallback<Enchantment> callback;

    public EnchantmentSelectScreen(GuiTheme theme1, GenericSelectionCallback<Enchantment> callback1) {
        super(theme1, "Select enchantment");
        this.theme = theme1;
        this.callback = callback1;
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).widget();
        for (Enchantment e : Registry.ENCHANTMENT) {
            table.add(theme.label(Names.get(e))).expandCellX();
            WButton a = table.add(theme.button("Select")).widget();
            a.action = () -> {
                callback.Selection(e);
                close();
            };
            table.row();
        }
    }
}
