package com.data.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.data.resolver.SlackUserResolver;

public class SlackServiceUtil {

    private static String decodeEntities(String s) {
        if (s == null) return null;
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
    
    public static Map<String, String> getInfo(String text) {
    	Map<String, String> map = new HashMap<>();
    	
	    String decoded = decodeEntities(text);
	
	    Pattern namePattern = Pattern.compile("<@([^>]+)>");
	    Matcher nameMatcher = namePattern.matcher(decoded);
	    String name = "";
	    if (nameMatcher.find()) {
	        name = nameMatcher.group(1).trim(); // 예: U047FP8V3NC
	    }
	
	    Pattern titlePattern = Pattern.compile("의\\s*([^>]+)>");
	    Matcher titleMatcher = titlePattern.matcher(decoded);
	    String title = "";
	    if (titleMatcher.find()) {
	        title = titleMatcher.group(1).trim();
	    }
	    
	    String type = "";
	    String platform = "";
	    if (!title.isEmpty()) {
	        String[] parts = title.split("\\s+", 2);
	        type = parts[0].trim();
	        if (parts.length > 1) {
	            platform = parts[1].trim();
	        }
	    }
	    
	    if (!platform.isBlank()) {
	        String[] toks = platform.split("\\s+");
	        if (toks[toks.length - 1].equals("업무공유")) {
	            platform = String.join(" ", Arrays.copyOf(toks, toks.length - 1)).trim();
	            if (platform.isBlank()) platform = "빈 값입니다.";
	        }
	    }
	    
	    Pattern starPattern = Pattern.compile("\\*(.*?)\\*");
	    Matcher starMatcher = starPattern.matcher(decoded);
	    StringBuilder detailBuilder = new StringBuilder();
	    if (starMatcher.find()) {
	        detailBuilder.append(starMatcher.group(1).trim()).append("\n");
	    }
	
	    String body = decoded
	            .replaceAll(">\\s*:exclamation:\\s*", "") // :exclamation: 제거
	            .replaceAll("\\*.*?\\*", "")               // *내용* 제거
	            .replaceAll("(?m)^<+<@.*?>.*$", "")        // "<<@...>" 라인 제거
	            .trim();
	
	    body = body.replaceAll("\\.(\\s+)", ".\n");
	
	    detailBuilder.append(body.trim());
	    String detail = detailBuilder.toString().trim();
	
	    map.put("name", name);
	    map.put("title", title);
	    map.put("type", type);
        map.put("platform", platform);
        map.put("detail", detail);
        
        String link = "";
        Pattern linkPattern = Pattern.compile("<(https[^>|]+)[>|]");
        Matcher linkMatcher = linkPattern.matcher(text);
        if (linkMatcher.find()) { link = linkMatcher.group(1).trim(); } else { link = "JIRA 링크데이터가 없습니다."; }
        map.put("link", link);
        
		return map;
    }
    
    public static String getRealName(String name,  String token) {
    	SlackUserResolver resolver = new SlackUserResolver(token);
    	return name = resolver.getSLackToUserID(name); // 실제 이름 변환
    }

    public static String getTimestamp(String message) {
    	String result = "";
        long epochSeconds = (long) Double.parseDouble(message);
        ZonedDateTime dateTime = Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.of("Asia/Seoul"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년MM월dd일_HH:mm:ss초");
        return result = dateTime.format(formatter);
    } 
    
}
