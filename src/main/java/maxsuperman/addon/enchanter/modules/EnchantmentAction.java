package maxsuperman.addon.enchanter.modules;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentAction implements ISerializable<EnchantmentAction> {
    public Item A = Items.BOOK;
    public List<EnchantmentWithLevel> Ae = new ArrayList<>(2);
    public Item B = Items.BOOK;
    public List<EnchantmentWithLevel> Be = new ArrayList<>(2);

    public EnchantmentAction() {
        A = Items.BOOK;
        Ae = new ArrayList<>();
        B = Items.DIAMOND_SWORD;
        Be = new ArrayList<>();
    }
    public EnchantmentAction(Item a, List<EnchantmentWithLevel> ae, Item b, List<EnchantmentWithLevel> be) {
        A = a;
        Ae = ae;
        B = b;
        Be = be;
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("A", Registry.ITEM.getId(A).toString());
        NbtCompound ae = new NbtCompound();
        for (EnchantmentWithLevel ae1 : Ae) {
            ae.putInt(EnchantmentHelper.getEnchantmentId(ae1.enchantment).toString(), ae1.level);
        }
        tag.put("Ae", ae);

        tag.putString("B", Registry.ITEM.getId(B).toString());
        NbtCompound be = new NbtCompound();
        for (EnchantmentWithLevel be1 : Be) {
            be.putInt(EnchantmentHelper.getEnchantmentId(be1.enchantment).toString(), be1.level);
        }
        tag.put("Be", be);

        return tag;
    }

    @Override
    public EnchantmentAction fromTag(NbtCompound tag) {
        var r = new EnchantmentAction();

        r.A = Registry.ITEM.get(new Identifier(tag.getString("A")));
        for (String ae : tag.getCompound("Ae").getKeys()) {
            r.Ae.add(new EnchantmentWithLevel(Registry.ENCHANTMENT.get(new Identifier(ae)), tag.getCompound("Ae").getInt(ae)));
        }

        r.B = Registry.ITEM.get(new Identifier(tag.getString("B")));
        for (String be : tag.getCompound("Be").getKeys()) {
            r.Be.add(new EnchantmentWithLevel(Registry.ENCHANTMENT.get(new Identifier(be)), tag.getCompound("Be").getInt(be)));
        }

        return r;
    }
}
