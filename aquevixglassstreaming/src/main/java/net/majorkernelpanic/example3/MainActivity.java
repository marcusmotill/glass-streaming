package net.majorkernelpanic.example3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspClient;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.view.KeyEvent;

public class MainActivity extends FragmentActivity implements
OnClickListener, 
RtspClient.Callback, 
Session.Callback, 
SurfaceHolder.Callback, 
OnCheckedChangeListener {

	public final static String TAG = "MainActivity";

	private Button mButtonSave;
	private ImageButton mButtonStart;
	private ImageButton mButtonFlash;
	private ImageButton mButtonCamera;
	private ImageButton mButtonSettings;
	private RadioGroup mRadioGroup;
	private FrameLayout mLayoutVideoSettings;
	private FrameLayout mLayoutServerSettings;
	private SurfaceView mSurfaceView;
	private TextView mTextBitrate;
	private EditText mEditTextPassword;
	private EditText mEditTextUsername;
	private ProgressBar mProgressBar;
	private Session mSession;
	private RtspClient mClient;
    private TextView ipAddressTextView;
    private GestureDetector gestureDetector;
    private Chronometer chronometer;
    private TextView liveTextView;
    private TextView batteryLevel;
    private FrameLayout.LayoutParams params;
    private FrameLayout menuContainer;
    private static final String FRAGMENT_TAG = "fragmentTag";
    private static final int STATUS_CODE_STANDBY = 1;
    private static final int STATUS_CODE_CONNECTING = 2;
    private static final int STATUS_CODE_LIVE = 3;
    private String batteryTimeRemaining = "0:00";


    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryLevel.setText(String.valueOf(level) + "%");
            long l = System.currentTimeMillis();
            if (previousBatteryLevel != level) {
                if (batteryChangeTime == 0) {
                    batteryChangeTime = l;
                    previousBatteryLevel = level;
                    int timeRemaining = (int) (level * .8 * 60);
                    int minutes = (int) ((timeRemaining / 60) % 60);
                    int hours = (int) (timeRemaining / 60 / 60);
                    batteryTimeRemaining = (hours + ":" + (String.valueOf(minutes).length() == 2 ? minutes : "0" + minutes));
                } else {
                    long changeInMillis = Math.abs(l - batteryChangeTime);
                    batteryChangeTime = l;
                    double seconds = ((((int) changeInMillis) + 0.0) / 1000);
                    double timeRemaining = seconds * level;
                    int minutes = (int) ((timeRemaining / 60) % 60);
                    int hours = (int) (timeRemaining / 60 / 60);
                    batteryTimeRemaining = (hours + ":" + (String.valueOf(minutes).length() == 2 ? minutes : "0" + minutes));
                    previousBatteryLevel = level;
                }
            }
            menuFragment.updateBatteryLife(batteryTimeRemaining);
            if (!isConnectedToInternet()) {
                menuFragment.updateStreamQuality(-9999);
            } else {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int linkSpeed = wifiManager.getConnectionInfo().getRssi();
                int numberOfBars = calculateSignalLevel(linkSpeed, 4);
                menuFragment.updateStreamQuality(linkSpeed);
            }
        }
    };

    private long batteryChangeTime = 0;
    private int previousBatteryLevel = -1;
    private int statusCode = STATUS_CODE_STANDBY; //refers to the status codes STATUS_CODE_STANDBY, STATUS_CODE_LIVE, etc.

    private MenuFragment menuFragment;

	private EditText edtIp;
	private EditText edtPort;
	private EditText edtPath;

    protected String ipAddress;
    protected int port = 0;
    protected String streamPath = "";
    private int menuType = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
        mLayoutServerSettings = (FrameLayout) findViewById(R.id.server_layout);
        mLayoutServerSettings.setVisibility(View.INVISIBLE);
        menuContainer = (FrameLayout) findViewById(R.id.menu_container);
        params = (FrameLayout.LayoutParams)menuContainer.getLayoutParams();

        SharedPreferences sPref = getPreferences(MODE_PRIVATE);
        String savedIPAddress = sPref.getString("ipAddress","-1");
        if(!(savedIPAddress.equals("-1"))) {
            ipAddress = savedIPAddress;
        } else {
            ipAddress = "192.168.1.2";
        }


        Intent intent = getIntent();
        if(intent != null && intent.getStringExtra("newIPAddress") != null) {
            ipAddress = intent.getStringExtra("newIPAddress");
            if (menuFragment != null) {
                menuFragment.updateStreamInfo(ipAddress + ":" + getResources().getString(R.string.port) + getResources().getString(R.string.streamPath));
            }
            SharedPreferences.Editor editor = sPref.edit();
            editor.putString("ipAddress", ipAddress);
            editor.commit();
        }

        menuFragment = new MenuFragment();

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        gestureDetector = createGestureDetector(this);


        ipAddressTextView = (TextView) findViewById(R.id.txt_ip);
        ipAddressTextView.setText(ipAddress);
		mButtonStart = (ImageButton) findViewById(R.id.start);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);


		mLayoutVideoSettings = (FrameLayout) findViewById(R.id.video_layout);

		mRadioGroup =  (RadioGroup) findViewById(R.id.radio);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

		mRadioGroup.setOnCheckedChangeListener(this);
		mRadioGroup.setOnClickListener(this);

        chronometer = (Chronometer) findViewById(R.id.chronometer);
        liveTextView = (TextView) findViewById(R.id.liveTextView);
        updateLiveTextView(STATUS_CODE_STANDBY);


        batteryLevel = (TextView) findViewById(R.id.batteryLevelTextView);


		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		// Configures the SessionBuilder
		mSession = SessionBuilder.getInstance()
				.setContext(getApplicationContext())
				.setAudioEncoder(SessionBuilder.AUDIO_AAC)
				.setAudioQuality(new AudioQuality(8000,16000))
				.setVideoEncoder(SessionBuilder.VIDEO_H264)
				.setSurfaceView(mSurfaceView)
				.setPreviewOrientation(0)
				.setCallback(this)
				.build();

		// Configures the RTSP client
		mClient = new RtspClient();
		mClient.setSession(mSession);
		mClient.setCallback(this);

		// Use this to force streaming with the MediaRecorder API
		//mSession.getVideoTrack().setStreamingMethod(MediaStream.MODE_MEDIARECORDER_API);

		// Use this to stream over TCP, EXPERIMENTAL!
		//mClient.setTransportMode(RtspClient.TRANSPORT_TCP);

		// Use this if you want the aspect ratio of the surface view to 
		// respect the aspect ratio of the camera preview
		//mSurfaceView.setAspectRatioMode(SurfaceView.ASPECT_RATIO_PREVIEW);

		mSurfaceView.getHolder().addCallback(this);


		selectQuality();


        port = Integer.parseInt(getResources().getString(R.string.port));
        streamPath = getResources().getString(R.string.streamPath);

        mLayoutVideoSettings.setVisibility(View.INVISIBLE);

	}
    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetectorTemp = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            //we are creating our gesture detector here
            @Override
            public boolean onDown(MotionEvent motionEvent) {

                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                int SOUND_DISALLOWED = 10;
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(SOUND_DISALLOWED);

                if(ipAddress.equals("...")) {
                    updateLiveTextView(STATUS_CODE_CONNECTING);
                    updateLiveTextView(STATUS_CODE_LIVE);        //for debug purposes
                }

                menuType = 0;
                invalidateOptionsMenu();

                return true;
            }
            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float distanceX, float distanceY) {
                return false;
            }
            @Override
            public void onLongPress(MotionEvent motionEvent) {
            }
            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) { //fling = a single slide
                int dx = (int) (motionEvent2.getX() - motionEvent.getX());
                int dy = (int) (motionEvent2.getY() - motionEvent.getY());
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) {
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.screenBrightness = 0f;
                        //int previousScreenBrightness = android.provider.Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                        getWindow().setAttributes(lp);
                    } else {
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        lp.screenBrightness = 1f;
                        getWindow().setAttributes(lp);
                    }
                    return true;
                } else {
                    if(dy > 0) {
                        menuType = 1;
                        invalidateOptionsMenu();
                        openOptionsMenu();
                        return true;
                    }
                }
                return false;
            }
        });
        return gestureDetectorTemp;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (gestureDetector != null) {
            return gestureDetector.onTouchEvent(event);
        }
        return false;
    }


    public boolean onCreateOptionsMenu(Menu menu)
    {
      //  System.out.println("menuType = " + menuType);
        switch(menuType) {
            case 0:
                getMenuInflater().inflate(R.menu.main_menu, menu);
                break;
            case 1:
                getMenuInflater().inflate(R.menu.close_menu, menu);
                break;
            default:
                getMenuInflater().inflate(R.menu.main_menu, menu);
               // System.out.println("Default menu called");
                break;
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection. Menu items typically start another
        // activity, start a service, or broadcast another intent.
        switch (item.getItemId())
        {
            case R.id.toggle_stream:
                toggleStream();
                return true;
            case R.id.change_ip:
                changeIP();
                return true;
            case R.id.quit_close_menu_item:
                finish();
                return true;
            //case R.id.return_close_menu_item:
            //    //do nothing!
            //    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_CAMERA && !event.isLongPress()) {
            if(statusCode != STATUS_CODE_CONNECTING) {  //this if was added 9/17/2014
                toggleStream();
            }
            return true;
        }
        super.onKeyDown(keycode, event);
        return false;
    }

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		mLayoutVideoSettings.setVisibility(View.GONE);
		mLayoutServerSettings.setVisibility(View.VISIBLE);
		selectQuality();
	}	

    public void onClick(View v) {
        //Log.v("onTest","Tap registered");
        //openOptionsMenu();
        //DO NOTHING FOR NOW
        //Log.v("onTest","THISWORKED-------------------------");
        //AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //am.playSoundEffect(SoundEffectConstants.CLICK);

    }

    public void onStartButton(View v) {
        toggleStream();
    }

	@Override
	public void onDestroy(){
		super.onDestroy();
        unregisterReceiver(mBatInfoReceiver);
		mClient.release();
		mSession.release();
		mSurfaceView.getHolder().removeCallback(this);

	}

	private void selectQuality() {
		int id = mRadioGroup.getCheckedRadioButtonId();
		RadioButton button = (RadioButton) findViewById(id);
		if (button == null) return;

		String text = button.getText().toString();
		Pattern pattern = Pattern.compile("(\\d+)x(\\d+)\\D+(\\d+)\\D+(\\d+)");
		Matcher matcher = pattern.matcher(text);

		matcher.find();
		int width = 960;
		int height = 720;
		int framerate = 30;
		//int bitrate = Integer.parseInt(matcher.group(4))*1000;//1000
        int bitrate = 800000; //8000000
        String streamInfo = String.valueOf(width) + "x" + String.valueOf(height) + " " + String.valueOf(framerate) + "fps " + String.valueOf(bitrate) + "kB/s";

		mSession.setVideoQuality(new VideoQuality(width, height, framerate, bitrate)); //hardcoded resolution and framerate
		//Toast.makeText(this, streamInfo , Toast.LENGTH_SHORT).show();

		Log.d(TAG, "Selected resolution: "+width+"x"+height);
	}

	private void enableUI() {
		mButtonStart.setEnabled(true);
		//mButtonCamera.setEnabled(true);
	}

	// Connects/disconnects to the RTSP server and starts/stops the stream
	public void toggleStream() {
		mProgressBar.setVisibility(View.VISIBLE);
		if (!mClient.isStreaming()) {
            mLayoutServerSettings.setVisibility(View.GONE);
			mClient.setCredentials(/*mEditTextUsername.getText().toString()*/"admin", /*mEditTextPassword.getText().toString()*/"admin");
			//mClient.setServerAddress(/*ip*//*"192.168.47.220"*/edtIp.getText().toString(), Integer.parseInt(/*port*//*"1935"*/edtPort.getText().toString()));
			mClient.setServerAddress(ipAddress, port);
            //mClient.setStreamPath("/"+/*path*//*"live/AQTest1.mp4"*/edtPath.getText().toString());
            mClient.setStreamPath(streamPath);
			mClient.startStream();
            updateLiveTextView(STATUS_CODE_CONNECTING);

		} else {
			// Stops the stream and disconnects from the RTSP server
           // mProgressBar.setVisibility(View.INVISIBLE); //added 15/9/14
//            int test123 = mProgressBar.getVisibility();
//            Log.v("Visibility code", " = " + test123);

            mClient.stopStream();
            chronometer.stop();
            updateLiveTextView(STATUS_CODE_STANDBY);
		}
	}

	private void logError(final String msg) {
		final String error = (msg == null) ? "Error unknown" : msg; 
		// Displays a popup to report the error to the user
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        chronometer.stop();
        if(statusCode != STATUS_CODE_STANDBY) {
            updateLiveTextView(STATUS_CODE_STANDBY);
        }
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void onBitrateUpdate(long bitrate) {
//		mTextBitrate.setText(""+bitrate/1000+" kbps");
	}

	@Override
	public void onPreviewStarted() {
	}

	@Override
	public void onSessionConfigured() {

	}

	@Override
	public void onSessionStarted() {
		enableUI();
		mButtonStart.setImageResource(R.drawable.ic_switch_video_active);
		mProgressBar.setVisibility(View.GONE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        updateLiveTextView(STATUS_CODE_LIVE);
	}
	@Override
	public void onSessionStopped() {
		enableUI();
		mButtonStart.setImageResource(R.drawable.ic_switch_video);
		mProgressBar.setVisibility(View.GONE);
        chronometer.stop();
        //right here
        updateLiveTextView(STATUS_CODE_STANDBY);
	}
	@Override
	public void onSessionError(int reason, int streamType, Exception e) {
		mProgressBar.setVisibility(View.GONE);
		switch (reason) {
		case Session.ERROR_CAMERA_ALREADY_IN_USE:
			break;
		case Session.ERROR_CAMERA_HAS_NO_FLASH:
			mButtonFlash.setImageResource(R.drawable.ic_flash_on_holo_light);
			mButtonFlash.setTag("off");
			break;
		case Session.ERROR_INVALID_SURFACE:
			break;
		case Session.ERROR_STORAGE_NOT_READY:
			break;
		case Session.ERROR_CONFIGURATION_NOT_SUPPORTED:
			VideoQuality quality = mSession.getVideoTrack().getVideoQuality();
			logError("The following settings are not supported on this phone: "+
					quality.toString()+" "+
					"("+e.getMessage()+")");
			e.printStackTrace();
			return;
		case Session.ERROR_OTHER:
			break;
		}
		if (e != null) {
			logError(e.getMessage());
			e.printStackTrace();
		}
	}
	@Override
	public void onRtspUpdate(int message, Exception e) {
		switch (message) {
		case RtspClient.ERROR_CONNECTION_FAILED:
		case RtspClient.ERROR_WRONG_CREDENTIALS:
			mProgressBar.setVisibility(View.GONE);
			enableUI();
			logError(e.getMessage());
			e.printStackTrace();
			break;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSession.startPreview();
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mClient.stopStream();
	}
    public void changeIP() {
        Intent intent = new Intent(getApplication(),ChangeIP.class);
        intent.putExtra("currentIPAddress",ipAddress);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    public void slideMenu() {
        slideMenu(300);
    }

    public void slideMenu(int maxScrollDistance) {
        //int maxScrollDistance = 300;  //in Marcus' code it was 900 originally
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float logicalDensity = metrics.density;
        int dp = (int) Math.ceil(maxScrollDistance / logicalDensity); //change 900 to slide a different distance
        //System.out.println("-----" + dp);
        params.height = dp;
        Fragment f = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if(f != null) {
            getSupportFragmentManager().popBackStack();
        } else {

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_down,
                            R.anim.slide_up,
                            R.anim.slide_down,
                            R.anim.slide_up)              //this is working
                    .replace(R.id.menu_container, menuFragment,
                            FRAGMENT_TAG
                    ).addToBackStack(null).commit();
            menuFragment.changeTabText(statusCode);
            menuFragment.updateStreamInfo(ipAddress + ":" + getResources().getString(R.string.port) + getResources().getString(R.string.streamPath));
           // Log.v("Status Code", "" + statusCode);


            //f = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            //f.liveTextViewSlidingMenu.setText(" ");
          //  MenuFragment menuFragment = (MenuFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
           // if(menuFragment == null)
            //    Log.v("TAG", "menuFragment is null");
          //  menuFragment.liveTextViewSlidingMenu.setText("Works");

            //List<Fragment> a = getSupportFragmentManager().getFragments();
            //Log.v("TAG",String.valueOf(a.size()));
        //    MenuFragment menuFragment = ((MenuFragment) getSupportFragmentManager().findFragmentById(R.id.menu_container));
        //    menuFragment.changeText();

        }
    }
/*
    private void slideMenu(int distance) {
        Log.v("TAG", "slideMenu Called");
        FrameLayout container = (FrameLayout) findViewById(R.id.menu_container);
        if(container == null) Log.v("TAG","container is null");
        container.setY(-20); //how far off the screen it starts (20 pixels above the screen) define the starting postion on launch in the oncreate method by container.setY(-20);
        container.animate().setDuration(1000).yBy(distance); //animating to 10 pixels lower than where it started
        MenuFragment.liveTextViewSlidingMenu.setText("Standby");//Replace Contact_Us with the name of your fragment and make the textview in the fragment public static
    }
    */
    public void updateLiveTextView(int statusCodeNew) {
            switch (statusCodeNew) {
                case STATUS_CODE_STANDBY:
                    slideMenu();
                    // Log.i("Close", "standy");
                    liveTextView.setText("Standby");
                    statusCode = 1;
                    menuFragment.changeTabText(MenuFragment.FRAGMENT_STANDBY);
                    liveTextView.setTextColor(Color.WHITE);
                    liveTextView.clearAnimation();
                    break;
                case STATUS_CODE_CONNECTING:
                    slideMenu();
                    liveTextView.setText("Connecting...");
                    statusCode = 2;
                    menuFragment.changeTabText(MenuFragment.FRAGMENT_CONNECTING);
                    liveTextView.setTextColor(Color.WHITE);
                    liveTextView.clearAnimation();
                    break;
                case STATUS_CODE_LIVE:
                    liveTextView.setText("Live");
                    statusCode = 3;
                    menuFragment.changeTabText(MenuFragment.FRAGMENT_LIVE);
                    liveTextView.setTextColor(Color.RED);
                    Animation anim = new AlphaAnimation(0.0f, 1.0f);
                    anim.setDuration(50); //You can manage the time of the blink with this parameter
                    anim.setStartOffset(500);
                    anim.setRepeatMode(Animation.REVERSE);
                    anim.setRepeatCount(Animation.INFINITE);
                    liveTextView.startAnimation(anim);
                    break;
                default:
                    break;

        }
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
    public boolean isConnectedToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }

        }
        return false;
    }
}

//current features
/*
swipe forward to dim
swipe back to brighten
save ip address between loads
edit ip address
slide down to access menu
slide down and tap to exit
slide down and to the right and tap to go edit ip address
in edit ip address, change values with sliding up and down, moving with sliding left or right. tap to choose either save or close
Sliding tab is updated dynamically
 */