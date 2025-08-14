package com.data.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlackLogUtil {

	private static final Logger logger = LoggerFactory.getLogger(SlackLogUtil.class);
	
	public static void logError(Throwable t) {
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    t.printStackTrace(pw); // Throwable에서 바로 호출 가능
	    String stackTraceString = sw.toString();
	    logger.error(stackTraceString);
	}


}
