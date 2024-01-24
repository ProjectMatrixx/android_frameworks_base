/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.systemui.qs

import android.content.Context
import android.testing.AndroidTestingRunner
import androidx.test.filters.SmallTest
import com.android.systemui.SysuiTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

import com.android.systemui.qs.QuickStatusBarHeader
import com.android.systemui.qs.QuickQSPanelController
import com.android.systemui.qs.QSPanelController
import com.android.systemui.statusbar.connectivity.AccessPointController
import com.android.systemui.statusbar.policy.BluetoothController
import com.android.systemui.qs.tiles.dialog.BluetoothDialogFactory
import com.android.systemui.qs.tiles.dialog.InternetDialogFactory
import com.android.systemui.statusbar.NotificationMediaManager
import android.media.session.MediaSessionManager
import com.android.systemui.media.dialog.MediaOutputDialogFactory

@SmallTest
@RunWith(AndroidTestingRunner::class)
class QuickStatusBarHeaderControllerTest : SysuiTestCase() {

    @Mock
    private lateinit var quickStatusBarHeader: QuickStatusBarHeader
    @Mock
    private lateinit var quickQSPanelController: QuickQSPanelController
    @Mock
    private lateinit var qsPanelController: QSPanelController
    @Mock
    private lateinit var accessPointController: AccessPointController
    @Mock
    private lateinit var bluetoothController: BluetoothController
    @Mock
    private lateinit var bluetoothDialogFactory: BluetoothDialogFactory
    @Mock
    private lateinit var internetDialogFactory: InternetDialogFactory
    @Mock
    private lateinit var notificationMediaManager: NotificationMediaManager
    @Mock
    private lateinit var mediaSessionManager: MediaSessionManager
    @Mock
    private lateinit var mediaOutputDialogFactory: MediaOutputDialogFactory

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var context: Context

    private lateinit var controller: QuickStatusBarHeaderController

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(quickStatusBarHeader.resources).thenReturn(mContext.resources)
        `when`(quickStatusBarHeader.isAttachedToWindow).thenReturn(true)
        `when`(quickStatusBarHeader.context).thenReturn(context)

        controller = QuickStatusBarHeaderController(
            quickStatusBarHeader,
            quickQSPanelController,
            qsPanelController,
            accessPointController,
            bluetoothController,
            bluetoothDialogFactory,
            internetDialogFactory,
            notificationMediaManager,
            mediaSessionManager,
            mediaOutputDialogFactory
        )
    }

    @After
    fun tearDown() {
        controller.onViewDetached()
    }

    @Test
    fun testListeningStatus() {
        controller.setListening(true)
        verify(quickQSPanelController).setListening(true)

        controller.setListening(false)
        verify(quickQSPanelController).setListening(false)
    }
}
