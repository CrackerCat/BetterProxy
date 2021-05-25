package me.ANONIMUS.proxy.utils;

import me.ANONIMUS.proxy.objects.Option;
import me.ANONIMUS.proxy.objects.Skin;
import me.ANONIMUS.proxy.protocol.data.ItemStack;
import me.ANONIMUS.proxy.protocol.objects.Player;
import me.ANONIMUS.proxy.protocol.packet.impl.server.play.ServerSetSlotPacket;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemUtil {
    public static ItemStack option(Option option) {
        final List<String> lores = new ArrayList<>();
        lores.add("");
        if (option.hasDescription()) {
            lores.add(ChatUtil.fixColor("&fDescription: "));
            Arrays.stream(option.getDescription()).forEach(s -> lores.add(ChatUtil.fixColor("&7" + s)));
            lores.add("");
        }
        lores.add(ChatUtil.fixColor("&fClick to " + (option.isEnabled() ? "&cdisable" : "&aenable")));
        return new ItemStack(160, 1, option.isEnabled() ? 5 : 14).setStackDisplayName(ChatUtil.fixColor("&l" + option.getName().toUpperCase())).setLoreName(lores);
    }

    public static ItemStack skull(Skin skin) {
        if(skin == null) {
            return new ItemStack(397, 1, 3);
        }

        NBTTagCompound nbt = new NBTTagCompound();

        NBTTagCompound property = new NBTTagCompound();
        property.setString("Signature", skin.getSignature());
        property.setString("Value", skin.getValue());

        NBTTagList textures = new NBTTagList();
        textures.appendTag(property);

        NBTTagCompound properties = new NBTTagCompound();
        properties.setTag("textures", textures);

        NBTTagCompound skullOwner = new NBTTagCompound();
        skullOwner.setString("Id", skin.getGameProfile().getIdAsString());
        skullOwner.setString("Name", skin.getGameProfile().getName());
        skullOwner.setTag("Properties", properties);

        nbt.setTag("SkullOwner", skullOwner);

        return new ItemStack(397, 1, 3, nbt);
    }

    public static ItemStack optionsMenu() {
        return new ItemStack(404, ChatUtil.fixColor("&8Options"));
    }

    public static ItemStack serverMenu() {
        return new ItemStack(2, ChatUtil.fixColor("&aServers"));
    }

    public static ItemStack changeSkinMenu(Player player) {
        return skull(player.getSkin()).setStackDisplayName(ChatUtil.fixColor("&6Skins"));
    }

    public static void loadStartItems(Player player) {
        player.getSession().sendPacket(new ServerSetSlotPacket(0, 36, serverMenu()));
        player.getSession().sendPacket(new ServerSetSlotPacket(0, 40, optionsMenu()));
        player.getSession().sendPacket(new ServerSetSlotPacket(0, 44, changeSkinMenu(player)));
    }
}