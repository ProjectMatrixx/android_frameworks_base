/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.systemui.qs;

import com.android.systemui.qs.dagger.QSScope;
import com.android.systemui.util.ViewController;

import javax.inject.Inject;

import com.android.systemui.qs.QuickStatusBarHeader;
import com.android.systemui.qs.QuickQSPanelController;
import com.android.systemui.qs.QSPanelController;
import com.android.systemui.statusbar.connectivity.AccessPointController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.qs.tiles.dialog.BluetoothDialogFactory;
import com.android.systemui.qs.tiles.dialog.InternetDialogFactory;
import com.android.systemui.media.dialog.MediaOutputDialogFactory;

/**
 * Controller for {@link QuickStatusBarHeader}.
 */
@QSScope
class QuickStatusBarHeaderController extends ViewController<QuickStatusBarHeader> {

    private final QuickQSPanelController mQuickQSPanelController;
    private boolean mListening;

    @Inject
    QuickStatusBarHeaderController(
        QuickStatusBarHeader quickStatusBarHeader,
        QuickQSPanelController quickQSPanelController,
        QSPanelController qsPanelController,
        AccessPointController accessPointController,
        BluetoothController bluetoothController,
        BluetoothDialogFactory bluetoothDialogFactory,
        InternetDialogFactory internetDialogFactory,
        MediaOutputDialogFactory mediaOutputDialogFactory
    ) {
        super(quickStatusBarHeader);
        mQuickQSPanelController = quickQSPanelController;
        quickStatusBarHeader.mQSPanelController = qsPanelController;
        quickStatusBarHeader.mAccessPointController = accessPointController;
        quickStatusBarHeader.mInternetDialogFactory = internetDialogFactory;
        quickStatusBarHeader.mBluetoothController = bluetoothController;
        quickStatusBarHeader.mBluetoothDialogFactory = bluetoothDialogFactory;
        quickStatusBarHeader.mMediaOutputDialogFactory = mediaOutputDialogFactory;
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
        setListening(false);
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mListening = listening;

        mQuickQSPanelController.setListening(listening);

        if (mQuickQSPanelController.switchTileLayout(false)) {
            if (mView != null) {
                mView.updateResources();
            }
        }
    }

    public void setContentMargins(int marginStart, int marginEnd) {
        mQuickQSPanelController.setContentMargins(marginStart, marginEnd);
    }
}
