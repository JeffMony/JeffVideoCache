package com.coolerfall.download;

/**
 * Download request will be processed from higher priorities to lower priorities.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public enum Priority {
  LOW,   /* The lowest priority. */

  NORMAL, /* Normal priority(default). */

  HIGH, /* The highest priority. */
}
