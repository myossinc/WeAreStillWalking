package com.example.offlinenavigation;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.offlinenavigation.AssetsController.RefImage;
import com.example.offlinenavigation.CustomSpinner.Type;

public class MainActivity extends Activity implements CvCameraViewListener2,
		ImageCompare.CompareThreadStatusListener, CompareProgressDialog.CancelComparissonListener {

	private CameraBridgeViewBase cameraView;
	private ImageView splashScreen;

	private CustomSpinner detectorDropdown, descriptorDropdown,
			matcherDropdown;

	private CompareProgressDialog compareProgressDialog;
	private NumberPickerDialog numberPickerDialog;

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
				setupViews();
				if (splashScreen != null) {
					splashScreen.setVisibility(View.GONE);
				}
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
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	private void setupViews() {
		cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_image);
		cameraView.setVisibility(SurfaceView.VISIBLE);
		cameraView.setCvCameraViewListener(this);
		cameraView.enableView();

		splashScreen = (ImageView) findViewById(R.id.splash_screen);
		splashScreen.setBackgroundColor(Color.WHITE);

		numberPickerDialog = new NumberPickerDialog(this);		
	}

	private void setupSpinners() {
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		MenuItem detectorItem = menu.findItem(R.id.detector_dropdown);
		MenuItem descriptorItem = menu.findItem(R.id.descriptor_dropdown);
		MenuItem matcherItem = menu.findItem(R.id.matcher_dropdown);
		detectorDropdown = (CustomSpinner) detectorItem.getActionView();
		descriptorDropdown = (CustomSpinner) descriptorItem.getActionView();
		matcherDropdown = (CustomSpinner) matcherItem.getActionView();
		setupSpinners();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
		case R.id.open_edit_min_distance_dialog:
			onOpenEditMinDistanceDialog();
			break;
		case R.id.action_takePicture:
			onTakePicture();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void onOpenEditMinDistanceDialog(){
		numberPickerDialog.show();
	}
	
	private void onTakePicture() {
		if (canStartNewThread) {
			takePictureOnNextFrame();
		} else {
			showToast("Image comparison is currently working ... blocked!");
		}
	}
	
	private void takePictureOnNextFrame(){
		shouldTakePictureOnNextFrame = true;
	}

	private void showToast(String msg){
		Toast.makeText(MainActivity.this,
				msg,
				Toast.LENGTH_SHORT).show();
	}
	
	private void cancelThread() {
		if (mCompareThread != null) {
			mCompareThread.cancel(false);
			showToast("Comparing cancelled");
			compareProgressDialog.resetDialog();
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
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this,
					mLoaderCallback);
		} else {
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		m_AssetController = new AssetsController(MainActivity.this, width,
				height);

		compareProgressDialog = new CompareProgressDialog(this,
				m_AssetController.getImages().size(), this);
	}

	@Override
	public void onCameraViewStopped() {
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		if (canStartNewThread && shouldTakePictureOnNextFrame) {
			canStartNewThread = false;
			shouldTakePictureOnNextFrame = false;

			final Mat frameImage = inputFrame.rgba().clone();

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					compareProgressDialog.show();
				}
			});

			ImageCompare ic = new ImageCompare(frameImage, m_AssetController,
					this, numberPickerDialog.getValue());
			mCompareThread = ic.startComparing();
		}

		return inputFrame.rgba();
	}

	@Override
	public void onComparingStarted() {
	}

	@Override
	public void onCompareFinished(final RefImage bestFittingImage) {
		canStartNewThread = true;
		mCompareThread = null;
		compareProgressDialog.resetDialog();
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (bestFittingImage != null) {
					showToast("Search completed");
				} else {
					showToast("No match found");
				}
			}
		});
	}

	@Override
	public void onCompareSinglePictureFinished(final RefImage image) {
		MainActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (image != null && image.m_CompareImage != null) {
					Bitmap bm = Bitmap.createBitmap(
							image.m_CompareImage.cols(),
							image.m_CompareImage.rows(),
							Bitmap.Config.ARGB_8888);
					Utils.matToBitmap(image.m_CompareImage, bm);

					compareProgressDialog.setImageViewContent(bm,
							R.id.keypoint_image);
				}

				compareProgressDialog.updateProgress();
			}

		});
	}

	@Override
	public void onComparissonCanceled() {
		cancelThread();
	}
}
