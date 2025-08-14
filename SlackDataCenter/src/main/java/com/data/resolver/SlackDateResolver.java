package com.data.resolver;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class SlackDateResolver {

	// 최후 기본값 (모든 외부 설정/DB가 없을 때)
    private static final LocalDate CODE_DEFAULT = LocalDate.of(2025, 5, 19);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter KOREAN_TS_DATE = DateTimeFormatter.ofPattern("yyyy년MM월dd일");

    // 자동 생성 시 쓸 기본 내용
    private static final String CONF_TEMPLATE = String.join(System.lineSeparator(),
        "# 날짜형식 : 2025-05-19로 지정",
        "# 시작날짜를 변경하려면 위 형식으로 바꿔주세요.",
        "startDate = ",
        "defaultDate = 2025-05-19",
        ""
    );

    /**
     * 다음 수행 날짜 계산:
     *  - DB timestamp가 있으면: 그 날짜 + 1일
     *  - 없거나 파싱 실패: conf(startDate → defaultDate) 사용
     *  - conf도 없거나 값 없음: 코드 기본값
     *
     * @param dbTimestamp 예: "2025년05월21일_16:19:24초" (없으면 null/빈문자열)
     * @param explicitConfPath null이 아니면 해당 경로의 conf 사용/생성
     */
    public static LocalDate resolveCandidateDate(String dbTimestamp, String explicitConfPath) {
        // 1) DB 값 우선
        if (dbTimestamp != null && !dbTimestamp.isEmpty() && dbTimestamp.length() >= 11) {
            try {
                String datePart = dbTimestamp.substring(0, 11); // "yyyy년MM월dd일"
                LocalDate latest = LocalDate.parse(datePart, KOREAN_TS_DATE);
                return latest.plusDays(1);
            } catch (Exception ignore) {
                // 파싱 실패 시 conf/기본값으로 폴백
            }
        }

        // 2) conf 경로 결정 & 없으면 자동 생성
        Path confPath = resolveTargetConfPath(explicitConfPath);
        ensureConfExists(confPath);

        // 3) conf 읽기 (startDate → defaultDate)
        LocalDate fromConf = loadFromConfOrNull(confPath);
        if (fromConf != null) return fromConf;

        // 4) 최후 기본값
        return CODE_DEFAULT;
    }

    /** 자동 생성 대상 conf 경로를 결정 (존재 여부와 관계없이 "어디에 둘지" 선택) */
    private static Path resolveTargetConfPath(String explicitPathOrNull) {
        if (notBlank(explicitPathOrNull)) {
            return Paths.get(explicitPathOrNull.trim()).toAbsolutePath().normalize();
        }
        String sysProp = System.getProperty("slack.conf.path");
        if (notBlank(sysProp)) {
            return Paths.get(sysProp.trim()).toAbsolutePath().normalize();
        }
        String env = System.getenv("SLACK_CONF_PATH");
        if (notBlank(env)) {
            return Paths.get(env.trim()).toAbsolutePath().normalize();
        }
        // 기본: 현재 실행 위치
        return Paths.get(System.getProperty("user.dir"), "slackDate.conf").toAbsolutePath().normalize();
    }

    /** conf 파일이 없으면 생성 (부모 디렉토리 생성 포함, 기본 템플릿 작성) */
    private static void ensureConfExists(Path confPath) {
        try {
            Path parent = confPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(confPath)) {
                Files.writeString(confPath, CONF_TEMPLATE, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                System.out.println("[INFO] slackDate.conf 생성: " + confPath);
            } else {
                // 존재해도 템플릿 강제 덮어쓰지 않음
                // 필요시 업데이트 로직 추가 가능
            }
        } catch (IOException e) {
            System.err.println("[WARN] slackDate.conf 자동생성 실패: " + e.getMessage());
        }
    }

    /** conf에서 startDate → defaultDate 순으로 읽어 LocalDate 반환 (못 읽으면 null) */
    private static LocalDate loadFromConfOrNull(Path confPath) {
        if (!Files.exists(confPath)) return null;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(confPath.toFile())) {
            props.load(fis);
            LocalDate start = parseIsoOrNull(trimToNull(props.getProperty("startDate")));
            if (start != null) return start;

            LocalDate def = parseIsoOrNull(trimToNull(props.getProperty("defaultDate")));
            if (def != null) return def;

        } catch (Exception e) {
            System.err.println("[WARN] slackDate.conf 읽기/파싱 실패: " + e.getMessage());
        }
        return null;
    }

    // -------- helpers --------
    private static LocalDate parseIsoOrNull(String s) {
        if (s == null) return null;
        try { return LocalDate.parse(s, ISO_DATE); } catch (Exception e) { return null; }
    }
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    
}
