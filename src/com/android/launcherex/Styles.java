/* 
 * This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://sam.zoy.org/wtfpl/COPYING for more details.
 */

package com.android.launcherex;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.Gallery;

public class Styles extends Activity implements OnDoubleTapListener, OnGestureListener {
	public static SharedPreferences sharedPref;
	private Gallery mGallery;
	private GestureDetector gesturedetector = null;
	private ProgressDialog mProgressDialog;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.styles_main);
                
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Please wait...");
        
        sharedPref = getSharedPreferences(Launcher.STYLE_PREF, 0);
        
        gesturedetector = new GestureDetector(this, this);
        gesturedetector.setOnDoubleTapListener(this);
        
        mGallery = (Gallery) findViewById(R.id.gallery);
        mGallery.setAdapter(new StylesAdapter(this));
        mGallery.setSelection(sharedPref.getInt(Launcher.PREFERENCE_STYLE, 0));
        mGallery.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				savePreference();
				return true;
			}
        });
        
        Button button = (Button)findViewById(R.id.apply);
        button.setOnClickListener(new OnClickListener() { 
        	public void onClick(View v) {
        		savePreference();
        	} 
        });
    }
    
    private void savePreference() {
        sharedPref.edit().putInt(Launcher.PREFERENCE_STYLE , mGallery.getSelectedItemPosition()).commit();
        mProgressDialog.show();
        new Handler().postDelayed(
        		new Runnable() {
        			@Override public void run() {
        				mProgressDialog.dismiss();
        				Styles.this.finish();
        				}
        			}, 550);
        /*
         * Generally, 100 to 200ms is the threshold beyond which users will perceive lag in an application.
         */
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
            return gesturedetector.onTouchEvent(event);
    }

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		savePreference();
		return false;
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.hold, R.anim.drawer_fade_out);
	}
	
	/* Land of the forgotten classes */
	
	public boolean onDoubleTapEvent(MotionEvent e) {return false;}
	public boolean onSingleTapConfirmed(MotionEvent e) {return false;}
	public boolean onDown(MotionEvent e) {return false;}
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {return false;}
	public void onLongPress(MotionEvent e) {}
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {return false;}
	public void onShowPress(MotionEvent e) {}
	public boolean onSingleTapUp(MotionEvent e) {return false;}
}