package com.data.service;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import com.data.repository.SlackLogRepository;
import com.data.util.SlackLogUtil;
import com.data.util.SlackServiceUtil;
import com.data.util.SlackUrlUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
public class SlackMessage {

	private static final Logger logger = LoggerFactory.getLogger(SlackMessage.class);
	
    private static final String TOKEN = "xoxb-325897457793-9242370128659-iIMcV3jtuHB7IwRclCC8rDop";

	@Autowired
    private static SlackLogRepository slackLogRepository;
    
    @Autowired
    public SlackMessage(SlackLogRepository slackLogRepository) {
        this.slackLogRepository = slackLogRepository;
    }
    
    @RequestMapping
    public static void fetchMessages() {
    	
    	String url = SlackUrlUtil.nextUrl();
    	if(url == "DONE") {  return; };
    	
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.addHeader("Authorization", "Bearer " + TOKEN);

            HttpResponse response = client.execute(request);
            
            String json = EntityUtils.toString(response.getEntity());
            //logger.info("Slack Response Json : [ {} ] ", json);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray messages = root.getAsJsonArray("messages");
            
            int msgSize = messages.size();
            boolean ok = root.get("ok").getAsBoolean();
            if (ok) {
            	String slackCallUrl = SlackUrlUtil.convertToTimeUrl(url);
            	logger.info("Slack 채널 호출 성공! => " + slackCallUrl);
            	logger.info("Slack message 갯수 : " + msgSize);
            	if(msgSize == 0) { logger.info("Slack 데이터 갯수 : [ " + msgSize + " ] 입니다. return 진행합니다."); return; }
            } else {
            	String errorMsg = root.has("error") ? root.get("error").getAsString() : "Unknown Error";
            	System.out.println("호출 실패! error: " + errorMsg);
            }
            
            for (int i = 0; i < messages.size(); i++) {
                JsonObject msg = messages.get(i).getAsJsonObject();

                String timestamp = msg.has("ts") ? msg.get("ts").getAsString() : "";
                String text = msg.has("text") ? msg.get("text").getAsString() : "";

                Map<String, String> infomation = SlackServiceUtil.getInfo(text);
                
                String name = infomation.get("name");
                String title = infomation.get("title");
                String type = infomation.get("type");
                String platform = infomation.get("platform");
                String detail = infomation.get("detail");
                String link = infomation.get("link");
                
                String formattedTimestamp = SlackServiceUtil.getTimestamp(timestamp);
                String realName = SlackServiceUtil.getRealName(name, TOKEN);
                try {
					slackLogRepository.insertIfNotExists(
					    title, detail, type, realName, platform, link, formattedTimestamp
					);
				} catch (Exception e) {
					logger.info("title: {}\ndetail: {}\ntype: {}\nrealName: {}\nplatform: {}\nlink: {}\ntimestamp: {}\n", 
						    title, detail, type, realName, platform, link, formattedTimestamp );
					e.printStackTrace();
				}

            } // for end
        } catch (Exception e) {
        	SlackLogUtil.logError(e);
        }
    }
    
}
