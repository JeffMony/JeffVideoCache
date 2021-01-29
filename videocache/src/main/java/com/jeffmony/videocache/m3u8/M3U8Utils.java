package com.jeffmony.videocache.m3u8;


import android.text.TextUtils;

import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.UrlUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jeffmony
 *
 * M3U8的通用处理类
 */
public class M3U8Utils {

    public static M3U8 parseNetworkM3U8Info(String videoUrl, Map<String, String> headers) throws IOException {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            HttpURLConnection connection = HttpUtils.getConnection(videoUrl, headers);
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            M3U8 m3u8 = new M3U8(videoUrl);
            int targetDuration = 0;
            int version = 0;
            int sequence = 0;
            boolean hasDiscontinuity = false;
            boolean hasEndList = false;
            boolean hasMasterList = false;
            boolean hasKey = false;
            String method = null;
            String keyIv = null;
            String keyUrl = null;
            float tsDuration = 0;
            int tsIndex = 0;

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (TextUtils.isEmpty(line)) {
                    continue;
                }
                if (line.startsWith(Constants.TAG_PREFIX)) {
                    if (line.startsWith(Constants.TAG_MEDIA_DURATION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_MEDIA_DURATION);
                        if (!TextUtils.isEmpty(ret)) {
                            tsDuration = Float.parseFloat(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_TARGET_DURATION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_TARGET_DURATION);
                        if (!TextUtils.isEmpty(ret)) {
                            targetDuration = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_VERSION)) {
                        String ret = parseStringAttr(line, Constants.REGEX_VERSION);
                        if (!TextUtils.isEmpty(ret)) {
                            version = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_MEDIA_SEQUENCE)) {
                        String ret = parseStringAttr(line, Constants.REGEX_MEDIA_SEQUENCE);
                        if (!TextUtils.isEmpty(ret)) {
                            sequence = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(Constants.TAG_STREAM_INF)) {
                        hasMasterList = true;
                    } else if (line.startsWith(Constants.TAG_DISCONTINUITY)) {
                        hasDiscontinuity = true;
                    } else if (line.startsWith(Constants.TAG_ENDLIST)) {
                        hasEndList = true;
                    } else if (line.startsWith(Constants.TAG_KEY)) {
                        hasKey = true;
                        method = parseOptionalStringAttr(line, Constants.REGEX_METHOD);
                        String keyFormat = parseOptionalStringAttr(line, Constants.REGEX_KEYFORMAT);
                        if (!Constants.METHOD_NONE.equals(method)) {
                            keyIv = parseOptionalStringAttr(line, Constants.REGEX_IV);
                            if (Constants.KEYFORMAT_IDENTITY.equals(keyFormat) || keyFormat == null) {
                                if (Constants.METHOD_AES_128.equals(method)) {
                                    // The segment is fully encrypted using an identity key.
                                    String tempKeyUri = parseStringAttr(line, Constants.REGEX_URI);
                                    if (tempKeyUri != null) {
                                        keyUrl = UrlUtils.getM3U8MasterUrl(videoUrl, tempKeyUri);
                                    }
                                } else {
                                    // Do nothing. Samples are encrypted using an identity key,
                                    // but this is not supported. Hopefully, a traditional DRM
                                    // alternative is also provided.
                                }
                            } else {
                                // Do nothing.
                            }
                        }
                    }
                    continue;
                }

                // It has '#EXT-X-STREAM-INF' tag;
                if (hasMasterList) {
                    String tempUrl = UrlUtils.getM3U8MasterUrl(videoUrl, line);
                    return parseNetworkM3U8Info(tempUrl, headers);
                }

                if (Math.abs(tsDuration - 0.0f) < 0.0001f) {
                    continue;
                }

                M3U8Ts ts = new M3U8Ts();
                String tempUrl = UrlUtils.getM3U8MasterUrl(videoUrl, line);
                ts.setUrl(tempUrl);
                ts.setTsIndex(tsIndex);
                ts.setDuration(tsDuration);
                ts.setHasDiscontinuity(hasDiscontinuity);
                ts.setHasKey(hasKey);
                if (hasKey) {
                    ts.setMethod(method);
                    ts.setKeyIv(keyIv);
                    ts.setKeyUrl(keyUrl);
                }
                m3u8.addTs(ts);
                tsIndex++;
                tsDuration = 0;
                hasDiscontinuity = false;
                hasKey = false;
                method = null;
                keyUrl = null;
                keyIv = null;
            }

            m3u8.setTargetDuration(targetDuration);
            m3u8.setVersion(version);
            m3u8.setSequence(sequence);
            m3u8.setIsLive(!hasEndList);
            return m3u8;
        } catch (IOException e) {
            throw e;
        } finally {
            ProxyCacheUtils.close(inputStreamReader);
            ProxyCacheUtils.close(bufferedReader);
        }
    }


    public static String parseStringAttr(String line, Pattern pattern) {
        if (pattern == null)
            return null;
        Matcher matcher = pattern.matcher(line);
        if (matcher.find() && matcher.groupCount() == 1) {
            return matcher.group(1);
        }
        return null;
    }

    private static String parseOptionalStringAttr(String line, Pattern pattern) {
        if (pattern == null)
            return null;
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

}
