/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2023 Paranoid Android
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

package com.android.server.biometrics.sensors.face.sense;

import android.annotation.NonNull;
import android.content.Context;
import android.hardware.face.IFaceServiceReceiver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;

import com.android.internal.util.Preconditions;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.GenerateChallengeClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import vendor.aospa.biometrics.face.ISenseService;

public class FaceGenerateChallengeClient extends GenerateChallengeClient<ISenseService> {

    private static final String TAG = "FaceGenerateChallengeClient";
    static final int CHALLENGE_TIMEOUT_SEC = 600; // 10 minutes
    private static final ClientMonitorCallback EMPTY_CALLBACK = new ClientMonitorCallback() {
    };

    private final long mCreatedAt;
    private List<IFaceServiceReceiver> mWaiting;
    private Long mChallengeResult;

    FaceGenerateChallengeClient(@NonNull Context context,
            @NonNull Supplier<ISenseService> lazyDaemon, @NonNull IBinder token,
            @NonNull ClientMonitorCallbackConverter listener, int userId, @NonNull String owner,
            int sensorId, @NonNull BiometricLogger logger,
            @NonNull BiometricContext biometricContext, long now) {
        super(context, lazyDaemon, token, listener, userId, owner, sensorId, logger,
                biometricContext);
        mCreatedAt = now;
        mWaiting = new ArrayList<>();
    }

    @Override
    protected void startHalOperation() {
        mChallengeResult = null;
        try {
            mChallengeResult = Long.valueOf(getFreshDaemon().generateChallenge(CHALLENGE_TIMEOUT_SEC));
            // send the result to the original caller via mCallback and any waiting callers
            // that called reuseResult
            sendChallengeResult(getListener(), mCallback);
            for (IFaceServiceReceiver receiver : mWaiting) {
                sendChallengeResult(new ClientMonitorCallbackConverter(receiver), EMPTY_CALLBACK);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "generateChallenge failed", e);
            mCallback.onClientFinished(this, false /* success */);
        } finally {
            mWaiting = null;
        }
    }

    /** @return An arbitrary time value for caching provided to the constructor. */
    public long getCreatedAt() {
        return mCreatedAt;
    }

    /**
     * Reuse the result of this operation when it is available. The receiver will be notified
     * immediately if a challenge has already been generated.
     *
     * @param receiver receiver to be notified of challenge result
     */
    public void reuseResult(@NonNull IFaceServiceReceiver receiver) {
        if (mWaiting != null) {
            mWaiting.add(receiver);
        } else {
            sendChallengeResult(new ClientMonitorCallbackConverter(receiver), EMPTY_CALLBACK);
        }
    }

    private void sendChallengeResult(@NonNull ClientMonitorCallbackConverter receiver,
            @NonNull ClientMonitorCallback ownerCallback) {
        Preconditions.checkState(mChallengeResult != null, "result not available");
        try {
            receiver.onChallengeGenerated(getSensorId(), getTargetUserId(), mChallengeResult);
            ownerCallback.onClientFinished(this, true /* success */);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
            ownerCallback.onClientFinished(this, false /* success */);
        }
    }
}
