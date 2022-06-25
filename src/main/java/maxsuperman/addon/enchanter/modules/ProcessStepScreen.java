package maxsuperman.addon.enchanter.modules;


import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;

public class ProcessStepScreen extends WindowScreen {
    private final GuiTheme theme;
    private EnchantmentAction action;

    public ProcessStepScreen(GuiTheme theme1, EnchantmentAction action1) {
        super(theme1, "Edit");
        this.theme = theme1;
        this.action = action1;
    }

    @Override
    public void initWidgets() {
        clearChildren();
        fillTable(add(theme.table()).widget());
    }

    public void fillTable(WTable table) {
        table.add(theme.label("Item A:"));
        table.add(theme.item(action.A.getDefaultStack()));
        var acb = table.add(theme.button("Change")).widget();
        acb.action = () -> {
            MeteorClient.mc.setScreen(new EnchantableSelectScreen(theme, (Item sel) -> {
                action.A = sel;
                table.clear();
                fillTable(table);
            }));
        };
        table.row();

        for (EnchantmentWithLevel e : action.Ae) {
            var d = table.add(theme.button("D")).widget();
            d.action = () -> {
                action.Ae.remove(e);
                table.clear();
                fillTable(table);
            };
            var c = table.add(theme.button("C")).widget();
            c.action = () -> {
                MeteorClient.mc.setScreen(new EnchantmentSelectScreen(theme, (Enchantment sel) -> {
                    e.enchantment = sel;
                    table.clear();
                    fillTable(table);
                }));
            };
            table.add(theme.label(e.enchantment.toString()));
            table.add(theme.intEdit(e.level, 1, e.enchantment.getMaxLevel(), true));
            table.row();
        }
        var aa = table.add(theme.button("Add")).widget();
        aa.action = () -> {
            action.Ae.add(new EnchantmentWithLevel(Enchantments.UNBREAKING, 3));
            table.clear();
            fillTable(table);
        };
        table.row();


        table.add(theme.label("Item B:"));
        table.add(theme.item(action.B.getDefaultStack()));
        var bcb = table.add(theme.button("Change")).widget();
        bcb.action = () -> {
            MeteorClient.mc.setScreen(new EnchantableSelectScreen(theme, (Item sel) -> {
                action.B = sel;
                table.clear();
                fillTable(table);
            }));
        };
        table.row();

        for (EnchantmentWithLevel e : action.Be) {
            var d = table.add(theme.button("D")).widget();
            d.action = () -> {
                action.Be.remove(e);
                table.clear();
                fillTable(table);
            };
            var c = table.add(theme.button("C")).widget();
            c.action = () -> {
                MeteorClient.mc.setScreen(new EnchantmentSelectScreen(theme, (Enchantment sel) -> {
                    e.enchantment = sel;
                    table.clear();
                    fillTable(table);
                }));
            };
            table.add(theme.label(Names.get(e.enchantment)));
            table.add(theme.intEdit(e.level, 1, e.enchantment.getMaxLevel(), true));
            table.row();
        }
        var ba = table.add(theme.button("Add")).widget();
        ba.action = () -> {
            action.Ae.add(new EnchantmentWithLevel(Enchantments.UNBREAKING, 3));
            table.clear();
            fillTable(table);
        };
        table.row();

        var done = table.add(theme.button("Done")).widget();
        done.action = this::close;
    }
}
