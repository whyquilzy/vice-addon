package com.vice.addon.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vice.addon.ViceAddon;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Shows your currently playing Spotify track, with keybinds to skip/previous/pause.
 *
 * This can't just work out of the box - Spotify requires OAuth, and there's no safe
 * way for me to bake shared API credentials into a mod that anyone could pull out
 * of the jar. You need your own (free) Spotify API app:
 *
 *   1. Go to developer.spotify.com/dashboard, log in, "Create app".
 *      Redirect URI can be anything, e.g. http://127.0.0.1:8888/callback - you
 *      won't actually use it for anything except step 2.
 *   2. Copy the Client ID and Client Secret into this module's settings.
 *   3. You still need a "refresh token" once. This is the fiddly part: it means
 *      doing the OAuth "Authorization Code" exchange one time in a browser/terminal
 *      with the scope "user-read-currently-playing user-modify-playback-state",
 *      then pasting the resulting refresh token into the refresh-token setting.
 *      Spotify's own guide walks through this: developer.spotify.com/documentation/web-api/tutorials/code-flow
 *      Ask me and I can walk you through those steps if you get stuck.
 *
 * Once refresh-token is filled in, this refreshes its access token automatically
 * and polls the "currently playing" endpoint every few seconds.
 */
public class SpotifyHud extends HudElement {
    public static final HudElementInfo<SpotifyHud> INFO = new HudElementInfo<>(ViceAddon.HUD_GROUP, "spotify", "Shows your current Spotify track. Requires your own API credentials, see module description.", SpotifyHud::new);

    private final Setting<String> clientId = settings.getDefaultGroup().add(new StringSetting.Builder()
        .name("client-id")
        .description("Your Spotify API app's Client ID.")
        .build()
    );

    private final Setting<String> clientSecret = settings.getDefaultGroup().add(new StringSetting.Builder()
        .name("client-secret")
        .description("Your Spotify API app's Client Secret.")
        .build()
    );

    private final Setting<String> refreshToken = settings.getDefaultGroup().add(new StringSetting.Builder()
        .name("refresh-token")
        .description("One-time OAuth refresh token. See this module's class description for how to get one.")
        .build()
    );

    private final Setting<Integer> pollSeconds = settings.getDefaultGroup().add(new IntSetting.Builder()
        .name("poll-interval")
        .description("How often to check Spotify, in seconds.")
        .defaultValue(5)
        .range(2, 60)
        .sliderRange(2, 30)
        .build()
    );

    private final Setting<SettingColor> textColor = settings.getDefaultGroup().add(new ColorSetting.Builder()
        .name("text-color")
        .defaultValue(new SettingColor(30, 215, 96))
        .build()
    );

    private final Setting<Keybind> skipKey = settings.getDefaultGroup().add(new KeybindSetting.Builder()
        .name("skip-keybind")
        .description("Skips to the next track.")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Keybind> previousKey = settings.getDefaultGroup().add(new KeybindSetting.Builder()
        .name("previous-keybind")
        .description("Goes back to the previous track.")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Keybind> playPauseKey = settings.getDefaultGroup().add(new KeybindSetting.Builder()
        .name("play-pause-keybind")
        .description("Toggles play/pause.")
        .defaultValue(Keybind.none())
        .build()
    );

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;
    private Instant lastPoll = Instant.EPOCH;

    private String track = "";
    private String artist = "";
    private boolean playing;
    private String error;

    public SpotifyHud() {
        super(INFO);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;

        if (skipKey.get().matches(event.input)) call("me/player/next", "POST");
        else if (previousKey.get().matches(event.input)) call("me/player/previous", "POST");
        else if (playPauseKey.get().matches(event.input)) call(playing ? "me/player/pause" : "me/player/play", "PUT");
    }

    @Override
    public void render(HudRenderer renderer) {
        String line;

        if (clientId.get().isEmpty() || clientSecret.get().isEmpty() || refreshToken.get().isEmpty()) {
            line = "Spotify: needs setup (see module description)";
        } else {
            maybePoll();
            line = error != null ? "Spotify: " + error
                : track.isEmpty() ? "Spotify: nothing playing"
                : (playing ? "\u25B6 " : "\u23F8 ") + artist + " - " + track;
        }

        setSize(renderer.textWidth(line, true), renderer.textHeight(true));
        renderer.text(line, x, y, textColor.get(), true);
    }

    private void maybePoll() {
        if (Instant.now().isBefore(lastPoll.plusSeconds(pollSeconds.get()))) return;
        lastPoll = Instant.now();

        if (!ensureAccessToken()) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204 || response.body() == null || response.body().isBlank()) {
                track = "";
                artist = "";
                error = null;
                return;
            }

            if (response.statusCode() != 200) {
                error = "HTTP " + response.statusCode();
                return;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            playing = json.has("is_playing") && json.get("is_playing").getAsBoolean();

            JsonObject item = json.getAsJsonObject("item");
            if (item != null) {
                track = item.get("name").getAsString();
                artist = item.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                error = null;
            }
        } catch (Exception e) {
            error = "request failed";
        }
    }

    private boolean ensureAccessToken() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiry)) return true;

        try {
            String creds = Base64.getEncoder().encodeToString((clientId.get() + ":" + clientSecret.get()).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .header("Authorization", "Basic " + creds)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=refresh_token&refresh_token=" + refreshToken.get()))
                .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                error = "auth failed (" + response.statusCode() + ")";
                return false;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            accessToken = json.get("access_token").getAsString();
            int expiresIn = json.has("expires_in") ? json.get("expires_in").getAsInt() : 3600;
            accessTokenExpiry = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));

            return true;
        } catch (Exception e) {
            error = "auth request failed";
            return false;
        }
    }

    private void call(String endpoint, String method) {
        if (!ensureAccessToken()) return;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.spotify.com/v1/" + endpoint))
                .header("Authorization", "Bearer " + accessToken)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(5))
                .build();

            HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }
}
