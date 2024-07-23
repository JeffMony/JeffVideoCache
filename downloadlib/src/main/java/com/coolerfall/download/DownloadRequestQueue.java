package com.coolerfall.download;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Download request queue, this is designed according to RequestQueue in Andoird-Volley.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
final class DownloadRequestQueue {
  private static final String TAG = DownloadRequestQueue.class.getSimpleName();

  private static final int CAPACITY = 20;
  private static final int DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 3;

  /**
   * The set of all requests currently being processed by this DownloadQueue. A Request
   * will be in this set if it is waiting in any queue or currently being processed by
   * any dispatcher.
   */
  private final Set<DownloadRequest> currentRequests = new HashSet<>();
  private PriorityBlockingQueue<DownloadRequest> downloadQueue =
      new PriorityBlockingQueue<>(CAPACITY);
  private DownloadDispatcher[] dispatchers;
  private final DownloadDelivery delivery;
  private final AtomicInteger sequenceGenerator = new AtomicInteger();
  private final Logger logger;

  /**
   * Create the download dispatchers according to pool size. Any number higher than 10 or less
   * than 1, then the size will be default size
   *
   * @param threadPoolSize thread pool size of download dispatcher
   */
  DownloadRequestQueue(int threadPoolSize, Logger logger) {
    if (threadPoolSize < 1 || threadPoolSize > 10) {
      threadPoolSize = DEFAULT_DOWNLOAD_THREAD_POOL_SIZE;
    }

    this.logger = logger;
    dispatchers = new DownloadDispatcher[threadPoolSize];
    delivery = new DownloadDelivery(new Handler(Looper.getMainLooper()));
  }

  /**
   * Starts the dispatchers in this queue.
   */
  void start() {
    /* make sure any currently running dispatchers are stopped */
    stop();

    /* create the download dispatcher and start it. */
    for (int i = 0; i < dispatchers.length; i++) {
      DownloadDispatcher dispatcher = new DownloadDispatcher(downloadQueue, delivery, logger);
      dispatchers[i] = dispatcher;
      dispatcher.start();
    }

    logger.log("Thread pool size: " + dispatchers.length);
  }

  /**
   * Stops the download dispatchers.
   */
  void stop() {
    for (DownloadDispatcher dispatcher : dispatchers) {
      if (dispatcher != null) {
        dispatcher.quit();
      }
    }
  }

  /**
   * Add download request to the download request queue.
   *
   * @param request download request
   * @return true if the request is not in queue, otherwise return false
   */
  boolean add(DownloadRequest request) {
    /* if the request is downloading, do nothing */
    if (query(request.downloadId()) != DownloadState.INVALID
        || query(request.uri()) != DownloadState.INVALID) {
      Log.w(TAG, "the download requst is in downloading");
      return false;
    }

    /* tag the request as belonging to this queue */
    request.downloadRequestQueue(this);
    /* add it to the set of current requests */
    synchronized (currentRequests) {
      currentRequests.add(request);
    }

    /* process requests in the order they are added in */
    downloadQueue.add(request);

    return true;
  }

  /**
   * Cancel a download in progress.
   *
   * @param downloadId download id
   * @return true if download has canceled, otherwise return false
   */
  boolean cancel(int downloadId) {
    synchronized (currentRequests) {
      for (DownloadRequest request : currentRequests) {
        if (request.downloadId() == downloadId) {
          request.cancel();
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Cancel all the download.
   */
  void cancelAll() {
    synchronized (currentRequests) {
      for (DownloadRequest request : currentRequests) {
        request.cancel();
      }
    }

    currentRequests.clear();
  }

  /**
   * Get the downloading task size.
   *
   * @return task size
   */
  int getDownloadingSize() {
    return currentRequests.size();
  }

  /**
   * To check if the request is downloading according to download id.
   *
   * @param downloadId download id
   * @return true if the request is downloading, otherwise return false
   */
  DownloadState query(int downloadId) {
    synchronized (currentRequests) {
      for (DownloadRequest request : currentRequests) {
        if (request.downloadId() == downloadId) {
          return request.downloadState();
        }
      }
    }

    return DownloadState.INVALID;
  }

  /**
   * To check if the request is downloading according to download url.
   *
   * @param uri the uri to check
   * @return true if the request is downloading, otherwise return false
   */
  DownloadState query(Uri uri) {
    synchronized (currentRequests) {
      for (DownloadRequest request : currentRequests) {
        if (request.uri().toString().equals(uri.toString())) {
          return request.downloadState();
        }
      }
    }

    return DownloadState.INVALID;
  }

  /**
   * Gets a sequence number.
   *
   * @return return the sequence number
   */
  int getSequenceNumber() {
    return sequenceGenerator.incrementAndGet();
  }

  /**
   * The download has finished and remove from set.
   *
   * @param request download reqeust
   */
  void finish(DownloadRequest request) {
    synchronized (currentRequests) {
      currentRequests.remove(request);
    }
  }

  /**
   * Release all the resource.
   */
  void release() {
    /* release current download request */
    cancelAll();

    /* release download queue */
    if (downloadQueue != null) {
      downloadQueue = null;
    }

    /* release dispathcers */
    if (dispatchers != null) {
      stop();

      for (int i = 0; i < dispatchers.length; i++) {
        dispatchers[i] = null;
      }

      dispatchers = null;
    }
  }
}
