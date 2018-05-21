package de.danoeh.antennapod.core.service.playback;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.playback.Playable;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Locale;

public class PlaybackSpeed {
    private static final String TAG = "PlaybackSpeed";

    public static final int SEEK_BAR_STEP = 2;
    private static final float SEEK_BAR_CONVERSION = 20.0f;
    private static final int SEEK_BAR_MIN_VALUE_OFFSET = 10;

    /** The default settings (speed factor 1.0) */
    public static final PlaybackSpeed DEFAULT = new PlaybackSpeed(1.0f, PlaybackSpeedSource.DEFAULT);
    /** This instance fetches the current speed factor from the {@link UserPreferences} */
    public static final PlaybackSpeed USER_PREFERENCES = new UserPreferencesPlaybackSpeed();

    private final float speed;
    private final PlaybackSpeedSource source;

    public PlaybackSpeed(float speed, @Nullable PlaybackSpeedSource source) {
        this.speed = speed;
        this.source = source;
    }

    public static PlaybackSpeed fromSeekBarProgress(int progress, @Nullable PlaybackSpeedSource source) {
        return new PlaybackSpeed((progress + SEEK_BAR_MIN_VALUE_OFFSET) / SEEK_BAR_CONVERSION, source);
    }

    public float getSpeed() {
        return speed;
    }

    public int getSeekBarProgress() {
        return (int) (SEEK_BAR_CONVERSION * getSpeed()) - SEEK_BAR_MIN_VALUE_OFFSET;
    }

    /** Get the speed factor to store in the preferences. This is {@code null} if the feed did not override the speed */
    public Float getSpeedForPrefs() {
        return source == PlaybackSpeedSource.FEED ? speed : null;
    }

    @Nullable
    public PlaybackSpeedSource getSource() {
        return source;
    }

    @NonNull
    public static PlaybackSpeed getPlaybackSpeed(@Nullable final Playable media) {
        PlaybackSpeed currentSpeed = getPlayBackSpeedFromFeedPreferences(media);
        if (currentSpeed == null) {
            // no speed-override configured for this feed, use the global user preference instead
            currentSpeed = USER_PREFERENCES;
        }
        return currentSpeed;
    }

    @Nullable
    private static PlaybackSpeed getPlayBackSpeedFromFeedPreferences(@Nullable final Playable media) {
        if (media instanceof FeedMedia) {
            final FeedItem item = ((FeedMedia) media).getItem();
            if (item != null) {
                return item.getFeed().getPreferences().getPlaybackSpeed();
            }
        }
        return null;
    }

    /** Get the speed in the localized format with a multiplier at the end (e.g. "1.25x") */
    @NonNull
    public String formatWithMultiplicator() {
        return String.format("%.2fx", getSpeed());
    }

    /** Get the speed in a non-localized format to store it in the preferences */
    @NonNull
    public String formatForPreferences() {
        return String.format(Locale.US, "%.2f", getSpeed());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("speed", getSpeed())
                .append("source", getSource())
                .toString();
    }

    public enum PlaybackSpeedSource {
        DEFAULT, PREFERENCES, FEED
    }

    private static final class UserPreferencesPlaybackSpeed extends PlaybackSpeed {
        UserPreferencesPlaybackSpeed() {
            super(getUserPreferencesSpeed(), PlaybackSpeedSource.PREFERENCES);
        }

        @Override
        public float getSpeed() {
            return getUserPreferencesSpeed();
        }

        private static Float getUserPreferencesSpeed() {
            try {
                return Float.valueOf(UserPreferences.getPlaybackSpeed());
            } catch (NumberFormatException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                final float defaultSpeed = DEFAULT.getSpeed();
                UserPreferences.setPlaybackSpeed(String.valueOf(defaultSpeed));
                return defaultSpeed;
            }
        }
    }
}
