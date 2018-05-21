package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.playback.PlaybackService;
import de.danoeh.antennapod.core.service.playback.PlaybackSpeed;
import de.danoeh.antennapod.core.util.playback.ExternalMedia;
import de.danoeh.antennapod.core.util.playback.PlaybackServiceStarter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;

/**
 * Activity for playing audio files.
 */
public class AudioplayerActivity extends MediaplayerInfoActivity {
    private static final String TAG = "AudioPlayerActivity";

    private final AtomicBoolean isSetup = new AtomicBoolean(false);

    @Override
    protected void onResume() {
        super.onResume();
        if (TextUtils.equals(getIntent().getAction(), Intent.ACTION_VIEW)) {
            Intent intent = getIntent();
            if (intent.getData() == null) {
                return;
            }
            Log.d(TAG, "Received VIEW intent: " + intent.getData().getPath());
            ExternalMedia media = new ExternalMedia(intent.getData().getPath(),
                    MediaType.AUDIO);

            new PlaybackServiceStarter(this, media)
                    .startWhenPrepared(true)
                    .shouldStream(false)
                    .prepareImmediately(true)
                    .start();

        } else if (PlaybackService.isCasting()) {
            Intent intent = PlaybackService.getPlayerActivityIntent(this);
            if (intent.getComponent() != null &&
                    !intent.getComponent().getClassName().equals(AudioplayerActivity.class.getName())) {
                saveCurrentFragment();
                finish();
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onReloadNotification(int notificationCode) {
        if (notificationCode == PlaybackService.EXTRA_CODE_CAST) {
            Log.d(TAG, "ReloadNotification received, switching to Castplayer now");
            saveCurrentFragment();
            finish();
            startActivity(new Intent(this, CastplayerActivity.class));

        } else {
            super.onReloadNotification(notificationCode);
        }
    }

    @Override
    protected void updatePlaybackSpeedButton() {
        if(butPlaybackSpeed == null) {
            return;
        }
        if (controller == null) {
            butPlaybackSpeed.setVisibility(View.GONE);
            return;
        }
        updatePlaybackSpeedButtonText();
        PlaybackSpeed.PlaybackSpeedSource playbackSpeedSource = getPlaybackSpeed().getSource();
        boolean canChangeSpeed = controller.canSetPlaybackSpeed() && playbackSpeedSource != PlaybackSpeed.PlaybackSpeedSource.FEED;
        ViewCompat.setAlpha(butPlaybackSpeed, canChangeSpeed ? 1.0f : 0.5f);
        butPlaybackSpeed.setVisibility(View.VISIBLE);
    }

    @Override
    protected void updatePlaybackSpeedButtonText() {
        if(butPlaybackSpeed == null) {
            return;
        }
        if (controller == null) {
            butPlaybackSpeed.setVisibility(View.GONE);
            return;
        }
        PlaybackSpeed speed = PlaybackSpeed.DEFAULT;
        if(controller.canSetPlaybackSpeed()) {
            speed = getPlaybackSpeed();
        }
        butPlaybackSpeed.setText(speed.formatWithMultiplicator());
    }

    @Override
    protected void setupGUI() {
        if(isSetup.getAndSet(true)) {
            return;
        }
        super.setupGUI();
        if(butCastDisconnect != null) {
            butCastDisconnect.setVisibility(View.GONE);
        }
        if(butPlaybackSpeed != null) {
            butPlaybackSpeed.setOnClickListener(v -> {
                if (controller == null) {
                    return;
                }
                final PlaybackSpeed playbackSpeed = getPlaybackSpeed();
                if (playbackSpeed.getSource() == PlaybackSpeed.PlaybackSpeedSource.FEED) {
                    final Long feedId = getFeedId();
                    if (feedId != null) {
                        VariableSpeedDialog.showSpeedConfiguredInFeedPluginDialog(this, feedId);
                    } else {
                        Log.w(TAG, "Could not get id of current feed");
                    }
                } else if (controller.canSetPlaybackSpeed()) {
                    String[] availableSpeeds = UserPreferences.getPlaybackSpeedArray();
                    String currentSpeed = playbackSpeed.formatForPreferences();

                    // Provide initial value in case the speed list has changed
                    // out from under us
                    // and our current speed isn't in the new list
                    String newSpeed;
                    if (availableSpeeds.length > 0) {
                        newSpeed = availableSpeeds[0];
                    } else {
                        newSpeed = PlaybackSpeed.DEFAULT.formatForPreferences();
                    }

                    for (int i = 0; i < availableSpeeds.length; i++) {
                        if (availableSpeeds[i].equals(currentSpeed)) {
                            if (i == availableSpeeds.length - 1) {
                                newSpeed = availableSpeeds[0];
                            } else {
                                newSpeed = availableSpeeds[i + 1];
                            }
                            break;
                        }
                    }
                    UserPreferences.setPlaybackSpeed(newSpeed);
                    controller.setPlaybackSpeed(Float.parseFloat(newSpeed));
                } else {
                    VariableSpeedDialog.showGetPluginDialog(this);
                }
            });
            butPlaybackSpeed.setOnLongClickListener(v -> {
                VariableSpeedDialog.showDialog(this);
                return true;
            });
            butPlaybackSpeed.setVisibility(View.VISIBLE);
        }
    }

    private Long getFeedId() {
        Playable media = controller.getMedia();
        if (media instanceof FeedMedia) {
            final FeedItem item = ((FeedMedia) media).getItem();
            if (item != null) {
                return item.getFeed().getId();
            }
        }
        return null;
    }

    private PlaybackSpeed getPlaybackSpeed() {
        return PlaybackSpeed.getPlaybackSpeed(controller.getMedia());
    }
}
