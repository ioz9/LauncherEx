/* 
 * This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://sam.zoy.org/wtfpl/COPYING for more details.
 */

package com.android.launcherex;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

public class AnimatedDrawer extends RelativeLayout {
	private int mAnimationIn = 0;
	private int mAnimationOut = 0;
	private Animation mAnimation;

	public AnimatedDrawer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
    /** @param animate Animation toggle, only applies if you utilized setAnimationStyle */
	public void open(boolean animate) {
		if (this.getVisibility() != View.VISIBLE) {
			if (animate && (mAnimationIn != 0)) {
				mAnimation = AnimationUtils.loadAnimation(getContext(), mAnimationIn);
				mAnimation.reset();
				this.clearAnimation();
				this.startAnimation(mAnimation);
			}
		    this.setVisibility(View.VISIBLE);
		}
	}
	
    /** @param animate Animation toggle, only applies if you utilized setAnimationStyle */
	public void close(boolean animate) {
		if (this.getVisibility() != View.GONE) {
			if (animate && (mAnimationOut != 0)) {
				mAnimation = AnimationUtils.loadAnimation(getContext(), mAnimationOut);
				mAnimation.reset();
				this.clearAnimation();
				this.startAnimation(mAnimation);
			}
			this.setVisibility(View.GONE);
		}
	}
	
    /**
     * Sets the animation style
     * 
     * @param open Animation used with open(true)
     * @param close Animation used with close(true)
     */
	public void setAnimationStyle(int open, int close) {
		mAnimationIn = open;
		mAnimationOut = close;
	}
}