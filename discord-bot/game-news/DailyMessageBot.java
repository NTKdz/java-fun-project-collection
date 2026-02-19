import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalTime;

public class DailyMessageBot {
    public static void main(String[] args) {
        String token = "";

        DiscordClient.create(token)
                .gateway()
                .setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT))
                .login()
                .flatMap(gateway -> {

                    gateway.on(ReadyEvent.class, e -> {
                        System.out.println("Logged in as " + e.getSelf().getUsername());

                        // Print all connected guilds
                        gateway.getGuilds()
                                .doOnNext(g -> System.out.println("Connected guild: " + g.getName() + " (" + g.getId().asLong() + ")"))
                                .subscribe();

                        // Schedule a daily message
                        scheduleDailyMessage(gateway);

                        return Mono.empty();
                    }).subscribe();

                    return Mono.never();
                })
                .block();
    }

    private static void scheduleDailyMessage(GatewayDiscordClient gateway) {
        // Compute seconds until the next target time (e.g., 12:00 PM)
        LocalTime targetTime = LocalTime.of(12, 0);
        long initialDelay = Duration.between(LocalTime.now(), targetTime).toSeconds();
        if (initialDelay < 0) initialDelay += 24 * 3600; // next day if time already passed

        Flux.interval(Duration.ofSeconds(initialDelay), Duration.ofSeconds(1))
                .flatMap(tick ->
                                gateway.getGuilds()
                                        .flatMap(guild ->   guild.getName().equals("testing")
                                                ? guild.getSystemChannel()
                                                : Mono.<MessageChannel>empty())
                                .flatMap(ch -> ch.createMessage("Daily hello!"))
                )
                .onErrorContinue((err, obj) -> err.printStackTrace())
                .subscribe();
    }
}
