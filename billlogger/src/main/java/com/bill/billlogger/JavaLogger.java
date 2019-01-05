package com.bill.billlogger;

import java.util.logging.Level;

/**
 * @author Bill
 *
 */
public class JavaLogger implements Logger {

	  protected final java.util.logging.Logger logger;

      public JavaLogger(String tag) {
          logger = java.util.logging.Logger.getLogger(tag);
      }

      @Override
      public void log(Level level, String msg) {
          logger.log(level, msg);
      }

      @Override
      public void log(Level level, String msg, Throwable th) {
          logger.log(level, msg, th);
      }


}
