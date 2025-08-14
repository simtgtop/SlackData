package com.data.resolver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;
import java.util.Map;

public class SlackUserResolver {

    private final String botToken;
    private final Map<String, String> cache = new HashMap<>();

    public SlackUserResolver(String botToken) {
        this.botToken = botToken;
    }

    /**
     * Slack 사용자 ID를 실명(@표시용 이름)으로 변환
     *
     * @param userId Slack 사용자 ID (예: U08MP7VF4JV)
     * @return 한글 이름 또는 실패 시 그대로 ID 반환
     */
    public String getSLackToUserID(String userId) {
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }

        String url = "https://slack.com/api/users.info?user=" + userId;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.addHeader("Authorization", "Bearer " + botToken);

            HttpResponse response = client.execute(request);
            String json = EntityUtils.toString(response.getEntity());

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.get("ok").getAsBoolean()) {
                JsonObject user = root.getAsJsonObject("user");
                String name = user.getAsJsonObject("profile").get("display_name").getAsString();
                cache.put(userId, name);
                return name;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return userId; // 실패 시 ID 반환
    }
}
