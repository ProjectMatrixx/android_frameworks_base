/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.UserHandle;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.TransitionDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.UserHandle;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.Space;
import android.widget.TextView;

import com.android.internal.graphics.ColorUtils;

import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.crdroid.header.StatusBarHeaderMachine;
import com.android.systemui.util.LargeScreenUtils;

import com.bosphere.fadingedgelayout.FadingEdgeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.Exception;
import java.lang.Math;
import java.util.Iterator;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.plugins.ActivityStarter;
import android.media.session.MediaController;
import android.media.session.MediaSessionLegacyHelper;
import android.net.ConnectivityManager;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.qs.TouchAnimator.Builder;
import com.android.internal.util.palette.Palette;

import com.android.systemui.qs.QSPanelController;
import com.android.systemui.statusbar.connectivity.AccessPointController;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.qs.tiles.dialog.BluetoothDialogFactory;
import com.android.systemui.qs.tiles.dialog.InternetDialogFactory;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.media.dialog.MediaOutputDialogFactory;

/**
 * View that contains the top-most bits of the QS panel (primarily the status bar with date, time,
 * battery, carrier info and privacy icons) and also contains the {@link QuickQSPanel}.
 */
public class QuickStatusBarHeader extends FrameLayout
            implements StatusBarHeaderMachine.IStatusBarHeaderMachineObserver,BluetoothCallback, NotificationMediaManager.MediaListener, Palette.PaletteAsyncListener, View.OnClickListener, View.OnLongClickListener {

    private boolean mExpanded;
    private boolean mQsDisabled;

    protected QuickQSPanel mHeaderQsPanel;
    public float mKeyguardExpansionFraction;

    // QS Header
    private ImageView mQsHeaderImageView;
    private FadingEdgeLayout mQsHeaderLayout;
    private boolean mHeaderImageEnabled;
    private StatusBarHeaderMachine mStatusBarHeaderMachine;
    private Drawable mCurrentBackground;
    private int mHeaderImageHeight;
    //private final Handler mHandler = new Handler();

    private class OmniSettingsObserver extends ContentObserver {
        OmniSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getContext().getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_CUSTOM_HEADER), false,
                    this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CUSTOM_HEADER_HEIGHT), false,
                    this, UserHandle.USER_ALL);
            }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
    private OmniSettingsObserver mOmniSettingsObserver;

    private int colorActive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorAccent);
    private int colorInactive = Utils.getColorAttrDefaultColor(mContext, R.attr.offStateColor);
    private int colorLabelActive = Utils.getColorAttrDefaultColor(mContext, com.android.internal.R.attr.textColorPrimaryInverse);
    private int colorLabelInactive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);

    private int mColorArtwork = Color.BLACK;
    private int mMediaTextIconColor = Color.WHITE;

    private ViewGroup mOpQsContainer;
    private ViewGroup mOpQsLayout;

    private ViewGroup mBluetoothButton;
    private ImageView mBluetoothIcon;
    private TextView mBluetoothText;
    private ImageView mBluetoothChevron;
    private boolean mBluetoothEnabled;

    private ViewGroup mInternetButton;
    private ImageView mInternetIcon;
    private TextView mInternetText;
    private ImageView mInternetChevron;
    private boolean mInternetEnabled;

    private ImageView mMediaPlayerBackground;
    private ImageView mAppIcon, mMediaOutputSwitcher;
    private TextView mMediaPlayerTitle, mMediaPlayerSubtitle;
    private ImageButton mMediaBtnPrev, mMediaBtnNext, mMediaBtnPlayPause;

    private String mMediaTitle, mMediaArtist;
    private Bitmap mMediaArtwork;
    private boolean mMediaIsPlaying;

    private final ActivityStarter mActivityStarter;
    private final ConnectivityManager mConnectivityManager;
    private final SubscriptionManager mSubManager;
    private final WifiManager mWifiManager;
    private final NotificationMediaManager mNotificationMediaManager;

    public TouchAnimator mQQSContainerAnimator;

    public QSPanelController mQSPanelController;
    public BluetoothController mBluetoothController;
    public BluetoothDialogFactory mBluetoothDialogFactory;
    public InternetDialogFactory mInternetDialogFactory;
    public AccessPointController mAccessPointController;
    public MediaOutputDialogFactory mMediaOutputDialogFactory;

    private final Handler mHandler;
    private Runnable mUpdateRunnableBluetooth;
    private Runnable mUpdateRunnableInternet;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStatusBarHeaderMachine = new StatusBarHeaderMachine(context);
        mHandler = new Handler(Looper.getMainLooper());
        mOmniSettingsObserver = new OmniSettingsObserver(mHandler);
        mOmniSettingsObserver.observe();
        mBluetoothEnabled = false;
        mInternetEnabled = false;
        mMediaIsPlaying = false;
        mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mSubManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mNotificationMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderQsPanel = findViewById(R.id.quick_qs_panel);

        mQsHeaderLayout = findViewById(R.id.layout_header);
        mQsHeaderImageView = findViewById(R.id.qs_header_image_view);
        mQsHeaderImageView.setClipToOutline(true);

        mOpQsContainer = findViewById(R.id.qs_container);
        mOpQsLayout = findViewById(R.id.qs_op_header_layout);
        mInternetButton = findViewById(R.id.qs_op_button_internet);
        mInternetIcon = findViewById(R.id.qs_op_internet_icon);
        mInternetText = findViewById(R.id.qs_op_internet_text);
        mInternetChevron = findViewById(R.id.qs_op_chevron_internet);
        mBluetoothButton = findViewById(R.id.qs_op_button_bluetooth);
        mBluetoothIcon = findViewById(R.id.qs_op_bluetooth_icon);
        mBluetoothText = findViewById(R.id.qs_op_bluetooth_text);
        mBluetoothChevron = findViewById(R.id.qs_op_chevron_bluetooth);

        mMediaPlayerBackground = findViewById(R.id.qs_op_media_player_bg);
        mAppIcon = findViewById(R.id.op_media_player_app_icon);
        mMediaOutputSwitcher = findViewById(R.id.op_media_player_output_switcher);
        mMediaPlayerTitle = findViewById(R.id.op_media_player_title);
        mMediaPlayerSubtitle = findViewById(R.id.op_media_player_subtitle);
        mMediaBtnPrev = findViewById(R.id.op_media_player_action_prev);
        mMediaBtnNext = findViewById(R.id.op_media_player_action_next);
        mMediaBtnPlayPause = findViewById(R.id.op_media_player_action_play_pause);

        mNotificationMediaManager.addCallback(this);

        initBluetoothManager();

        mMediaPlayerBackground.setOnClickListener(this);
        mMediaOutputSwitcher.setOnClickListener(this);
        mMediaBtnPrev.setOnClickListener(this);
        mMediaBtnNext.setOnClickListener(this);
        mMediaBtnPlayPause.setOnClickListener(this);

        mInternetButton.setOnClickListener(this);
        mBluetoothButton.setOnClickListener(this);

        mInternetButton.setOnLongClickListener(this);
        mBluetoothButton.setOnLongClickListener(this);

        updateSettings();

        startUpdateInterntTileStateAsync();
        startUpdateBluetoothTileStateAsync();
    }

    private void initBluetoothManager() {
        LocalBluetoothManager localBluetoothManager = LocalBluetoothManager.getInstance(mContext, null);

        if (localBluetoothManager != null) {
            localBluetoothManager.getEventManager().registerCallback(this);
            LocalBluetoothAdapter localBluetoothAdapter = localBluetoothManager.getBluetoothAdapter();
            int bluetoothState = BluetoothAdapter.STATE_DISCONNECTED;

            synchronized (localBluetoothAdapter) {
                if (localBluetoothAdapter.getAdapter().getState() != localBluetoothAdapter.getBluetoothState()) {
                    localBluetoothAdapter.setBluetoothStateInt(localBluetoothAdapter.getAdapter().getState());
                }
                bluetoothState = localBluetoothAdapter.getBluetoothState();
            }
            updateBluetoothState(bluetoothState);
        }
    }

    @Override
    public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
        updateBluetoothState(bluetoothState);
    }

    private void updateBluetoothState(@AdapterState int bluetoothState) {
        mBluetoothEnabled = bluetoothState == BluetoothAdapter.STATE_ON
                || bluetoothState == BluetoothAdapter.STATE_TURNING_ON;
        updateBluetoothTile();
    }

    public final void updateBluetoothTile() {
        if (mBluetoothButton == null
                || mBluetoothIcon == null
                || mBluetoothText == null
                || mBluetoothChevron == null)
            return;
        Drawable background = mBluetoothButton.getBackground();
        if (mBluetoothEnabled) {
            background.setTint(colorActive);
            mBluetoothIcon.setColorFilter(colorLabelActive);
            mBluetoothText.setTextColor(colorLabelActive);
            mBluetoothChevron.setColorFilter(colorLabelActive);
        } else {
            background.setTint(colorInactive);
            mBluetoothIcon.setColorFilter(colorLabelInactive);
            mBluetoothText.setTextColor(colorLabelInactive);
            mBluetoothChevron.setColorFilter(colorLabelInactive);
        }
    }

    public void updateInterntTile() {
        if (mInternetButton == null
                || mInternetIcon == null
                || mInternetText == null
                || mInternetChevron == null)
            return;

        String carrier;
        int iconResId = 0;

        if (isWifiConnected()) {
            carrier = getWifiSsid();
            mInternetEnabled = true;
            iconResId = mContext.getResources().getIdentifier("ic_wifi_signal_4", "drawable", "android");
        } else {
            carrier = getSlotCarrierName();
            mInternetEnabled = true;
            iconResId = mContext.getResources().getIdentifier("ic_signal_cellular_4_4_bar", "drawable", "android");
        }

        mInternetText.setText(carrier);
        mInternetIcon.setImageResource(iconResId);

        Drawable background = mInternetButton.getBackground();

        if (mInternetEnabled) {
            background.setTint(colorActive);
            mInternetIcon.setColorFilter(colorLabelActive);
            mInternetText.setTextColor(colorLabelActive);
            mInternetChevron.setColorFilter(colorLabelActive);
        } else {
            background.setTint(colorInactive);
            mInternetIcon.setColorFilter(colorLabelInactive);
            mInternetText.setTextColor(colorLabelInactive);
            mInternetChevron.setColorFilter(colorLabelInactive);
        }
    }

    private boolean isWifiConnected() {
        final Network network = mConnectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            return false;
        }
    }

    private String getSlotCarrierName() {
        CharSequence result = mContext.getResources().getString(R.string.quick_settings_internet_label);
        int subId = mSubManager.getDefaultDataSubscriptionId();
        final List<SubscriptionInfo> subInfoList = mSubManager.getActiveSubscriptionInfoList(true);
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subId == subInfo.getSubscriptionId()) {
                    result = subInfo.getDisplayName();
                    break;
                }
            }
        }
        return result.toString();
    }

    private String getWifiSsid() {
        final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo.getHiddenSSID() || wifiInfo.getSSID() == WifiManager.UNKNOWN_SSID) {
            return mContext.getResources().getString(R.string.quick_settings_wifi_label);
        } else {
            return wifiInfo.getSSID().replace("\"", "");
        }
    }

    @Override
    public void onPrimaryMetadataOrStateChanged(MediaMetadata metadata, @PlaybackState.State int state) {
        if (metadata != null) {
            CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);

            mMediaTitle = title != null ? title.toString() : null;
            mMediaArtist = artist != null ? artist.toString() : null;
            Bitmap albumArtwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
	    Bitmap mediaArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
            mMediaArtwork = (albumArtwork != null) ? albumArtwork : mediaArt;

            if (mMediaArtwork != null) {
                Palette.generateAsync(mMediaArtwork, this);
            }
        } else {
            mMediaTitle = null;
            mMediaArtist = null;
            mMediaArtwork = null;
        }

        if (mMediaArtwork == null) {
            mMediaPlayerTitle.setTextColor(colorLabelInactive);
            mMediaPlayerSubtitle.setTextColor(colorLabelInactive);
            mMediaBtnPrev.setColorFilter(colorLabelInactive);
            mMediaBtnPlayPause.setColorFilter(colorLabelInactive);
            mMediaBtnNext.setColorFilter(colorLabelInactive);
            mMediaOutputSwitcher.setColorFilter(colorLabelInactive);
            mMediaPlayerBackground.setColorFilter(null);
        }

        mMediaIsPlaying = state == PlaybackState.STATE_PLAYING;

        updateMediaPlayer();
    }

    @Override
    public void setMediaNotificationColor(int color) {
    }

    @Override
    public void onGenerated(Palette palette) {
        int mShadow = 120;
        int alphaValue = 100;
        mColorArtwork = ColorUtils.setAlphaComponent(palette.getDarkVibrantColor(Color.BLACK), mShadow);
        int mMediaOutputIconColor = palette.getLightVibrantColor(Color.WHITE);

        mMediaPlayerTitle.setTextColor(mMediaTextIconColor);
        mMediaPlayerSubtitle.setTextColor(mMediaTextIconColor);
        mMediaBtnPrev.setColorFilter(mMediaTextIconColor);
        mMediaBtnPlayPause.setColorFilter(mMediaTextIconColor);
        mMediaBtnNext.setColorFilter(mMediaTextIconColor);
        mMediaOutputSwitcher.setColorFilter(mMediaOutputIconColor);
        mMediaPlayerBackground.setColorFilter(ColorUtils.setAlphaComponent(mColorArtwork, alphaValue), PorterDuff.Mode.SRC_ATOP);

    }

    private void updateMediaPlayer() {
        if (mMediaPlayerBackground == null
                || mAppIcon == null
                || mMediaOutputSwitcher == null
                || mMediaPlayerTitle == null
                || mMediaPlayerSubtitle == null
                || mMediaBtnPrev == null
                || mMediaBtnNext == null
                || mMediaBtnPlayPause == null)
            return;

        mMediaPlayerTitle.setText(mMediaTitle == null ?
                                    mContext.getResources().getString(R.string.op_media_player_default_title) : mMediaTitle);
        mMediaPlayerSubtitle.setText(mMediaArtist == null ? "" : mMediaArtist);
        mMediaPlayerSubtitle.setVisibility(mMediaArtist == null ? View.GONE : View.VISIBLE);

        if (mMediaIsPlaying) {
            mMediaBtnPlayPause.setImageResource(R.drawable.ic_op_media_player_action_pause);
        } else {
            mMediaBtnPlayPause.setImageResource(R.drawable.ic_op_media_player_action_play);
        }

        if (mNotificationMediaManager != null && mNotificationMediaManager.getMediaIcon() != null
                && mMediaTitle != null) {
            mAppIcon.setImageIcon(mNotificationMediaManager.getMediaIcon());
        } else {
            mAppIcon.setImageResource(R.drawable.ic_op_media_player_icon);
        }
        mAppIcon.setColorFilter(colorLabelActive);

        mMediaPlayerBackground.setImageDrawable(getMediaArtwork());
        mMediaPlayerBackground.setClipToOutline(true);
    }

    private Drawable getMediaArtwork() {
        if (mMediaArtwork == null) {
            Drawable artwork = ContextCompat.getDrawable(mContext, R.drawable.qs_op_media_player_bg);
            DrawableCompat.setTint(DrawableCompat.wrap(artwork), colorInactive);
            return artwork;
        } else {
            Drawable artwork = new BitmapDrawable(mContext.getResources(), mMediaArtwork);
            return artwork;
        }
    }

    public void onClick(View v) {
        if (v == mInternetButton) {
            new Handler().post(() -> mInternetDialogFactory.create(true,
                    mAccessPointController.canConfigMobileData(),
                    mAccessPointController.canConfigWifi(),
                    v));
        } else if (v == mBluetoothButton) {
            new Handler().post(() -> mBluetoothDialogFactory.create(true, v));
        } else if (v == mMediaBtnPrev) {
            mNotificationMediaManager.skipTrackPrevious();
        } else if (v == mMediaBtnPlayPause) {
            mNotificationMediaManager.playPauseTrack();
        } else if (v == mMediaBtnNext) {
            mNotificationMediaManager.skipTrackNext();
        } else if (v == mMediaPlayerBackground) {
            launchMediaPlayer();
        } else if (v == mMediaOutputSwitcher) {
            launchMediaOutputSwitcher(v);
        }
    }

    public boolean onLongClick(View v) {
        if (v == mInternetButton) {
            mActivityStarter.postStartActivityDismissingKeyguard(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
        } else if (v == mBluetoothButton) {
            mActivityStarter.postStartActivityDismissingKeyguard(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), 0);
        } else {
            return false;
        }
        return true;
    }

    private void launchMediaPlayer() {
        String packageName = mNotificationMediaManager.getMediaController() != null ? mNotificationMediaManager.getMediaController().getPackageName() : null;
        Intent appIntent = packageName != null ? new Intent(mContext.getPackageManager().getLaunchIntentForPackage(packageName)) : null;
        if (appIntent != null) {
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appIntent.setPackage(packageName);
            mActivityStarter.startActivity(appIntent, true);
            return;
        }

        sendMediaButtonClickEvent();
    }

    private void launchMediaOutputSwitcher(View v) {
        String packageName = mNotificationMediaManager.getMediaController() != null ? mNotificationMediaManager.getMediaController().getPackageName() : null;
        if (packageName != null) {
            mMediaOutputDialogFactory.create(packageName, true, v);
        }
    }

    private void sendMediaButtonClickEvent() {
        long now = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(now, now, 0, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0);
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        helper.sendMediaButtonEvent(keyEvent, true);
        helper.sendMediaButtonEvent(KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP), true);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mStatusBarHeaderMachine.addObserver(this);
        mStatusBarHeaderMachine.updateEnablement();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mStatusBarHeaderMachine.removeObserver(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Only react to touches inside QuickQSPanel
        if (event.getY() > mHeaderQsPanel.getTop()) {
            return super.onTouchEvent(event);
        } else {
            return false;
        }
    }

    void updateResources() {
        colorActive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.colorAccent);
        colorInactive = Utils.getColorAttrDefaultColor(mContext, R.attr.offStateColor);
        colorLabelActive = Utils.getColorAttrDefaultColor(mContext, com.android.internal.R.attr.textColorPrimaryInverse);
        colorLabelInactive = Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary);

        Resources resources = mContext.getResources();
        int orientation = getResources().getConfiguration().orientation;
        boolean largeScreenHeaderActive = LargeScreenUtils.shouldUseLargeScreenShadeHeader(resources);

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (mQsDisabled) {
            lp.height = 0;
        } else {
            lp.height = WRAP_CONTENT;
        }
        setLayoutParams(lp);

        MarginLayoutParams qqsLP = (MarginLayoutParams) mHeaderQsPanel.getLayoutParams();
        qqsLP.topMargin = 0;
        mHeaderQsPanel.setLayoutParams(qqsLP);

        MarginLayoutParams opQqsLP = (MarginLayoutParams) mOpQsLayout.getLayoutParams();
        int qqsMarginTop = resources.getDimensionPixelSize(largeScreenHeaderActive ?
                            R.dimen.qqs_layout_margin_top : R.dimen.large_screen_shade_header_min_height);
        opQqsLP.topMargin = qqsMarginTop;
        mOpQsLayout.setLayoutParams(opQqsLP);

        float qqsExpandY = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                            0 : resources.getDimensionPixelSize(R.dimen.qs_header_height)
                            + resources.getDimensionPixelSize(R.dimen.qs_op_header_layout_expanded_top_margin)
                            - qqsMarginTop;
        TouchAnimator.Builder builderP = new TouchAnimator.Builder()
            .addFloat(mOpQsLayout, "translationY", 0, qqsExpandY);
        mQQSContainerAnimator = builderP.build();

        // Hide header image in landscape mode
    	if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
    	    mQsHeaderImageView.setVisibility(View.GONE);
    	} else {
    	    mQsHeaderImageView.setVisibility(View.VISIBLE);
    	    updateHeaderImage();
    	    applyHeaderBackgroundShadow();
      }
      updateMediaPlayer();
    }

    public void startUpdateInterntTileStateAsync() {
        AsyncTask.execute(new Runnable() {
            public void run() {
                startUpdateInterntTileState();
            }
        });
    }

    public void startUpdateBluetoothTileStateAsync() {
        AsyncTask.execute(new Runnable() {
            public void run() {
                startUpdateBluetoothTileState();
            }
        });
    }

    public void startUpdateInterntTileState() {
        Runnable runnable = mUpdateRunnableInternet;

        if (runnable == null) {
            mUpdateRunnableInternet = new Runnable() {
                public void run() {
                    updateInterntTile();
                    scheduleInternetUpdate();
                }
            };
        } else {
            mHandler.removeCallbacks(runnable);
        }

        scheduleInternetUpdate();
    }

    public void startUpdateBluetoothTileState() {
        Runnable runnable = mUpdateRunnableBluetooth;

        if (runnable == null) {
            mUpdateRunnableBluetooth = new Runnable() {
                public void run() {
                    updateBluetoothTile();
                    scheduleBluetoothUpdate();
                }
            };
        } else {
            mHandler.removeCallbacks(runnable);
        }

        scheduleBluetoothUpdate();
    }

    public void scheduleInternetUpdate() {
        Runnable runnable;
        if ((runnable = mUpdateRunnableInternet) != null) {
            mHandler.postDelayed(runnable, 1000);
        }
    }

    public void scheduleBluetoothUpdate() {
        Runnable runnable;
        if ((runnable = mUpdateRunnableBluetooth) != null) {
            mHandler.postDelayed(runnable, 1000);
        }
    }

    public void setExpanded(boolean expanded, QuickQSPanelController quickQSPanelController) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        quickQSPanelController.setExpanded(expanded);
    }

    public void setExpansion(boolean forceExpanded, float expansionFraction, float panelTranslationY) {
		final float keyguardExpansionFraction = forceExpanded ? 1f : expansionFraction;

		if (mQQSContainerAnimator != null) {
			mQQSContainerAnimator.setPosition(keyguardExpansionFraction);
		}

		if (forceExpanded) {
			setAlpha(expansionFraction);
		} else {
			setAlpha(1);
		}

		mKeyguardExpansionFraction = keyguardExpansionFraction;
	}

    public void disable(int state1, int state2, boolean animate) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        mHeaderQsPanel.setDisabledByPolicy(disabled);
        updateResources();
    }

    private void updateSettings() {
        mHeaderImageEnabled = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0,
                UserHandle.USER_CURRENT) == 1;
        updateHeaderImage();
        updateResources();
    }

    private void setContentMargins(View view, int marginStart, int marginEnd) {
        MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        lp.setMarginStart(marginStart);
        lp.setMarginEnd(marginEnd);
        view.setLayoutParams(lp);
    }

    @Override
    public void updateHeader(final Drawable headerImage, final boolean force) {
        post(new Runnable() {
            public void run() {
                doUpdateStatusBarCustomHeader(headerImage, force);
            }
        });
    }

    @Override
    public void disableHeader() {
        post(new Runnable() {
            public void run() {
                mCurrentBackground = null;
                mQsHeaderImageView.setVisibility(View.GONE);
                mHeaderImageEnabled = false;
                updateResources();
            }
        });
    }

    @Override
    public void refreshHeader() {
        post(new Runnable() {
            public void run() {
                doUpdateStatusBarCustomHeader(mCurrentBackground, true);
            }
        });
    }

    private void doUpdateStatusBarCustomHeader(final Drawable next, final boolean force) {
        if (next != null) {
            mQsHeaderImageView.setVisibility(View.VISIBLE);
            mCurrentBackground = next;
            setNotificationPanelHeaderBackground(next, force);
            mHeaderImageEnabled = true;
            updateResources();
        } else {
            mCurrentBackground = null;
            mQsHeaderImageView.setVisibility(View.GONE);
            mHeaderImageEnabled = false;
            updateResources();
        }
    }

    private void setNotificationPanelHeaderBackground(final Drawable dw, final boolean force) {
        if (mQsHeaderImageView.getDrawable() != null && !force) {
            Drawable[] arrayDrawable = new Drawable[2];
            arrayDrawable[0] = mQsHeaderImageView.getDrawable();
            arrayDrawable[1] = dw;

            TransitionDrawable transitionDrawable = new TransitionDrawable(arrayDrawable);
            transitionDrawable.setCrossFadeEnabled(true);
            mQsHeaderImageView.setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(1000);
        } else {
            mQsHeaderImageView.setImageDrawable(dw);
        }
        applyHeaderBackgroundShadow();
    }

    private void applyHeaderBackgroundShadow() {
        final int headerShadow = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, 0,
                UserHandle.USER_CURRENT);
        if (mCurrentBackground != null && mQsHeaderImageView.getDrawable() != null) {
            mQsHeaderImageView.setImageAlpha(255 - headerShadow);
        }
    }

    private void updateHeaderImage() {
        mHeaderImageEnabled = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0,
                UserHandle.USER_CURRENT) == 1;
        int headerHeight = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_HEIGHT, 142,
                UserHandle.USER_CURRENT);
        int bottomFadeSize = (int) Math.round(headerHeight * 0.555);

        // Set the image header size
        mHeaderImageHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
            headerHeight, getContext().getResources().getDisplayMetrics());
        ViewGroup.MarginLayoutParams qsHeaderParams = 
            (ViewGroup.MarginLayoutParams) mQsHeaderLayout.getLayoutParams();
        qsHeaderParams.height = mHeaderImageHeight;
        mQsHeaderLayout.setLayoutParams(qsHeaderParams);

        // Set the image fade size (it has to be a 55,5% related to the main size)
        mQsHeaderLayout.setFadeSizes(0,0,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
            bottomFadeSize, getContext().getResources().getDisplayMetrics()), 0);
    }
}
