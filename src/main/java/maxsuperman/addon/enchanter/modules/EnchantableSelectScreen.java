package maxsuperman.addon.enchanter.modules;

import maxsuperman.addon.enchanter.GenericSelectionCallback;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;

public class EnchantableSelectScreen extends WindowScreen {
    private final GuiTheme theme;
    private final GenericSelectionCallback<Item> callback;

    public EnchantableSelectScreen(GuiTheme theme1, GenericSelectionCallback<Item> callback1) {
        super(theme1, "Select enchantable item");
        this.theme = theme1;
        this.callback = callback1;
    }

    @Override
    public void initWidgets() {
        WTable table = add(theme.table()).widget();
        for (Item e : Registry.ITEM) {
            if (e.getEnchantability() > 0) {
                table.add(theme.item(e.getDefaultStack()));
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
}
