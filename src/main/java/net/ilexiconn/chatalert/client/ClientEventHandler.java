package net.ilexiconn.chatalert.client;

import net.ilexiconn.chatalert.ChatAlert;
import net.ilexiconn.chatalert.server.config.ChatAlertConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {
    public Minecraft mc = Minecraft.getMinecraft();

    public Pattern vanillaPattern = Pattern.compile("(<)((?:[a-zA-Z0-9_]+))(>)(.*)");
    public int vanillaUsernameIndex = 2;

    public Pattern currentPattern = vanillaPattern;
    public int currentUsernameIndex = vanillaUsernameIndex;

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        for (String s : ChatAlertConfig.serverRegex) {
            String[] p = s.split(";");
            if (p.length != 3) {
                ChatAlert.logger.error("Found faulty config entry for 'Custom Chat Layouts': " + s);
                continue;
            }
            if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.equals(p[0])) {
                currentPattern = Pattern.compile(p[1]);
                currentUsernameIndex = Integer.parseInt(p[2]);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onServerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        currentPattern = vanillaPattern;
        currentUsernameIndex = vanillaUsernameIndex;
    }

    @SubscribeEvent
    public void onMessageReceived(ClientChatReceivedEvent event) {
        Matcher matcher = currentPattern.matcher(event.message.getUnformattedText());
        if (matcher.find()) {
            String username = matcher.group(currentUsernameIndex);
            for (String s : ChatAlertConfig.ignoredPeople) {
                if (s.equals(username.trim())) {
                    event.setCanceled(true);
                }
            }
            String lastColor = EnumChatFormatting.WHITE.toString();
            int lastIndex = event.message.getFormattedText().substring(0, event.message.getFormattedText().length() - 8).lastIndexOf("§");
            if (lastIndex != -1) {
                lastColor = event.message.getFormattedText().substring(lastIndex, lastIndex + 2);
            }
            boolean flag = false;
            for (String s : ChatAlertConfig.tags) {
                if (event.message.getUnformattedText().contains(s)) {
                    event.message = new ChatComponentText(event.message.getFormattedText().replace(s, ChatAlertConfig.chatFormatting + s + lastColor));
                    flag = true;
                }
            }
            if (flag) {
                mc.thePlayer.playSound(ChatAlertConfig.sound, 1f, 1f);
            }
        }
    }
}
