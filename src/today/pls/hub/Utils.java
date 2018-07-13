package today.pls.hub;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class Utils {

    public static ItemStack itemStackWithName(Material m, String n){
        return itemStackWithNameAndLore(m,n,null);
    }

    public static ItemStack itemStackWithNameAndLore(Material m, String n, String l){
        return setItemNameAndLore(new ItemStack(m),n,l);
    }

    public static ItemStack setItemNameAndLore(ItemStack i, String n, String l){
        ItemMeta m = i.getItemMeta();
        if(l != null){
            m.setLore(Arrays.asList(l.split("\n")));
        }
        m.setDisplayName(n);
        i.setItemMeta(m);
        return i;
    }
}
