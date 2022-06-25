package maxsuperman.addon.enchanter.modules;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;

public class EnchantmentWithLevel {
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
}
