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
import org.opencv.core.MatOfKeyPoint;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.offlinenavigation.AssetsController.RefImage;
import com.example.offlinenavigation.CustomSpinner.Type;
import com.example.offlinenavigation.FancyButton.fancyButtonListener;

public class MainActivity extends Activity implements CvCameraViewListener2, ImageCompare.CompareThreadStatusListener, fancyButtonListener {

	private static final String USER_NODE = "Aktueller Node: ";
	private static final String BASE_URI = "http://urwalking.ur.de/navi/index.php?";
	private static final String START_AREA = "startArea=";
	private static final String START_NODE = "startNode=";
	
	private String areaValue = "";
	private String nodeValue = "";

	private FancyButton button;
	private TextView currentNodeLabel;
	private TextView matchPercentageLabel;

	private CameraBridgeViewBase cameraView;
	private ImageView imageFromAssets;
	private ImageView splashScreen;
	
	private CustomSpinner detectorDropdown, descriptorDropdown, matcherDropdown;
	
	
	/* Progress Bar */
	private ProgressBar progressBar;
	private int progressBarProgress = 0;
	
	private AssetsController m_AssetController;
	
	private boolean shouldTakePictureOnNextFrame = false;
	private boolean canStartNewThread = true;
	
	private ImageCompare.CompareImages mCompareThread = null;
	
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
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MainActivity() {}
	
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
		matchPercentageLabel.setText("Matches: 0");
		
		button = (FancyButton) findViewById(R.id.fanciest_button_ever);
		button.setListener(this);
		button.setEnabled(false);
		
		progressBar = (ProgressBar) findViewById(R.id.threadProgressBar);
		progressBar.setProgress(0);
		
		cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_image);
		cameraView.setVisibility(SurfaceView.VISIBLE);
		cameraView.setCvCameraViewListener(this);
	}

	private void setupSpinners(){	
		detectorDropdown.setType(Type.DETECTOR);
		detectorDropdown.setupSpinner(this, CustomSpinner.DETECTOR_LIST);
		detectorDropdown.setListener();
		
		descriptorDropdown.setType(Type.DESCRIPTOR);
		descriptorDropdown.setupSpinner(this, CustomSpinner.DESCRIPTOR_LIST);
		descriptorDropdown.setListener();
		
		matcherDropdown.setType(Type.MATCHER);
		matcherDropdown.setupSpinner(this, CustomSpinner.MATCHER_LIST);
		matcherDropdown.setListener();
	}
	
	private void setupListeners() {
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				button.startAnimation(currentNodeLabel, USER_NODE, nodeValue);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		MenuItem detectorItem = menu.findItem(R.id.detectorDropdown);
		MenuItem descriptorItem = menu.findItem(R.id.descriptorDropdown);
		MenuItem matcherItem = menu.findItem(R.id.matcherDropdown);
		detectorDropdown = (CustomSpinner) detectorItem.getActionView();
		descriptorDropdown = (CustomSpinner) descriptorItem.getActionView();
		matcherDropdown = (CustomSpinner) matcherItem.getActionView();
		setupSpinners();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch(id) {
		case R.id.action_settings:
			onActionSettings();
			break;
		case R.id.action_takePicture:
//			inflateNewLayout();
			onTakePicture();
			break;
		case R.id.action_cancelThread:
			onCancelThread();
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void onActionSettings(){
		m_PreviewEnabled = !m_PreviewEnabled;
	}
	
	private void onTakePicture(){
		if (canStartNewThread) {
			shouldTakePictureOnNextFrame = true;
		}
		else {
			Toast.makeText(this, "Image comparison is currently working ... blocked!", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void onCancelThread(){
		if (mCompareThread != null) {
			mCompareThread.cancel(false);
			Toast.makeText(this, "Comparing cancelled", Toast.LENGTH_SHORT).show();
			progressBar.setProgress(0);
			progressBarProgress = 0;
			canStartNewThread = true;
		}
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
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
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
					
					setGoodMatchesNumber(bestFittingImage.m_Percentage);
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
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (image != null && image.m_CompareImage != null && m_PreviewEnabled) {
					Bitmap b = Bitmap.createBitmap(image.m_CompareImage.cols(), image.m_CompareImage.rows(), Bitmap.Config.ARGB_8888);
					Utils.matToBitmap(image.m_CompareImage, b);
					imageFromAssets.setImageBitmap(b);
					setGoodMatchesNumber(image.m_Percentage);
					Log.e("COMPARE_SINGLE", image.m_Name + " | Matching: " + image.m_Percentage + "%");
				}
				
				progressBar.setProgress(progressBarProgress++);
			}
			
		});
	}
	
	private void setGoodMatchesNumber(double matchPer){
		matchPercentageLabel.setText("Matches: " + (int) matchPer);
	}

	private void openUrWalking() {
		String url = BASE_URI + START_AREA + areaValue + "&"
				+ START_NODE + nodeValue;
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}
	
	@Override
	public void onFancyAnimationEnded() {
		openUrWalking();
	}
	
	private void inflateNewLayout(){
		LayoutInflater inflater = (LayoutInflater)   this.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		inflater.inflate(R.layout.test, null);
	}
}
