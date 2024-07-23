package com.coolerfall.download;

/**
 * A simple indirection for logging debug messages.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public interface Logger {
  Logger EMPTY = new Logger() {
    @Override public void log(String message) {
    }
  };

  /**
   * Output log with given message.
   *
   * @param message message
   */
  void log(String message);
}
