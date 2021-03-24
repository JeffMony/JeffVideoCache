# JeffVideoCache
Better than AndroidVideoCache

参考 [中文开发文档](./README.md)

#### Development Document
> * 1.Realize the pre-loading function without the player
> * 2.Realize the video playback function while caching
> * 3.Realize the M3U8 video playback function while caching
> * 4.Realize the MP4 video playback function while caching
> * 5.Support the player such as exoplayer and ijkplayer
> * 6.Support okhttp library
> * 7.Support the function of continuing to cache to the local after dragging the progress bar
> * 8.Support LRU cleanup rules, you can set the expiration time of the cache

#### JeffVideoCache Architecture
![](./JeffVideoCache架构.png)

The core of JeffVideoCache is placed on the client, and the local server and client do a good job of data synchronization

##### 1.How to use

###### 1.1 Initialization
Set JeffVideoCache when the program starts ----> SDK initialization configuration
```
File saveFile = StorageUtils.getVideoFileDir(this);
if (!saveFile.exists()) {
    saveFile.mkdir();
}
VideoProxyCacheManager.Builder builder = new VideoProxyCacheManager.Builder().
        setFilePath(saveFile.getAbsolutePath()).    //File location
        setConnTimeOut(60 * 1000).                  //Connection timeout
        setReadTimeOut(60 * 1000).                  //Read timeout
        setExpireTime(2 * 24 * 60 * 60 * 1000).     //Expire timeout
        setMaxCacheSize(2 * 1024 * 1024 * 1024);    //Cache Size limit
VideoProxyCacheManager.getInstance().initProxyConfig(builder.build());
```
Initialization configuration:
> * 1.Set Cache File location
> * 2.Set the Connection timeout
> * 3.Set the Read timeout
> * 4.Set the cache Expire timeout
> * 5.Set the cache size limit

###### 1.2 Build local proxy url
```
playUrl = ProxyCacheUtils.getProxyUrl(uri.toString(), null, null);

public static String getProxyUrl(String videoUrl, Map<String, String> headers, Map<String, Object> cacheParams)
```
You can pass in headers, or pass in other additional parameters, according to your own needs

The constructed url is mainly base64 encoded

###### 1.3 Initiate a request
```
VideoProxyCacheManager.getInstance().startRequestVideoInfo(videoUrl, headers, extraParams);

public void startRequestVideoInfo(String videoUrl, Map<String, String> headers, Map<String, Object> extraParams)
```

###### 1.4 Set up cache listener
```
VideoProxyCacheManager.getInstance().addCacheListener(videoUrl, mListener);

mListener:

public interface IVideoCacheListener {

    void onCacheStart(VideoCacheInfo cacheInfo);

    void onCacheProgress(VideoCacheInfo cacheInfo);

    void onCacheError(VideoCacheInfo cacheInfo, int errorCode);

    void onCacheForbidden(VideoCacheInfo cacheInfo);

    void onCacheFinished(VideoCacheInfo cacheInfo);
}
```

###### 1.5 Set now playing link
```
VideoProxyCacheManager.getInstance().setPlayingUrlMd5(ProxyCacheUtils.computeMD5(videoUrl));
```

###### 1.6 Pause cache task
```
VideoProxyCacheManager.getInstance().pauseCacheTask(mVideoUrl);
```

###### 1.7 Resume cache task
```
VideoProxyCacheManager.getInstance().resumeCacheTask(mVideoUrl);
```

###### 1.8 Drag the video progress bar
```
long totalDuration = mPlayer.getDuration();
if (totalDuration > 0) {
    float percent = position * 1.0f / totalDuration;
    VideoProxyCacheManager.getInstance().seekToCacheTaskFromClient(mVideoUrl, percent);
}
```

###### 1.9 stop cache task and release resources
```
VideoProxyCacheManager.getInstance().stopCacheTask(mVideoUrl);
VideoProxyCacheManager.getInstance().releaseProxyReleases(mVideoUrl);
```
