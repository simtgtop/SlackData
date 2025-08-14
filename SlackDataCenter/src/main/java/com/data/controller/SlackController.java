package com.data.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.data.repository.SlackLogRepository;
import com.data.service.SlackMessage;

@Component
public class SlackController {
	
	private static final Logger logger = LoggerFactory.getLogger(SlackController.class);

	@Autowired
	private final SlackMessage slackMessage;
	
	@SuppressWarnings("unused")
	private final SlackLogRepository repository;
	
	public SlackController(SlackLogRepository repository, SlackMessage slackMessage) {
		this.slackMessage = slackMessage;
		this.repository = repository;
    }
	
	@RequestMapping("/fetch")
	public void fetch() {
		SlackMessage.fetchMessages();
	}
	
	@Value("${Slack.call.time}")
	private long fixedRate;
	
	// 10초마다 fetch() 자동 실행
	@Scheduled(fixedRateString = "${Slack.call.time}") // 10,000ms = 10초
    public void scheduledFetch() {
		//logger.info("SLACK API 콜 주기 [ " + fixedRate/1000 + "] 초");
        fetch(); // 위의 fetch() 호출
        //logger.info("10초마다 fetch() 실행됨");
    }
	
}