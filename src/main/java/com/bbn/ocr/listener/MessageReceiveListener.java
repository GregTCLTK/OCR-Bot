package com.bbn.ocr.listener;

import com.bbn.ocr.Config;
import com.bbn.ocr.OCR;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;

public class MessageReceiveListener extends ListenerAdapter {

    Config config;

    public MessageReceiveListener(Config config) {
        this.config = config;
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().startsWith("!bot ocr ")) {
            String url = event.getMessage().getContentRaw().replace("!bot ocr ", "");
            try {
                new URL(url).toURI();
                String output = OCR.handle(url, event);
                String[] boxes = output.split("21日前");
                EmbedBuilder embedBuilder = new EmbedBuilder();
                List<MessageEmbed.Field> fields = new ArrayList<>();
                for (int i = 0; boxes.length > i; i++) {
                    String box = boxes[i].replaceAll("\n", "");
                    if (i < 4) {
                        if (!box.isEmpty()) {
                            String name = box.split("が")[0];
                            String boss_name = box.split("に")[0].split("が")[1];
                            String damage = box.split("ダメージ")[0].split("に")[1];
                            String[] temp = box.split("ダメージ");
                            boolean kill;
                            if (temp.length > 1) {
                                kill = temp[1].contains("撃破");
                            } else kill = false;
                            fields.add(new MessageEmbed.Field("Report " + (boxes.length - i - 1), name + "\n" + boss_name + "\n" + damage + ((kill) ? "(撃破)" : ""), false));
                        }
                    }
                }
                for (int i = fields.size() - 1; i >= 0; i--) {
                    embedBuilder.addField(fields.get(i));
                }
                event.getTextChannel().sendMessage(embedBuilder
                        .setTitle("OCR Result")
                        .setColor(Color.YELLOW)
                        .setImage(url)
                        .setFooter("If you believe this message contains a error please contact the developer or sth. like this")
                        .build()).queue(
                        msg -> {
                            for (int i = 1; i < 5; i++) {
                                msg.addReaction(getNumberEmoji(i)).queue();
                            }
                            msg.addReaction("⭕").queue();
                            msg.addReaction("❌").queue();
                            Object listener = new ListenerAdapter() {
                                @Override
                                public void onGuildMessageReactionAdd(@Nonnull GuildMessageReactionAddEvent event2) {
                                    if (event.getAuthor().getId().equals(event2.getUser().getId()) && msg.getId().equals(event2.getMessageId())) {


                                        switch (event2.getReactionEmote().getName()) {
                                            case "❌":
                                                event2.getJDA().removeEventListener(this);
                                                event2.getChannel().sendMessage(new EmbedBuilder().setTitle("Marked as invalid").setDescription("I marked the results as invalid :(").build()).queue();
                                                break;

                                            case "⭕":
                                                event2.getJDA().removeEventListener(this);

                                                StringBuilder sb = new StringBuilder();
                                                boolean killed = false;

                                                for (MessageEmbed.Field field : msg.getEmbeds().get(0).getFields()) {
                                                    if (field.getValue().contains("撃破")) {
                                                        killed = true;
                                                        sb.append(field.getName()).append(" killed an enemy.\n");
                                                    }
                                                }

                                                if (killed) {
                                                    event2.getChannel().sendMessage(new EmbedBuilder()
                                                            .setDescription(sb.toString() + "\n Please indicate the rewarded overkill time in the following format:\n" +
                                                                    "[Report #] [Time] [Report #] [Time]\ni.e. 3 1:30 4 0:45").build()).queue(
                                                            msg2 -> {
                                                                Object listener2 = new ListenerAdapter() {
                                                                    @Override
                                                                    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
                                                                        if (event.getAuthor().getId().equals(event2.getUser().getId())) {
                                                                            event.getJDA().removeEventListener(this);
                                                                            try {
                                                                                doSpreadsheetMagic(msg, msg2, url);
                                                                            } catch (GeneralSecurityException | IOException e) {
                                                                                e.printStackTrace();
                                                                            }
                                                                        }
                                                                    }
                                                                };
                                                                msg2.getJDA().addEventListener(listener2);
                                                            }
                                                    );
                                                } else {
                                                    try {
                                                        doSpreadsheetMagic(msg, null, url);
                                                    } catch (GeneralSecurityException | IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                break;
                                        }
                                    }
                                }
                            };
                            event.getJDA().addEventListener(listener);
                        }
                );


            } catch (Exception e) {
                event.getTextChannel().sendMessage(new EmbedBuilder()
                        .setTitle("No URL")
                        .setDescription("No URL found in the message.")
                        .setColor(Color.RED)
                        .setFooter("If you believe that this was a error please contact the developer or sth. like this")
                        .build()).queue();
                e.printStackTrace();
            }
        }
        super.onMessageReceived(event);
    }

    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final Set<String> SCOPES = SheetsScopes.all();
    private static final String CREDENTIALS_FILE_PATH = "./credentials.json";

    public void doSpreadsheetMagic(Message message, Message message2, String url) throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = "1c9Z3KbgrluEoDAoDoTi0sxow6ItBa9Rtuv42JaNtvV8";
        final String range = "B1:B500";
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange result = service.spreadsheets().values().get(spreadsheetId, range).execute();
        int numRows = result.getValues() != null ? result.getValues().size() : 0;
        List<MessageEmbed.Field> fields = message.getEmbeds().get(0).getFields();
        for (int i = 0; fields.size() > i; i++) {
            MessageEmbed.Field field = fields.get(i);
            String[] array = field.getValue().split("\n");
            List<MessageReaction> reactions = message.getReactions();
            int reactioncount = 0;
            for (MessageReaction reaction : reactions) {
                if (reaction.getReactionEmote().getEmoji().equals(getNumberEmoji(i))) {
                    reactioncount = reaction.getCount();
                    break;
                }
            }
            String[] overkilltimes = message2.getContentRaw().split(" ");
            String overkilltime = "";
            if (overkilltimes.length > 2) {
                if ((overkilltimes.length % 2) == 0) {
                    for (int k = 0; overkilltimes.length > k; k += 2) {
                        if (overkilltimes[k].equals(String.valueOf(i))) {
                            overkilltime = overkilltimes[k + 1];
                        }
                    }
                }
            }
            List<List<Object>> values = Collections.singletonList(
                    Arrays.asList(
                            "", // day
                            array[0], // name
                            array[1], // boss name
                            array[2].replace("(撃破)", ""), // damage
                            array[2].contains("撃破"), // kill
                            reactioncount == 2, // magic
                            overkilltime, // overkilltime
                            "", // TODO: Killlabel
                            new File("./lastpicture.png"), // cropped Image
                            url, // Original image
                            message.getJumpUrl() // Message Link
                    )
            );
            ValueRange body = new ValueRange()
                    .setValues(values);
            service.spreadsheets().values().update(spreadsheetId, "B" + (numRows + 1 + i), body)
                    .setValueInputOption("RAW")
                    .execute();
        }
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(new File(CREDENTIALS_FILE_PATH));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static String getNumberEmoji(int i) {
        String ret = " ";
        if (i == 10) {
            ret = "\uD83D\uDD1F";
        } else if (i < 10) {
            ret = i + "\u20E3";
        }
        return ret;
    }
}
