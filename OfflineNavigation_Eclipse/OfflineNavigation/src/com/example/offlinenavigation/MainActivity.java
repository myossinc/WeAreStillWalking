package com.example.offlinenavigation;

import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import com.example.offlinenavigation.AssetsController.RefImage;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements CvCameraViewListener2, ImageCompare.CompareThreadStatusListener {

	private static final String WATCH_BUTTON = "Schau dir lieber den fancy Button an!";
	private static final String USER_NODE = "Der Nutzer befindet sich auf dem Node: ";
	private static final String ROTATION = "rotation";
	private static final String BASE_URI = "http://urwalking.ur.de/navi/index.php?";
	private static final String START_AREA = "startArea=";
	private static final String START_NODE = "startNode=";
	private String areaValue = "";
	private String nodeValue = "";

	private Button button;
	private TextView text;

	private CameraBridgeViewBase cameraView;
	private ImageView imageFromAssets;
	
	private AssetsController m_AssetController;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				cameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MainActivity() {
		System.out.println("started");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setupViews();
		setupListeners();		

		m_AssetController = new AssetsController(this);
		
		// Get test image from assets folder
		Bitmap temp = getBitmapFromAsset(this, "ZHG_2_33_2.JPG");
		imageFromAssets.setImageBitmap(temp);
	}

	private void setupViews() {
		imageFromAssets = (ImageView) findViewById(R.id.matched_image);
		text = (TextView) findViewById(R.id.node_name);
		button = (Button) findViewById(R.id.fanciest_button_ever);
		cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_image);
		cameraView.setVisibility(SurfaceView.VISIBLE);
		cameraView.setCvCameraViewListener(this);
	}

	private void setupListeners() {
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startNavigation();
			}
		});
	}

	private void startNavigation() {
		ObjectAnimator anim = ObjectAnimator.ofFloat(button, ROTATION, 0f, 1080f);
		anim.setDuration(500);
		anim.start();
		blinkButton();
		showNode(WATCH_BUTTON);
	}

	private void blinkButton() {
		button.setBackgroundColor(Color.GREEN);
		final Animation animation = new AlphaAnimation(1f, 0.2f);
		animation.setDuration(10);
		animation.setInterpolator(new LinearInterpolator());
		animation.setRepeatCount(Animation.INFINITE);
		animation.setRepeatMode(Animation.REVERSE);
		button.startAnimation(animation);
		stopBlinkingAfterOneSecond();
	}

	private void stopBlinkingAfterOneSecond() {
		button.postDelayed(new Runnable() {
			@Override
			public void run() {
				button.clearAnimation();
				String url = BASE_URI + START_AREA + areaValue + "&" + START_NODE + nodeValue;
				Uri uri = Uri.parse(url);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		}, 500);
	}

	private void showNode(String node) {
		text.setText(USER_NODE + node);
		ObjectAnimator anim = ObjectAnimator.ofFloat(button, ROTATION, 0f, 1080f);
		anim.setDuration(1000);
		anim.start();
		blinkButton();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (cameraView != null)
			cameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
		} else {
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
	}

	@Override
	public void onCameraViewStopped() {
	}

	private boolean shouldStartNewThread = true;
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		if (shouldStartNewThread) {
			shouldStartNewThread = false;
			ImageCompare ic = new ImageCompare(inputFrame.gray(), m_AssetController, this);
			ic.startComparing();
		}
		return inputFrame.rgba();
	}

	// Get bitmap from assets folder
	public static Bitmap getBitmapFromAsset(Context context, String filePath) {
		AssetManager assetManager = context.getAssets();

		InputStream istr;
		Bitmap bitmap = null;
		try {
			istr = assetManager.open(filePath);
			bitmap = BitmapFactory.decodeStream(istr);
		} catch (IOException e) {
			// handle exception
		}

		return bitmap;
	}

	@Override
	public void onComparingStarted() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCompareFinished(RefImage bestFittingImage) {
		shouldStartNewThread = true;
	}

	@Override
	public void onCompareSinglePictureFinished(RefImage image) {
		// TODO Auto-generated method stub
		
	}
}
