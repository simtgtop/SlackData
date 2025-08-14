package com.data.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.data.entity.SlackEntity;

public interface SlackLogRepository extends JpaRepository<SlackEntity, Long> {

    boolean existsByDetail(String detail);
    
    static final Logger logger = LoggerFactory.getLogger(SlackLogRepository.class);
    
    @Query(value = "SELECT timestamp FROM data ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    String findLatestTimestampAsString();

    // 기존 JDBC insert+중복체크를 JPA로 통합! (builder 미사용)
    default void insertIfNotExists(
            String title,
            String detail,
            String type,
            String name,
            String platform,
            String link,
            String timestamp) {
    	
    	if (!existsByDetail(detail)) {
            SlackEntity log = new SlackEntity();
            log.setTitle(title);
            log.setDetail(detail);
            log.setType(type);
            log.setName(name);
            log.setPlatform(platform);
            log.setLink(link);
            log.setTimestamp(timestamp);
            try {
            	save(log);
            	logger.info("✅ Slack 데이터 DB INSERT 성공! - END");
            } catch(Exception e) {
            	logger.info("✅ Slack 데이터 DB INSERT 실패! - END");
            	e.printStackTrace();
            }
    	} else {
        	logger.info("✅ Slack 데이터 Detail 데이터가 존재하여 INSERT SKIP {}  - END ", title);
    	}

    }

}
