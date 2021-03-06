/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.media;

import dalvik.system.CloseGuard;

import android.os.Handler;
import android.view.Surface;

///M:
import android.util.Slog;

/**
 * Listens for Wifi remote display connections managed by the media server.
 *
 * @hide
 */
public final class RemoteDisplay {
    private static final String TAG = "RemoteDisplay";

    /* these constants must be kept in sync with IRemoteDisplayClient.h */

    public static final int DISPLAY_FLAG_SECURE = 1 << 0;

    public static final int DISPLAY_ERROR_UNKOWN = 1;
    public static final int DISPLAY_ERROR_CONNECTION_DROPPED = 2;

    private final CloseGuard mGuard = CloseGuard.get();
    private final Listener mListener;
    private final Handler mHandler;

    private int mPtr;

    private native int nativeListen(String iface);
    private native void nativeDispose(int ptr);

    private RemoteDisplay(Listener listener, Handler handler) {
        mListener = listener;
        mHandler = handler;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose(true);
        } finally {
            super.finalize();
        }
    }

    /**
     * Starts listening for displays to be connected on the specified interface.
     *
     * @param iface The interface address and port in the form "x.x.x.x:y".
     * @param listener The listener to invoke when displays are connected or disconnected.
     * @param handler The handler on which to invoke the listener.
     */
    public static RemoteDisplay listen(String iface, Listener listener, Handler handler) {
        Slog.d(TAG, "listen");
        if (iface == null) {
            throw new IllegalArgumentException("iface must not be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }

        RemoteDisplay display = new RemoteDisplay(listener, handler);
        display.startListening(iface);
        return display;
    }

    /**
     * Disconnects the remote display and stops listening for new connections.
     */
    public void dispose() {
        Slog.d(TAG, "dispose");
        dispose(false);
    }

    private void dispose(boolean finalized) {
        Slog.d(TAG, "dispose");
        if (mPtr != 0) {
            if (mGuard != null) {
                if (finalized) {
                    mGuard.warnIfOpen();
                } else {
                    mGuard.close();
                }
            }

            nativeDispose(mPtr);
            mPtr = 0;
        }
    }

    private void startListening(String iface) {
        Slog.d(TAG, "startListening");
        mPtr = nativeListen(iface);
        if (mPtr == 0) {
            throw new IllegalStateException("Could not start listening for "
                    + "remote display connection on \"" + iface + "\"");
        }
        mGuard.open("dispose");
    }

    // Called from native.
    private void notifyDisplayConnected(final Surface surface,
            final int width, final int height, final int flags) {
        Slog.d(TAG, "notifyDisplayConnected");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onDisplayConnected(surface, width, height, flags);
            }
        });
    }

    // Called from native.
    private void notifyDisplayDisconnected() {
        Slog.d(TAG, "notifyDisplayDisconnected");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onDisplayDisconnected();
            }
        });
    }

    // Called from native.
    private void notifyDisplayError(final int error) {
        Slog.d(TAG, "notifyDisplayError");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onDisplayError(error);
            }
        });
    }

    ///M: add by MTK @{
    private void  notifyDisplayKeyEvent(final int keyCode, final int flags) {
        Slog.d(TAG, "notifyDisplayKeyEvent");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onDisplayKeyEvent(keyCode,flags);
            }
        });
    }

    private void notifyDisplayTouchEvent(final int x, final int y, final int flags){
        Slog.d(TAG, "notifyDisplayTouchEvent");
         mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onDisplayTouchEvent(x,y,flags);
            }
        });
    }

    private void notifyDisplayGenericMsgEvent(final int event){
        Slog.d(TAG, "notifyDisplayGenericMsgEvent");
         mHandler.post(new Runnable() {
            @Override
            public void run() {
                mListener.onDisplayGenericMsgEvent(event);
            }
        });
    }    
    ///@}

    /**
     * Listener invoked when the remote display connection changes state.
     */
    public interface Listener {
        void onDisplayConnected(Surface surface, int width, int height, int flags);
        void onDisplayDisconnected();
        void onDisplayError(int error);
        ///M: add by MTK@{
        void onDisplayKeyEvent(int keyCode, int flags);
        void onDisplayTouchEvent(int x, int y, int flags);
        void onDisplayGenericMsgEvent(int event);
        ///@}
    }
}
