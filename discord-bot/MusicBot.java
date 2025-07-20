// Import necessary packages

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class MusicBot extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final String token;

    public MusicBot(String token) {
        this.token = token;
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    // Synchronized to avoid race conditions in multi-thread environment
    private synchronized GuildMusicManager getGuildMusicManager(Guild guild) {
        long guildId = guild.getIdLong();
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
            musicManagers.put(guildId, musicManager);
        }

        return musicManager;
    }

    public void startBot() throws Exception {
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                .enableCache(CacheFlag.VOICE_STATE)
                .setActivity(Activity.listening("music | /play"))
                .addEventListeners(this)
                .build();

        // Wait for bot to start
        jda.awaitReady();

        // Register slash commands
        jda.updateCommands().addCommands(
                Commands.slash("play", "Play a song")
                        .addOptions(new OptionData(OptionType.STRING, "url", "The URL of the song to play").setRequired(true)),
                Commands.slash("skip", "Skip the current song"),
                Commands.slash("stop", "Stop the music and clear the queue"),
                Commands.slash("leave", "Leave the voice channel"),
                Commands.slash("queue", "Show the current queue")
        ).queue();
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("Bot is ready! Logged in as: " + event.getJDA().getSelfUser().getName());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Handle slash commands
        String command = event.getName();

        switch (command) {
            case "play":
                String url = Objects.requireNonNull(event.getOption("url")).getAsString();
                handlePlay(event, url);
                break;
            case "skip":
                handleSkip(event);
                break;
            case "stop":
                handleStop(event);
                break;
            case "leave":
                handleLeave(event);
                break;
            case "queue":
                handleQueue(event);
                break;
            default:
                event.reply("Unknown command").setEphemeral(true).queue();
        }
    }

    private void handlePlay(SlashCommandInteractionEvent event, String url) {
        if (!Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).inAudioChannel()) {
            event.reply("You need to be in a voice channel to use this command!").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) return;
        GuildMusicManager musicManager = getGuildMusicManager(guild);

        // Connect to the voice channel if not already connected
        if (!guild.getAudioManager().isConnected()) {
            guild.getAudioManager().openAudioConnection(event.getMember().getVoiceState().getChannel());
        }

        // Acknowledge the command immediately
        event.deferReply().queue();

        // Load and play the track
        playerManager.loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.scheduler.queue(track);
                event.getHook().sendMessage("Added to queue: " + track.getInfo().title).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                musicManager.scheduler.queue(firstTrack);
                event.getHook().sendMessage("Added to queue: " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();
            }

            @Override
            public void noMatches() {
                event.getHook().sendMessage("Nothing found by " + url).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                event.getHook().sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void handleSkip(SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).inAudioChannel()) {
            event.reply("You need to be in a voice channel to use this command!").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        GuildMusicManager musicManager = getGuildMusicManager(guild);

        musicManager.scheduler.nextTrack();
        event.reply("Skipped the current track").queue();
    }

    private void handleStop(SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(Objects.requireNonNull(event.getMember()).getVoiceState()).inAudioChannel()) {
            event.reply("You need to be in a voice channel to use this command!").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        GuildMusicManager musicManager = getGuildMusicManager(guild);

        musicManager.scheduler.clearQueue();
        musicManager.player.stopTrack();
        event.reply("Playback stopped and queue cleared").queue();
    }

    private void handleLeave(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();

        assert guild != null;
        if (guild.getAudioManager().isConnected()) {
            GuildMusicManager musicManager = getGuildMusicManager(guild);
            musicManager.scheduler.clearQueue();
            musicManager.player.stopTrack();
            guild.getAudioManager().closeAudioConnection();
            event.reply("Disconnected from the voice channel").queue();
        } else {
            event.reply("I'm not in a voice channel").setEphemeral(true).queue();
        }
    }

    private void handleQueue(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        GuildMusicManager musicManager = getGuildMusicManager(guild);

        StringBuilder queueMessage = new StringBuilder("**Current Queue:**\n");

        if (musicManager.player.getPlayingTrack() != null) {
            queueMessage.append("**Now Playing:** ")
                    .append(musicManager.player.getPlayingTrack().getInfo().title)
                    .append("\n\n");
        } else {
            queueMessage.append("**Nothing is currently playing**\n\n");
        }

        if (musicManager.scheduler.getQueue().isEmpty()) {
            queueMessage.append("The queue is empty");
        } else {
            int trackCount = 1;
            for (AudioTrack track : musicManager.scheduler.getQueue()) {
                queueMessage.append(trackCount).append(". ")
                        .append(track.getInfo().title)
                        .append("\n");
                trackCount++;
            }
        }

        event.reply(queueMessage.toString()).queue();
    }

    public static void main(String[] args) {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("./application.properties")) {
            properties.load(input);

            String BOT_TOKEN = properties.getProperty("BOT_TOKEN");
            System.out.println("API Key: " + BOT_TOKEN);
            MusicBot bot = new MusicBot(BOT_TOKEN);
            bot.startBot();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}