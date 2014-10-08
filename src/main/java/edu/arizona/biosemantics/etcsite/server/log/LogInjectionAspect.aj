package edu.arizona.biosemantics.etcsite.server.log;

/**
 * LogInjectionAspect specifies ILoggables and adds them a log method
 * @author rodenhausen
 */
public aspect LogInjectionAspect {
	
	/**
	 * log method is defined for ILoggables
	 * @param logLevel
	 * @param message
	 */
	public void ILoggable.log(LogLevel logLevel, String message) {
		Logger logger = Logger.getLogger(this.getClass());
		switch(logLevel) {
		case TRACE:
			logger.trace(message);
			break;
		case DEBUG:
			logger.debug(message);
			break;
		case INFO:
			logger.info(message);
			break;
		case ERROR:
			logger.error(message);
			break;
		case FATAL:
			logger.fatal(message);
			break;
		case WARN:
			logger.warn(message);
			break;
		}
	}
	
	/**
	 * log method is defined for ILoggables
	 * @param logLevel
	 * @param message
	 */
	public void ILoggable.log(LogLevel logLevel, String message, Throwable throwable) {
		Logger logger = Logger.getLogger(this.getClass());
		switch(logLevel) {
		case TRACE:
			logger.trace(message, throwable);
			break;
		case DEBUG:
			logger.debug(message, throwable);
			break;
		case INFO:
			logger.info(message, throwable);
			break;
		case ERROR:
			logger.error(message, throwable);
			break;
		case FATAL:
			logger.fatal(message, throwable);
			break;
		case WARN:
			logger.warn(message, throwable);
			break;
		}
	}
	
	/**
	 * ILoggable classes are specified below
	 */
	declare parents : edu.arizona.biosemantics.etcsite.client.* implements ILoggable;
	declare parents : edu.arizona.biosemantics.etcsite.client.content.visualization.* implements ILoggable;
	//.. more to add; some packages will cause error/exception to happen during compile for client code; need to figure out which
	declare parents : edu.arizona.biosemantics.etcsite.server..* implements ILoggable;
	declare parents : edu.arizona.biosemantics.etcsite.shared..* implements ILoggable;
}
