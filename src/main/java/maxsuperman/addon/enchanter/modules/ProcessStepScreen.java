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
        var labelA = table.add(theme.horizontalList()).widget();
        labelA.add(theme.label("Item A:"));
        labelA.add(theme.item(action.A.getDefaultStack()));
        var acb = labelA.add(theme.button("Change")).widget();
        acb.action = () -> {
            MeteorClient.mc.setScreen(new EnchantableSelectScreen(theme, (Item sel) -> {
                action.A = sel;
                table.clear();
                fillTable(table);
            }));
        };
        table.row();

        var atable = table.add(theme.table()).widget();
        for (EnchantmentWithLevel e : action.Ae) {
            var d = atable.add(theme.button("D")).widget();
            d.action = () -> {
                action.Ae.remove(e);
                table.clear();
                fillTable(table);
            };
            var c = atable.add(theme.button("C")).widget();
            c.action = () -> {
                MeteorClient.mc.setScreen(new EnchantmentSelectScreen(theme, (Enchantment sel) -> {
                    e.enchantment = sel;
                    table.clear();
                    fillTable(table);
                }));
            };
            atable.add(theme.label(Names.get(e.enchantment)));
            atable.add(theme.intEdit(e.level, 1, e.enchantment.getMaxLevel(), true));
            atable.row();
        }
        table.row();
        var aa = table.add(theme.button("Add")).widget();
        aa.action = () -> {
            action.Ae.add(new EnchantmentWithLevel(Enchantments.UNBREAKING, 3));
            table.clear();
            fillTable(table);
        };
        table.row();


        var labelB = table.add(theme.horizontalList()).widget();
        labelB.add(theme.label("Item B:"));
        labelB.add(theme.item(action.B.getDefaultStack()));
        var bcb = labelB.add(theme.button("Change")).widget();
        bcb.action = () -> {
            MeteorClient.mc.setScreen(new EnchantableSelectScreen(theme, (Item sel) -> {
                action.B = sel;
                table.clear();
                fillTable(table);
            }));
        };
        table.row();

        var btable = table.add(theme.table()).widget();
        for (EnchantmentWithLevel e : action.Be) {
            var d = btable.add(theme.button("D")).widget();
            d.action = () -> {
                action.Be.remove(e);
                table.clear();
                fillTable(table);
            };
            var c = btable.add(theme.button("C")).widget();
            c.action = () -> {
                MeteorClient.mc.setScreen(new EnchantmentSelectScreen(theme, (Enchantment sel) -> {
                    e.enchantment = sel;
                    table.clear();
                    fillTable(table);
                }));
            };
            btable.add(theme.label(Names.get(e.enchantment)));
            btable.add(theme.intEdit(e.level, 1, e.enchantment.getMaxLevel(), true));
            btable.row();
        }
        table.row();

        var ba = table.add(theme.button("Add")).widget();
        ba.action = () -> {
            action.Be.add(new EnchantmentWithLevel(Enchantments.UNBREAKING, 3));
            table.clear();
            fillTable(table);
        };
        table.row();

        var done = table.add(theme.button("Done")).widget();
        done.action = this::close;
    }
}
