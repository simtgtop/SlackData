package com.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCode {
	
	    private static String decodeEntities(String s) {
	        if (s == null) return null;
	        return s.replace("&lt;", "<")
	                .replace("&gt;", ">")
	                .replace("&amp;", "&");
	    }

	    public static Map<String, String> getTitleTypePlatform(String text) {

	    	Map<String, String> map = new HashMap<>();
	    	
	        // 0) HTML 엔티티 디코딩
	        String decoded = decodeEntities(text);

	        // 1) title 추출
	        Pattern titlePattern = Pattern.compile("의\\s*([^>]+)>");
	        Matcher titleMatcher = titlePattern.matcher(decoded);
	        String title = "";
	        if (titleMatcher.find()) {
	            title = titleMatcher.group(1).trim();
	        }

	        // 2) platform 추출 (title에서 첫 단어 제외)
	        String platform = "";
	        if (!title.isEmpty()) {
	            String[] parts = title.split("\\s+", 2);
	            if (parts.length > 1) {
	                platform = parts[1].trim();
	            }
	        }
	        
	        if (!platform.isBlank()) {
	            // 끝 단어가 "업무공유"면 제거
	            String[] toks = platform.split("\\s+");
	            if (toks[toks.length - 1].equals("업무공유")) {
	                platform = String.join(" ", Arrays.copyOf(toks, toks.length - 1)).trim();
	                if (platform.isBlank()) platform = "빈 값입니다.";
	            }
	        }
	        

	        // 3) detail 추출
	        // 3-1) 첫 번째 줄: * 사이 값
	        Pattern starPattern = Pattern.compile("\\*(.*?)\\*");
	        Matcher starMatcher = starPattern.matcher(decoded);
	        StringBuilder detailBuilder = new StringBuilder();
	        if (starMatcher.find()) {
	            detailBuilder.append(starMatcher.group(1).trim()).append("\n");
	        }

	        // 3-2) 나머지 줄: . 있을 때마다 줄바꿈
	        // :exclamation: 제거
	        String body = decoded.replaceAll(">\\s*:exclamation:\\s*", "");
	        // 별표 안의 내용은 이미 추출했으니 제거
	        body = body.replaceAll("\\*.*?\\*", "");
	        // 첫 줄(title) 제거
	        body = body.replaceAll("^<@.*?>.*?\\n", "").trim();

	        // . 뒤에 줄바꿈
	        body = body.replaceAll("\\.(\\s+)", ".\n");

	        detailBuilder.append(body.trim());

	        String detail = detailBuilder.toString().trim();

	        // 결과 출력
	        System.out.println("title: " + title);
	        System.out.println("platform: " + platform);
	        System.out.println("detail:\n" + detail);
	    	
	        map.put("title", title);
	      //  map.put("type", type);
	        map.put("platform", platform);
	        
			return map;
	    }
	    
	    public static void main(String[] args) {
	    	
	    	TestCode main = new TestCode();
	    	
	        String raw = "&lt;<@U047FP8V3NC> 의 SMS Amazon CloudWatch 업무공유&gt;\n"
	                   + "&gt; :exclamation: *server 프로젝트 - 클라우드 모니터링 메뉴 무한로딩 현상*\n"
	                   + "이미지와 같이 서버 프로젝트 -&gt; 관리 -&gt; 클라우드 모니터링 메뉴에서 \"설치하기\" 버튼이 무한로딩 반복되는 현상은 메뉴 조회한 계정에 프로젝트 권한 중 \"수정\" 권한이 없는 경우 발생됩니다.\n\n"
	                   + "해당 현상 발생될 경우 권한 먼저 확인해보시면 좋을것 같습니다.";

	        // 0) HTML 엔티티 디코딩
	        String decoded = decodeEntities(raw);

	        // 1) name 추출 (@로 시작, > 전까지)
	        Pattern namePattern = Pattern.compile("<@([^>]+)>");
	        Matcher nameMatcher = namePattern.matcher(decoded);
	        String name = "";
	        if (nameMatcher.find()) {
	            name = nameMatcher.group(1).trim(); // 예: U047FP8V3NC
	        }

	        // 2) title 추출
	        Pattern titlePattern = Pattern.compile("의\\s*([^>]+)>");
	        Matcher titleMatcher = titlePattern.matcher(decoded);
	        String title = "";
	        if (titleMatcher.find()) {
	            title = titleMatcher.group(1).trim();
	        }
	        
	        // 3) type, platform 추출
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
	            // 끝 단어가 "업무공유"면 제거
	            String[] toks = platform.split("\\s+");
	            if (toks[toks.length - 1].equals("업무공유")) {
	                platform = String.join(" ", Arrays.copyOf(toks, toks.length - 1)).trim();
	                if (platform.isBlank()) platform = "빈 값입니다.";
	            }
	        }
	        
	        // 4) detail 추출
	        // 첫 줄: * 사이 값
	        Pattern starPattern = Pattern.compile("\\*(.*?)\\*");
	        Matcher starMatcher = starPattern.matcher(decoded);
	        StringBuilder detailBuilder = new StringBuilder();
	        if (starMatcher.find()) {
	            detailBuilder.append(starMatcher.group(1).trim()).append("\n");
	        }

	        // 본문 처리
	        String body = decoded
	                .replaceAll(">\\s*:exclamation:\\s*", "") // :exclamation: 제거
	                .replaceAll("\\*.*?\\*", "")               // *내용* 제거
	                .replaceAll("(?m)^<+<@.*?>.*$", "")        // "<<@...>" 라인 제거
	                .trim();

	        // . 뒤에 줄바꿈
	        body = body.replaceAll("\\.(\\s+)", ".\n");

	        detailBuilder.append(body.trim());
	        String detail = detailBuilder.toString().trim();

	        // 결과 출력
	        System.out.println("name: " + name);
	        System.out.println("title: " + title);
	        System.out.println("type: " + type);
	        System.out.println("platform: " + platform);
	        System.out.println("detail:\n" + detail);
	        
	        String link = "";
	        Pattern linkPattern = Pattern.compile("<(https[^>|]+)[>|]");
	        Matcher linkMatcher = linkPattern.matcher(raw);
	        if (linkMatcher.find()) { link = linkMatcher.group(1).trim(); } else { link = "JIRA 링크데이터가 없습니다."; }
	        
	        // 결과 출력
	        System.out.println("name : " + name);
	        System.out.println("title : " + title);
	        System.out.println("type : " + type);
	        System.out.println("platform : " + platform);
	        System.out.println("detail :\n" + detail);
	        System.out.println("link : " + link);
	        
	    }
	    
	    
}