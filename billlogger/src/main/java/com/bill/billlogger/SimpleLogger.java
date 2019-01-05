package com.bill.billlogger;

import java.util.logging.Level;

/**
 * @author Bill
 *
 */
public class SimpleLogger implements Logger {

	  @Override
      public void log(Level level, String msg) {
          System.out.println("[" + level + "] " + msg);
      }

      @Override
      public void log(Level level, String msg, Throwable th) {
          System.out.println("[" + level + "] " + msg);
          th.printStackTrace(System.out);
      }

}
