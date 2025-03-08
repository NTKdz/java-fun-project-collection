import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class  MyBot extends ListenerAdapter {
    public static void main(String[] arguments) throws Exception
    {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("./application.properties")) {
            properties.load(input);

            String BOT_TOKEN = properties.getProperty("BOT_TOKEN");
            System.out.println("API Key: " + BOT_TOKEN);
            JDA api =  JDABuilder.createDefault(BOT_TOKEN, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new MyBot())
                    .build();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        System.out.println(msg.getContentRaw());
        if (msg.getContentRaw().equalsIgnoreCase("!ping")) {
            event.getChannel().sendMessage("Pong! 🏓").queue();
        }
    }
}
