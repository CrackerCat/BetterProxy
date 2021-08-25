package me.ANONIMUS.proxy.commands.normal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import me.ANONIMUS.proxy.enums.ConnectedType;
import me.ANONIMUS.proxy.objects.Command;
import me.ANONIMUS.proxy.protocol.data.ConnectionState;
import me.ANONIMUS.proxy.protocol.objects.Bot;
import me.ANONIMUS.proxy.protocol.objects.Player;
import me.ANONIMUS.proxy.protocol.packet.Packet;
import me.ANONIMUS.proxy.protocol.packet.impl.client.play.ClientCustomPayloadPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.client.play.ClientKeepAlivePacket;
import me.ANONIMUS.proxy.protocol.packet.impl.client.play.ClientSettingsPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.login.ServerLoginDisconnectPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.login.ServerLoginSetCompressionPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.login.ServerLoginSuccessPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.play.ServerChatPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.play.ServerDisconnectPacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.play.ServerJoinGamePacket;
import me.ANONIMUS.proxy.protocol.packet.impl.server.play.ServerKeepAlivePacket;
import me.ANONIMUS.proxy.utils.ChatUtil;
import me.ANONIMUS.proxy.utils.WorldUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class CommandStay extends Command {
    public CommandStay() {
        super("stay", "detach", null, null, ConnectedType.CONNECTED);
    }

    private final EventLoopGroup group = new NioEventLoopGroup();

    @Override
    public void onCommand(Player sender, String[] args) {
        final Bot bot = new Bot(sender);

        sender.getRemoteSession().getChannel().pipeline().replace("handler", "handler", new SimpleChannelInboundHandler<Packet>() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) {
                disconnect(ctx.name(), bot, sender);
            }

            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet packet) {
                if (packet instanceof ServerLoginSetCompressionPacket) {
                    bot.getSession().setCompressionThreshold(((ServerLoginSetCompressionPacket) packet).getThreshold());
                } else if (packet instanceof ServerLoginSuccessPacket) {
                    bot.getSession().setConnectionState(ConnectionState.PLAY);
                } else if (packet instanceof ServerKeepAlivePacket) {
                    bot.getSession().sendPacket(new ClientKeepAlivePacket(((ServerKeepAlivePacket) packet).getKeepaliveId()));
                } else if (packet instanceof ServerJoinGamePacket) {
                    ChatUtil.sendChatMessage(sender.getThemeType().getColor(1) + ">> &8Bot &a" + bot.getUsername() + " &8connected to the server", bot.getOwner(), true);
                    bot.getOwner().getBots().add(bot);
                    bot.getSession().sendPacket(new ClientCustomPayloadPacket("MC|Brand", "vanilla".getBytes()));
                    bot.getSession().sendPacket(new ClientSettingsPacket("pl_PL", (byte) 32, (byte) 0, false, (byte) 1));
                } else if (packet instanceof ServerDisconnectPacket) {
                    disconnect(ChatColor.stripColor(BaseComponent.toLegacyText(((ServerDisconnectPacket) packet).getReason())), bot, sender);
                } else if (packet instanceof ServerLoginDisconnectPacket) {
                    disconnect(ChatColor.stripColor(BaseComponent.toLegacyText(((ServerLoginDisconnectPacket) packet).getReason())), bot, sender);
                }
            }
        });

        bot.setSession(sender.getRemoteSession());
        bot.getSession().setUsername(sender.getRemoteSession().getUsername());

        sender.getBots().add(bot);

        sender.getRemoteSession().getChannel().close();
        sender.setConnectedType(ConnectedType.DISCONNECTED);
        WorldUtil.lobby(sender, true);
    }

    private void disconnect(String cause, Bot bot, Player owner) {
        owner.getSession().sendPacket(new ServerChatPacket(new TextComponent(ChatUtil.fixColor(bot.getOwner().getThemeType().getColor(1) + ">> &8Bot &c" + bot.getUsername() + " &8disconnected from the server"))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(ChatUtil.fixColor(" &fcause: &c" + cause))))));


        bot.getSession().getChannel().close();
        bot.setSession(null);
        owner.getBots().remove(bot);
        group.shutdownGracefully();
    }
}