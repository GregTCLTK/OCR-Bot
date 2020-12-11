package com.bbn.ocr;

import com.bbn.ocr.listener.MessageReceiveListener;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot {

    public static void main(String[] args) {
        Config config = new Config("./config.json");
        config.load();
        JDABuilder builder = JDABuilder.create(config.getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS));
        builder.setAutoReconnect(true)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners(new MessageReceiveListener(config));

        try {
            builder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
