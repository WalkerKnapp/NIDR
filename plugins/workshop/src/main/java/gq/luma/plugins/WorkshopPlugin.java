package gq.luma.plugins;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import gq.luma.render.plugins.Plugin;
import gq.luma.render.renderer.configuration.SrcGameConfiguration;
import io.wkna.sdp.SourceDemo;
import io.wkna.sdp.messages.PacketMessage;
import io.wkna.sdp.messages.SignOnMessage;
import io.wkna.sdp.structures.NetData;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WorkshopPlugin implements Plugin {
    private static final Path workshopsConfigPath = Paths.get("workshops.json");
    private static String steamKey;
    private static JsonArray workshopsConfig;

    private static final Pattern ID_PATTERN = Pattern.compile("(?<=workshop\\\\|/)(?<id>\\d*)(?=[\\\\/])");
    private static HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
            .scheme("https")
            .host("api.steampowered.com")
            .addPathSegment("ISteamRemoteStorage")
            .addPathSegment("GetUGCFileDetails")
            .addPathSegment("v1");

    static {
        try {
            steamKey = new String(WorkshopPlugin.class.getResourceAsStream("/steam.key").readAllBytes());

            try(BufferedReader reader = Files.newBufferedReader(workshopsConfigPath)) {
                workshopsConfig = Json.parse(reader).asArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    private Optional<JsonObject> getMatchingWorkshopConfig(SrcGameConfiguration game) {
        for(JsonValue value : workshopsConfig) {
            if(value.asObject().get("directoryName").asString().equals(game.getDirectoryName())
                && value.asObject().get("appCode").asInt() == game.getAppCode()) {
                return Optional.ofNullable(value.asObject());
            }
        }

        return Optional.empty();
    }

    private Optional<Path> downloadSteamMap(Path workshopDir, String ugcID, int deployApp, int deployAppAlt){
        try {
            System.out.println("Using key: " + steamKey);

            String result = performRequest(urlBuilder.setQueryParameter("appid", String.valueOf(deployApp))
                    .setQueryParameter("key", steamKey)
                    .setQueryParameter("ugcid", ugcID).build());

            System.out.println("String: " + result);

            JsonObject node = Json.parse(result).asObject();
            if(node.get("data") != null && !node.get("data").asObject().isEmpty()){
                node = node.get("data").asObject();
            } else {
                String secondaryResult =  performRequest(urlBuilder.setQueryParameter("appid", String.valueOf(deployAppAlt))
                        .setQueryParameter("key", steamKey)
                        .setQueryParameter("ugcid", ugcID).build());
                node = Json.parse(secondaryResult).asObject();
                System.out.println("String: " + node.toString());
                if(node.get("data") != null){
                    node = node.get("data").asObject();
                } else {
                    return Optional.empty();
                }
            }

            System.out.println("Got response " + node.toString());

            Path contentDir = workshopDir.resolve(ugcID);
            if(!Files.exists(contentDir)) {
                Files.createDirectory(contentDir);
            }
            Path finalMapFile = contentDir.resolve(lastOf(node.get("filename").asString().split("/")));

            try(InputStream inputStream = new URL(node.get("url").asString()).openStream();
                ReadableByteChannel rbc = Channels.newChannel(inputStream);
                FileChannel channel = FileChannel.open(finalMapFile)){
                channel.transferFrom(rbc, 0, Long.MAX_VALUE);
            }

            System.out.println("Downloaded to " + finalMapFile.toString());

            System.out.println("Downloaded id: " + ugcID);
            return Optional.of(finalMapFile);

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private String performRequest(HttpUrl url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        System.out.println(request.toString());
        return Objects.requireNonNull(okHttpClient.newCall(request)
                .execute()
                .body())
                .string();
    }

    private Optional<String> parseHcontent(String signOnString){
        Matcher m = ID_PATTERN.matcher(signOnString);
        while(m.find()){
            if(m.group("id") != null) return Optional.of(m.group("id"));
        }
        return Optional.empty();
    }

    private static String lastOf(String[] array){
        return array[array.length - 1];
    }

    @Override
    public void onDemoLoad(SrcGameConfiguration gameConfig, SourceDemo demo) {
        getMatchingWorkshopConfig(gameConfig).ifPresentOrElse(workshopConfig -> {
            Path workshopDir = Paths.get(workshopConfig.get("workshopDir").asString());
            int appCode = workshopConfig.get("publishingAppCode").asInt();

            demo.getMessages().stream()
                    .filter(message -> message instanceof SignOnMessage)
                    .findFirst()
                    .map(message -> (SignOnMessage)message)
                    .map(PacketMessage::getNetData)
                    .map(NetData::getData)
                    .map(String::new)
                    .flatMap(this::parseHcontent)
                    .flatMap(hcontentString -> {
                        try {
                            return Files.list(workshopDir).filter(path -> {
                                try {
                                    return path.getFileName().toString().equalsIgnoreCase(hcontentString) &&
                                            Files.list(path)
                                                    .anyMatch(subPath ->
                                                            subPath.getFileName().toString().equalsIgnoreCase(demo.getMapName() + ".bsp"));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            }).findFirst()
                                    .or(() -> downloadSteamMap(workshopDir, hcontentString, appCode, gameConfig.getAppCode()));
                        } catch (IOException e) {
                            e.printStackTrace();
                            return Optional.empty();
                        }
                    })
                    .ifPresentOrElse(path -> System.out.println("Successfully found map: " + path.toString()),
                            () -> System.err.println("Failed to download workshop map specified in demo."));

        }, () -> System.err.println("Not running workshop check, unknown game: " + gameConfig.getDirectoryName()));
    }
}
