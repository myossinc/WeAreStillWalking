package com.example.offlinenavigation;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

public class FancyButton extends Button {
	private static final String ROTATION = "rotation";
	
	public interface FancyButtonListener {
		public void onFancyAnimationEnded();
	}

	private FancyButtonListener listener;

	public FancyButton(Context context) {
		super(context);
	}
	
	public FancyButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	public FancyButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public void setListener(FancyButtonListener listener){
		this.listener = listener;
	}
	
	public void startAnimation() {
		animateButton(500);
		blinkButton();
		animateButton(1000);
		blinkButton();
	}
	
	private void animateButton(int duration){
		ObjectAnimator anim = ObjectAnimator.ofFloat(this, ROTATION, 0f, 1080f);
		anim.setDuration(duration);
		anim.start();
	}

	private void blinkButton() {
		this.setBackgroundColor(Color.GREEN);
		final Animation animation = new AlphaAnimation(1f, 0.2f);
		animation.setDuration(10);
		animation.setInterpolator(new LinearInterpolator());
		animation.setRepeatCount(Animation.INFINITE);
		animation.setRepeatMode(Animation.REVERSE);
		this.startAnimation(animation);
		stopBlinkingAfterOneSecond();
	}

	private void stopBlinkingAfterOneSecond() {
		this.postDelayed(new Runnable() {
			@Override
			public void run() {
				FancyButton.this.clearAnimation();
				listener.onFancyAnimationEnded();
			}
		}, 500);
	}
}
