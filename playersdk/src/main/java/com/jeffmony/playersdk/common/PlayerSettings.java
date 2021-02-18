package com.jeffmony.playersdk.common;

public class PlayerSettings {

    private boolean mLocalProxyEnable = false;     //是否打开本地代理开关

    public void setLocalProxyEnable(boolean enable) {
        mLocalProxyEnable = enable;
    }

    public boolean getLocalProxyEnable() {
        return mLocalProxyEnable;
    }
}
