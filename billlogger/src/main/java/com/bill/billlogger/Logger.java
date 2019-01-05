package com.bill.billlogger;

import java.util.logging.Level;

/**
 * @author Bill
 *
 */
public interface Logger {

	void log(Level level, String msg);

	void log(Level level, String msg, Throwable th);

}
