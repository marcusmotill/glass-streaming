package net.majorkernelpanic.example3;


import android.content.Intent;
import android.graphics.Color;
import android.media.SoundPool;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.GestureDetector;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ChangeIP extends Activity {

    public static final String[] wordList = {"0","1", "2", "3", "4", "5", "6", "7", "8", "9"}; //characters available for selection
    private String[] ipCharacters = {"0", "1", "2", ".", "2", "4", "5", ".", "1", "3", "8", ".", "0", "8", "9"}; //long list of currently selected characters
    private int position = 0; //current selection position in the wordList. If the currently selected textView says "3", this is 3.
    private int textViewNumber = 0; //current textView selected (can only be 0, 1, or 2)
    private int characterNumber = 0; //the index of the ipCharacter number currently selected
    private String originalIP;


    private View mView;
    private GestureDetector gestureDetector;
    private VelocityTracker velocityTracker;
    TextView number1;
    TextView number2;
    TextView number3;
    TextView[] views;


    @Override
    protected void onCreate(Bundle bundle) {
        //String temp = "123.8.2.780";  //one way or another, we need to get the ipAddress in here. you need to decide how!
        //parseIPAddress(temp);  //this will set out ipCharacters String[] to the right values (the ones we passed in)

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(bundle);
        Intent intent = getIntent();
        String originalIP = intent.getStringExtra("currentIPAddress");
        parseIPAddress(originalIP);
        setContentView(R.layout.changeip);
        number1 = (TextView) findViewById(R.id.number1);
        number2 = (TextView) findViewById(R.id.number2);
        number3 = (TextView) findViewById(R.id.number3);
        number1.setText(ipCharacters[0]);
        number2.setText(ipCharacters[1]);
        number3.setText(ipCharacters[2]);
        views = new TextView[]{number1, number2, number3};
        views[textViewNumber].setBackgroundColor(Color.DKGRAY);
        gestureDetector = createGestureDetector(this);
        velocityTracker = VelocityTracker.obtain(); //not implemented yet

        ArrayList<Integer> a = new ArrayList<Integer>();
        a.add(1);

        ImageView iV = new ImageView(this);
        iV.setImageResource(a.get(0));
        iV.setImageDrawable(getResources().getDrawable(R.drawable.bg));
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.changeip_card_menu, menu);
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spanString.length(), 0); //fix the color to white
            item.setTitle(spanString);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.save_menu_item:
                saveAddress();
                return true;
            case R.id.quit_menu_item:
                quit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(SoundEffectConstants.CLICK);   //if we tap once, play a click sound and open the options menu
                //kill_activity();
                openOptionsMenu();
                return false;
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
                int dx = (int) (motionEvent2.getX() - motionEvent.getX());   //figure out the distance moved horizontally
                int dy = (int) (motionEvent2.getY() - motionEvent.getY());   //figure out the distance moved vertically

                //Log.d("onScroll:", "dx = " + String.valueOf(dx) + "dy = " + String.valueOf(dy));
                if (Math.abs(dy) > Math.abs(dx)){  //if the biggest change was vertical
                    if (dy < -30 && !views[textViewNumber].getText().toString().equals(".")) {  //make sure that our character is a number
                        // Log.d("dycheck", String.valueOf(textViewNumber));
                        updatePosition(true);
                        views[textViewNumber].setText(wordList[position]);  //set the correct textView to the new number from out wordList
                        ipCharacters[characterNumber] = wordList[position];  //update our data saving array accordingly
                    } else if (dy > 30 && !views[textViewNumber].getText().toString().equals(".")) {
                        // Log.d("dycheck", String.valueOf(textViewNumber));
                        updatePosition(false);
                        views[textViewNumber].setText(wordList[position]);
                        ipCharacters[characterNumber] = wordList[position];
                    }
                } else { //otherwise, if the biggest change was horizontal
                    if (dx < -100) { //move to the right.
                        if(characterNumber < ipCharacters.length - 1) { //if we aren't at the very edge
                            if(characterNumber == 0 || characterNumber == ipCharacters.length-2) { //if we are 1 away from an edge
                                textViewNumber++;
                                characterNumber++;  //update the numbers, then fix the background colors to show an edge
                                updateBackgroundColors(textViewNumber);

                            } else {
                                characterNumber++; //simply move along
                                updateTextViews();
                            }
                        }
                    } else if (dx > 100) { //move to the left
                        if(characterNumber > 0) {
                            if(characterNumber == 1 || characterNumber == ipCharacters.length-1) {
                                textViewNumber--;
                                characterNumber--;
                                updateBackgroundColors(textViewNumber);
                            } else {
                                characterNumber--;
                                updateTextViews();
                            }
                        }
                    }
                }
                return true;
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

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void updatePosition(boolean positive) {
        int limit;
        if(characterNumber % 4 == 0) {
            limit = 2;
        } else if((characterNumber - 1) % 4 == 0) {
            if(ipCharacters[characterNumber - 1].equals("2")) {
                limit = 5;
            } else {
                limit = wordList.length - 1;
            }
        } else {
            if(ipCharacters[characterNumber - 1].equals("5") && ipCharacters[characterNumber - 2].equals("2")) {
                limit = 5;
            } else {
                limit = wordList.length - 1;
            }
        }
        if(positive) {
            position++;
            if(position > limit) { //wordList.length - 1
                position = 0;
            }
        } else {
            position--;
            if(position < 0) {
                position = limit; //wordList.length - 1
            }
        }
    }
    public void updateBackgroundColors(int pos) { //only called if we reach an edge or move away from one
        for(TextView t:views) {
            t.setBackgroundColor(Color.BLACK); //set each textview's color to black
        }
        views[pos].setBackgroundColor(Color.DKGRAY); //then fix the selected textview's color to darkgrey
        if(!views[pos].getText().toString().equals(".")) { //don't try to look for a period in the list that doesn't have one
            position = java.util.Arrays.asList(wordList).indexOf(views[pos].getText().toString());  //correct the current position (choice)
        }
    }
    public void updateTextViews() {
        views[0].setText(ipCharacters[characterNumber - 1]);
        views[1].setText(ipCharacters[characterNumber]);  //update the text of all 3 text views
        views[2].setText(ipCharacters[characterNumber+1]);
        if(!views[1].getText().toString().equals(".")) { //don't try to look for a period in the list that doesn't have one
            position = java.util.Arrays.asList(wordList).indexOf(views[1].getText().toString()); //correct the current position (choice)
        }
    }
    public void saveAddress() {
        StringBuilder newAddressSB = new StringBuilder();
        for(int i = 0; i < ipCharacters.length; i++) {
            if(!(ipCharacters[i].equals("0") && (newAddressSB.length()==0 || String.valueOf(newAddressSB.charAt(newAddressSB.length()-1)).equals(".")))) {
                newAddressSB.append(ipCharacters[i]); //create our string for the ip Address, ignoring 0s
            }
        }
        String newAddress = newAddressSB.toString();
        //MainActivity.setIP]
        //SET THE XML FILE TO HAVE THE newAddress VARIABLE AND WE ARE DONE!
        Intent intent = new Intent(getApplication(),MainActivity.class);
        intent.putExtra("newIPAddress",newAddress);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        kill_activity();
    }
    public void parseIPAddress(String originalIP) {
        String[] ipParts = originalIP.split("\\."); //break it by periods
        for(int i = 0; i < ipParts.length; i++) {
            if(ipParts.length != 4 || ipParts[i].length() > 3) {
                return;                                      //lets make sure we have a valid ipAddress here!
            }
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < ipParts.length; i++) {
            sb.append("000".substring(0,(3 - ipParts[i].length())) + ipParts[i] + ((i == ipParts.length-1)?"":".")); //fix it to have 0s
        }
        /*
        for(int i = 0; i < ipParts.length; i++) {
            if(ipParts[i].length() == 0) {
                sb.append("000");
            } else if(ipParts[i].length() == 1) {
                sb.append("00" + ipParts[i]);     //Same thing as that single line in the forloop above. Just for demonstrational purposes
            } else if(ipParts[i].length() == 2)
                sb.append("0" + ipParts[i]);
            } else {
                sb.append(ipParts[i]);
            }
            sb.append(i == ipParts.length - 1?"":"."); //only add a period if it isn't the last set of 3
        }
        */
        String ipAddressParsed = sb.toString();
        for(int i = 0; i < ipAddressParsed.length(); i++) {
            ipCharacters[i] = ipAddressParsed.substring(i,i+1); //update our array
        }
    }
    private void quit() {
        Intent intent = new Intent(getApplication(),MainActivity.class);
        intent.putExtra("newIPAddress",originalIP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        kill_activity();
    }
    void kill_activity() {
        finish();
    }
}