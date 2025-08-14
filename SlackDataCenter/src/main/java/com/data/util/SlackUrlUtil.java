package com.data.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.data.repository.SlackLogRepository;

public class SlackUrlUtil {

	private static final Logger logger = LoggerFactory.getLogger(SlackUrlUtil.class);
	
    private static LocalDate current;
    private static LocalDate end;
	
    public static String nextUrl() {
    	
    	current = getNextDate();
    	
        // 오늘 날짜 구하기
        LocalDate today = LocalDate.now();
        long daysBetween = ChronoUnit.DAYS.between(current, today);
        logger.info("오늘 날짜: [ {} ], 요청 날짜: [ {} ], 차이: [ {}일 ]", today, current, daysBetween);

        if(current.isAfter(today)) { logger.info("요청 날짜가 미래입니다. return 합니다."); return "DONE"; }
        
        String CHANNEL_ID = "C08MYS38FSA";
        String slackMainURL = "https://slack.com/api/conversations.history?";

        LocalDateTime start = current.atTime(0, 0, 0);
        LocalDateTime endTime = current.atTime(23, 59, 59);

        long oldest = toEpochSeconds(start);
        long latest = toEpochSeconds(endTime);

        String url = slackMainURL
                + "channel=" + CHANNEL_ID
                + "&oldest=" + oldest
                + "&latest=" + latest;

        current = current.plusDays(1); // 다음 날짜로 이동
        return url;
    }
    
    public static String convertToTimeUrl(String url) {
        Pattern pattern = Pattern.compile("(oldest|latest)=(\\d+)");
        Matcher matcher = pattern.matcher(url);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            long epochSeconds = Long.parseLong(matcher.group(2));
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSeconds),
                ZoneId.systemDefault()
            );
            String formattedDate = dateTime.format(formatter);
            matcher.appendReplacement(result, matcher.group(1) + "=" + formattedDate);
        }
        matcher.appendTail(result);
        return result.toString();
    }
    
    private static SlackLogRepository slackLogRepository;
    
    // 클래스 내부에 마지막 날짜 저장 (프로그램 실행 중에만 유지)
    private static LocalDate lastReturnedDate = null;

    public static LocalDate getNextDate() {
        String timestamp = slackLogRepository.findLatestTimestampAsString();

        LocalDate candidateDate = null;

        // (1) conf는 최초 한 번만 읽어서 confStartDate에 캐싱
        ensureConfLoadedOnce();
        if (confStartDate != null) {
            candidateDate = confStartDate;
        }

        // (2) conf에서 못 얻었으면: DB → 기본값 (기존 로직 유지)
        if (candidateDate == null) {
            if (timestamp != null && !timestamp.isEmpty()) {
                // "2025년05월21일_16:19:24초" → "2025년05월21일"
                String datePart = timestamp.substring(0, 11);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년MM월dd일");
                LocalDate latestDate = LocalDate.parse(datePart, formatter);
                candidateDate = latestDate.plusDays(1);
            } else {
                candidateDate = DEFAULT_DATE;
            }
        }

        // (3) lastReturnedDate 로직 (그대로 유지)
        if (lastReturnedDate == null) {
            lastReturnedDate = candidateDate;
        } else if (!lastReturnedDate.isBefore(candidateDate)) {
            lastReturnedDate = lastReturnedDate.plusDays(1);
        } else {
            lastReturnedDate = candidateDate;
        }

        return lastReturnedDate;
    }
    
    private static Path CONF_PATH = Paths.get(System.getProperty("user.dir"), "CRE_Date.conf");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final LocalDate DEFAULT_DATE = LocalDate.of(2025, 5, 19);

    // 최초 1회만 conf 로드/생성을 보장
    private static final AtomicBoolean CONF_INIT = new AtomicBoolean(false);
    private static volatile LocalDate confStartDate = null;

    private static void ensureConfLoadedOnce() {
        if (!CONF_INIT.compareAndSet(false, true)) {
            return; // 이미 초기화됨
        }
        
        logger.info("CRE_Date.conf 설정 파일 경로입니다. : {}", CONF_PATH.toAbsolutePath());
        
        try {
            if (Files.exists(CONF_PATH)) {
            	
            	List<String> lines;
				try {
					lines = readLinesWithFallback(CONF_PATH);
					String dateLine = lines.stream()
							.filter(l -> l.startsWith("startDate="))
							.map(l -> l.substring("startDate=".length()).trim())
							.findFirst()
							.orElse(null);
					
					if (dateLine != null && !dateLine.isEmpty()) {
						confStartDate = LocalDate.parse(dateLine, ISO);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
            } else {
                // 없으면: DEFAULT_DATE로 파일 생성 + 캐싱
                confStartDate = DEFAULT_DATE;
                String content = "# Default 2025-05-19일로 생성됩니다."  + System.lineSeparator()
                		+ "# generatedAt=" + LocalDateTime.now() + System.lineSeparator()
                		+ "startDate=" + DEFAULT_DATE.format(ISO);
                Files.write(CONF_PATH, content.getBytes(),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static List<String> readLinesWithFallback(Path path) throws Exception {
        // 1) UTF-8 시도
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            // 2) MS949(=CP949)로 폴백
            return Files.readAllLines(path, Charset.forName("MS949"));
        }
    }
	
    public static long toEpochSeconds(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();
    }
    
    
}
