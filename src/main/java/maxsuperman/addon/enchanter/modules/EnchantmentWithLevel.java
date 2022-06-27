package maxsuperman.addon.enchanter.modules;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EnchantmentWithLevel implements Comparable<EnchantmentWithLevel> {
    public Enchantment enchantment;
    public int level;
    public EnchantmentWithLevel() {
        enchantment = Enchantments.UNBREAKING;
        level = Enchantments.UNBREAKING.getMaxLevel();
    }
    public EnchantmentWithLevel(Enchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
    }

    @Override
    public int compareTo(@NotNull EnchantmentWithLevel o) {
        var c1 = EnchantmentHelper.getEnchantmentId(enchantment).compareTo(EnchantmentHelper.getEnchantmentId(o.enchantment));
        if (c1 != 0) {
            return c1;
        }
        return level - o.level;
    }
}
