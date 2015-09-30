package com.example.offlinenavigation;

import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
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
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2, ImageCompare.CompareThreadStatusListener {

	private static final String USER_NODE = "Aktueller Node: ";
	private static final String ROTATION = "rotation";
	private static final String BASE_URI = "http://urwalking.ur.de/navi/index.php?";
	private static final String START_AREA = "startArea=";
	private static final String START_NODE = "startNode=";
	private String areaValue = "";
	private String nodeValue = "";

	private Button button;
	private TextView currentNodeLabel;
	private TextView matchPercentageLabel;

	private CameraBridgeViewBase cameraView;
	private ImageView imageFromAssets;
	private ImageView splashScreen;
	
	/* Progress Bar */
	private ProgressBar progressBar;
	private int progressBarProgress = 0;
	
	private AssetsController m_AssetController;
	
	static {
	    System.loadLibrary("opencv_java");
	    System.loadLibrary("nonfree");
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				cameraView.enableView();
				m_AssetController = new AssetsController(MainActivity.this);
				progressBar.setMax(m_AssetController.getImages().size()+1);
				progressBar.setProgress(0);
				//splashScreen.setVisibility(View.GONE);
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
	}

	Handler splashHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
            switch (msg.what) {
            case 0:
                    //remove SplashScreen from view
                    splashScreen.setVisibility(View.GONE);
                    break;
            }
            super.handleMessage(msg);
		}
	};
	
	private void setupViews() {
		/* Right side Image view */
		imageFromAssets = (ImageView) findViewById(R.id.matched_image1);
		imageFromAssets.setBackgroundColor(Color.LTGRAY);
		
		splashScreen = (ImageView) findViewById(R.id.splashScreen);
		splashScreen.setBackgroundColor(Color.WHITE);
		splashHandler.sendEmptyMessageDelayed(0, 1500);
		
		currentNodeLabel = (TextView) findViewById(R.id.node_name);
		currentNodeLabel.setText("No matching node so far");
		
		matchPercentageLabel = (TextView) findViewById(R.id.matchPercentage);
		matchPercentageLabel.setText("Match Percentage: 0.0%");
		
		button = (Button) findViewById(R.id.fanciest_button_ever);
		button.setEnabled(false);
		
		progressBar = (ProgressBar) findViewById(R.id.threadProgressBar);
		progressBar.setProgress(0);
		
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
		showNode(areaValue + " " + nodeValue);
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
				openUrWalking();
			}

			private void openUrWalking() {
				String url = BASE_URI + START_AREA + areaValue + "&" + START_NODE + nodeValue;
				Uri uri = Uri.parse(url);
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		}, 500);
	}

	private void showNode(String node) {
		currentNodeLabel.setText(USER_NODE + node);
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
			m_PreviewEnabled = !m_PreviewEnabled;
			return true;
		} else if (id == R.id.action_takePicture) {
			if (canStartNewThread) {
				shouldTakePictureOnNextFrame = true;
				//item.setEnabled(false);
			}
			else {
				Toast.makeText(this, "Image comparison is currently working ... blocked!", Toast.LENGTH_SHORT).show();
			}
		} else if (id == R.id.action_cancelThread) {
			if (mCompareThread != null) {
				mCompareThread.cancel(false);
				Toast.makeText(this, "Comparing cancelled", Toast.LENGTH_SHORT).show();
				progressBar.setProgress(0);
				progressBarProgress = 0;
				canStartNewThread = true;
			}
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

	private boolean shouldTakePictureOnNextFrame = false;
	private boolean canStartNewThread = true;
	
	private ImageCompare.CompareImages mCompareThread = null;
	
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		if (canStartNewThread && shouldTakePictureOnNextFrame) {
			canStartNewThread = false;
			shouldTakePictureOnNextFrame = false;
			ImageCompare ic = new ImageCompare(inputFrame.rgba().clone(), m_AssetController, this);
			mCompareThread = ic.startComparing();
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
		//Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
		Log.e("CompareThread", "Started");
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				progressBarProgress = 0;
				progressBar.setProgress(progressBarProgress);
			}
		});
	}

	@Override
	public void onCompareFinished(final RefImage bestFittingImage) {
		canStartNewThread = true;
		Log.e("CompareThread", "Finished");
		mCompareThread = null;
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (bestFittingImage != null) {
					Toast.makeText(MainActivity.this, "Search completed", Toast.LENGTH_SHORT).show();
					imageFromAssets.setImageBitmap(bestFittingImage.getImage());
					currentNodeLabel.setText("You are at this node: " + bestFittingImage.m_Name);
					button.setEnabled(true);
					
					String[] data = bestFittingImage.m_Name.split("_");
					areaValue = data[0];
					nodeValue = data[2];
					
					setMatchPercentage(bestFittingImage.m_Percentage);
				}
				else {
					Toast.makeText(MainActivity.this, "No match found", Toast.LENGTH_SHORT).show();
				}
			}
			
		});
	}

	private boolean m_PreviewEnabled = true;
	
	@Override
	public void onCompareSinglePictureFinished(final RefImage image) {
		//Log.e("CompareThread", "One finished");
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				//Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
				
				if (image != null && image.m_CompareImage != null && m_PreviewEnabled) {
					Bitmap b = Bitmap.createBitmap(image.m_CompareImage.cols(), image.m_CompareImage.rows(), Bitmap.Config.ARGB_8888);
					Utils.matToBitmap(image.m_CompareImage, b);
					imageFromAssets.setImageBitmap(b);
					setMatchPercentage(image.m_Percentage);
					
					Log.e("COMPARE_SINGLE", image.m_Name + " | Matching: " + image.m_Percentage + "%");
				}
				
				progressBar.setProgress(progressBarProgress++);
			}
			
		});
	}
	
	private void setMatchPercentage(double matchPer) {
		matchPer *= 100.0f;
		matchPercentageLabel.setText("Match Percentage: " + String.format("%.3f", matchPer) + "%");
	}
}
