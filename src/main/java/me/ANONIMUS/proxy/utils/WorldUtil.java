package me.ANONIMUS.proxy.utils;

import me.ANONIMUS.proxy.BetterProxy;
import me.ANONIMUS.proxy.objects.Skin;
import me.ANONIMUS.proxy.protocol.ProtocolType;
import me.ANONIMUS.proxy.protocol.data.*;
import me.ANONIMUS.proxy.protocol.objects.GameProfile;
import me.ANONIMUS.proxy.protocol.objects.Player;
import me.ANONIMUS.proxy.protocol.packet.impl.CustomPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.play.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class WorldUtil {
    public static void dimSwitch(Player player, ServerJoinGamePacket packet) {
        if(packet == null) {
            packet = new ServerJoinGamePacket(0, Gamemode.SURVIVAL, Dimension.OVERWORLD, Difficulty.PEACEFULL, 1, "default_1_1", false);
        }

        player.getSession().sendPacket(new ServerRespawnPacket(Dimension.END, Difficulty.PEACEFULL, Gamemode.SURVIVAL, "default_1_1"));
        player.getSession().sendPacket(packet);
        player.getSession().sendPacket(new ServerRespawnPacket(packet.getDimension(), packet.getDifficulty(), packet.getGamemode(), packet.getLevelType()));
    }

    public static void emptyWorld(Player player) {
        dimSwitch(player, null);

        player.getSession().sendPacket(new ServerSpawnPositionPacket(new Position(0, 1, 0)));
        player.getSession().sendPacket(new ServerPlayerAbilitiesPacket(false, true, false, false, 0f, 0f));
        player.getSession().sendPacket(new ServerPlayerPosLookPacket(0, 70, 0, 180, 90));
    }

    public static void lobby(Player player, boolean clear) {
        if (clear) {
            dimSwitch(player, null);
        }

        if (player.getSession().getProtocolID() != ProtocolType.PROTOCOL_1_9_2.getProtocol() &&
                player.getSession().getProtocolID() != ProtocolType.PROTOCOL_1_9.getProtocol() &&
                player.getSession().getProtocolID() != ProtocolType.PROTOCOL_1_9_1.getProtocol() &&
                player.getSession().getProtocolID() != ProtocolType.PROTOCOL_1_11.getProtocol()) {
            try {
                int i = 0;
                File file;
                while ((file = new File(BetterProxy.getInstance().getDirFolder() + "/world/" + (player.getSession().getProtocolID() != 47 ? "other" : "47") + "/world_" + i)).exists()) {
                    byte[] data = Files.readAllBytes(file.toPath());
                    player.getSession().sendPacket(new CustomPacket(player.getSession().getProtocolID() == 47 ? 0x26 : 0x20, data));
                    i++;
                }
            } catch (IOException ignored) { }
        }

        player.getSession().sendPacket(new ServerPlayerAbilitiesPacket(false, false, false, false, 0f, 0f));
        PacketUtil.lobbyPosTeleport(player);
        PacketUtil.clearTabList(player);

        if (player.getSession().getProtocolID() == ProtocolType.PROTOCOL_1_8_X.getProtocol()) {
            spawnPlayers(player);
            loadTextsOnSign(player);
        }

        PacketUtil.clearInventory(player);
        ItemUtil.loadStartItems(player);
    }

    private static void spawnPlayers(Player p) {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(BetterProxy.getInstance().getDirFolder() + "/world/players.json"));
            JSONArray jsonObj = (JSONArray) obj;

            int i = 1;
            for (Object players : jsonObj.toArray()) {
                JSONObject player = (JSONObject) ((JSONObject) players).get("player");
                String name = (String) player.get("name");

                JSONObject position = (JSONObject) player.get("position");
                double x = ((Number) position.get("x")).doubleValue();
                double y = ((Number) position.get("y")).doubleValue();
                double z = ((Number) position.get("z")).doubleValue();

                JSONObject textures = (JSONObject) player.get("textures");
                String value = (String) textures.get("value");
                String signature = (String) textures.get("signature");

                GameProfile profile = new GameProfile(UUID.randomUUID(), name);
                profile.getProperties().add(new GameProfile.Property("textures", value, signature));

                p.getTabList().add(SkinUtil.showSkin(p.getSession(), null, new Skin(profile)));
                p.getSession().sendPacket(new ServerSpawnPlayerPacket(i, profile.getId(), x, y, z, 0, 0, 0, new EntityMetadata(10, MetadataType.BYTE, Byte.MAX_VALUE)));
                i++;
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void loadTextsOnSign(Player p) {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(BetterProxy.getInstance().getDirFolder() + "/world/signs.json"));
            JSONArray jsonObj = (JSONArray) obj;

            jsonObj.forEach(signs -> {
                JSONObject sign = (JSONObject) ((JSONObject) signs).get("sign");
                String text_1 = (String) sign.get("text_1");
                String text_2 = (String) sign.get("text_2");
                String text_3 = (String) sign.get("text_3");
                String text_4 = (String) sign.get("text_4");

                JSONObject position = (JSONObject) sign.get("position");
                int x = ((Number) position.get("x")).intValue();
                int y = ((Number) position.get("y")).intValue();
                int z = ((Number) position.get("z")).intValue();

                p.getSession().sendPacket(new ServerUpdateSignPacket(new Position(x, y, z), ChatUtil.fixColor(text_1), ChatUtil.fixColor(text_2), ChatUtil.fixColor(text_3), ChatUtil.fixColor(text_4)));
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}