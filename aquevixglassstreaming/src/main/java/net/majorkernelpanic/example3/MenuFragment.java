package net.majorkernelpanic.example3;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Alex on 9/4/14.
 */
public class MenuFragment extends Fragment {
    private boolean firstLaunch = true;
    private TextView liveTextViewSlidingMenu;
    private TextView batteryLifeTextView;
    private TextView streamInfoTextView;
    //private TextView streamQualityTextView;
    private ImageView streamQualityImageView;
    public static final int FRAGMENT_INITIAL_LAUNCH = 1;
    public static final int FRAGMENT_STANDBY = 1;
    public static final int FRAGMENT_CONNECTING = 2;
    public static final int FRAGMENT_LIVE = 3;
    private String liveTextViewString = "Standby";
    private String batteryLifeString = "*:** remaining";
    private String streamQualityString = "*";
    private String streamInfoString = "***.***.***.***:****/unknown/myStream";
    private int streamQualityImageIndex = 0;
    private Drawable[] wifiImage = new Drawable[5];

    @Override             //very simple system. If onCreate has not been called yet, we save new info in strings.
                          //otherwise, we set it directly
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.menu_fragment, container, false);
        firstLaunch = false;

        Resources resources = getResources();
        Drawable d = resources.getDrawable(R.drawable.signalnone);
        d.setColorFilter(Color.LTGRAY,PorterDuff.Mode.SRC_ATOP);
        wifiImage[0] = d;
        d = resources.getDrawable(R.drawable.signal0);
        d.setColorFilter(Color.LTGRAY,PorterDuff.Mode.SRC_ATOP);
        wifiImage[1] = d;
        d = resources.getDrawable(R.drawable.signal1);
        d.setColorFilter(Color.LTGRAY,PorterDuff.Mode.SRC_ATOP);
        wifiImage[2] = d;
        d = resources.getDrawable(R.drawable.signal2);
        d.setColorFilter(Color.LTGRAY,PorterDuff.Mode.SRC_ATOP);
        wifiImage[3] = d;
        d = resources.getDrawable(R.drawable.signal3);
        d.setColorFilter(Color.LTGRAY,PorterDuff.Mode.SRC_ATOP);
        wifiImage[4] = d;

        Typeface roboto = Typeface.createFromAsset(getActivity().getAssets(), "Roboto/Roboto-Medium.ttf");
        liveTextViewSlidingMenu = (TextView) rootView.findViewById(R.id.liveTextViewSlidingMenu);
        batteryLifeTextView = (TextView) rootView.findViewById(R.id.batteryLevelTextView);
        streamInfoTextView = (TextView) rootView.findViewById(R.id.streamInfoTextView);
        //streamQualityTextView = (TextView) rootView.findViewById(R.id.streamQualityTextView);
        streamQualityImageView = (ImageView) rootView.findViewById(R.id.streamQualityImageView);
        liveTextViewSlidingMenu.setTypeface(roboto);
        batteryLifeTextView.setTypeface(roboto);
        streamInfoTextView.setTypeface(roboto);
       // streamQualityTextView.setTypeface(roboto);

        liveTextViewSlidingMenu.setText(liveTextViewString);
        batteryLifeTextView.setText(batteryLifeString);
        streamInfoTextView.setText(streamInfoString);
        //streamQualityTextView.setText(streamQualityString);
        streamQualityImageView.setImageDrawable(wifiImage[streamQualityImageIndex]);

        return rootView;
    }

    public void changeTabText(int id) {
        switch(id) {
            case FRAGMENT_STANDBY:
                liveTextViewString = "Standby";
                break;
            case FRAGMENT_CONNECTING:
                liveTextViewString = "Connecting...";
                break;
            case FRAGMENT_LIVE:
                liveTextViewString = "Live";
                break;
            default:
                break;
        }
        updateText();
    }
    private void updateText() {
        if(!firstLaunch) {
            liveTextViewSlidingMenu.setText(liveTextViewString);
            batteryLifeTextView.setText(batteryLifeString);
            streamInfoTextView.setText(streamInfoString);
            //streamQualityTextView.setText(streamQualityString);
            streamQualityImageView.setImageDrawable(wifiImage[streamQualityImageIndex]);
        }
    }

    public void updateBatteryLife(String s) {
        batteryLifeString = "~" + s + " remaining";
        updateText();
    }
    public void updateStreamInfo(String s) {
        streamInfoString = s;
        updateText();
    }
    public void updateStreamQuality(int rssi) {
        if(rssi == -9999) {       //9999 is the value for the wifi rssi if not connected
            streamQualityImageIndex = 0;
        } else {
            streamQualityImageIndex = calculateSignalLevel(rssi, 4) + 1;
        }
        updateText();
    }
    public int calculateSignalLevel(int rssi, int numLevels) {
        int MIN_RSSI = -100;
        int MAX_RSSI = -55;
        if(rssi <= MIN_RSSI) {
            return 0;
        } else if(rssi >= MAX_RSSI) {
            return numLevels - 1;
        } else {
            float inputRange = (MAX_RSSI - MIN_RSSI);
            float outputRange = (numLevels - 1);
            if(inputRange != 0)
                return (int) ((float) (rssi - MIN_RSSI) * outputRange / inputRange);
        }
        return 0;
    }

}
