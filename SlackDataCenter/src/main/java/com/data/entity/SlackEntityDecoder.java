package com.data.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class SlackEntityDecoder {

    private static final Map<String, String> REPLACEMENTS = new LinkedHashMap<>();
    static {
        // 순서 중요: &amp; / %amp; 를 먼저 처리해야 뒤의 &gt; 등과 충돌 없음
        REPLACEMENTS.put("%amp;", "&");
        REPLACEMENTS.put("&amp;", "&");

        REPLACEMENTS.put("%gt;", ">");
        REPLACEMENTS.put("&gt;", ">");

        REPLACEMENTS.put("%lt;", "<");
        REPLACEMENTS.put("&lt;", "<");
    }

    /** Slack 메시지에 섞여 들어온 %gt; / %lt; / %amp; 와 일반 HTML 엔티티를 실제 기호로 복원 */
    public static String decode(String text) {
        if (text == null || text.isEmpty()) return text;
        String out = text;
        for (Map.Entry<String, String> e : REPLACEMENTS.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }
}