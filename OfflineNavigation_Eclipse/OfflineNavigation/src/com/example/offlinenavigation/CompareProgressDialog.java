package com.example.offlinenavigation;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

;

public class CompareProgressDialog extends Dialog implements FancyButton.FancyButtonListener{

	public interface CancelComparissonListener {
		public void onComparissonCanceled();
	}
	
	private static final String USER_NODE = "Aktueller Node: ";
	private static final String BASE_URI = "http://urwalking.ur.de/navi/index.php?";
	private static final String START_AREA = "startArea=";
	private static final String START_NODE = "startNode=";

	private String areaValue = "";
	private String nodeValue = "";
	
	private CancelComparissonListener listener;
	
	private ProgressBar progressBar;
	private ImageView databaseImageView;
	private ImageView referenceImageView;
	private ImageView keypointImageView;
	
	private FancyButton button;

	private int maxProgress;

	private boolean windowFeatureRequested = false;

	public CompareProgressDialog(Context context, int theme, int maxProgress, CancelComparissonListener listener) {
		super(context, theme);
		this.maxProgress = maxProgress;
		this.listener = listener;

		setupViews(context);
	}

	public CompareProgressDialog(Context context, int maxProgress, CancelComparissonListener listener) {
		super(context);
		this.maxProgress = maxProgress;
		this.listener = listener;

		setupViews(context);
	}
	
	public void setListener(CancelComparissonListener listener){
		this.listener = listener;
	}

	public void resetDialog() {
		progressBar.setProgress(1);
	}

	public void updateProgress() {
		progressBar.setProgress(progressBar.getProgress() + 1);
	}

	public void setImageViewContent(Bitmap bm, int id) {
		switch (id) {
		case R.id.database_image:
			setBitmap(databaseImageView, bm);
			break;
		case R.id.reference_image:
			setBitmap(referenceImageView, bm);
			break;
		case R.id.keypoint_image:
			setBitmap(keypointImageView, bm);
		}
	}

	private void setBitmap(ImageView view, Bitmap bm) {
		view.setImageBitmap(bm);
	}

	private void setupDialogView() {
		windowFeatureRequested = this
				.requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (windowFeatureRequested) {
			this.setContentView(getLayoutInflater().inflate(
					R.layout.compare_progress_dialog, null));

			this.setCancelable(false);
			
			setDialogFullscreen();
		}
	}
	
	private void setDialogFullscreen(){
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

		lp.copyFrom(this.getWindow().getAttributes());

		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.MATCH_PARENT;

		this.getWindow().setAttributes(lp);
	}

	private void setupViews(Context context) {
		setupDialogView();
		setupProgressBar();
		setupImageViews();
		setupCancelButton();
		setupFancyButton();
	}
	
	private void setupFancyButton(){
		button = (FancyButton) findViewById(R.id.fanciest_button_ever);
		button.setListener(this);
		button.setEnabled(false);
		
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				button.startAnimation(currentNodeLabel, USER_NODE, nodeValue);
			}
		});
	}

	private void setupProgressBar() {
		progressBar = (ProgressBar) this
				.findViewById(R.id.image_compare_progress);

		progressBar.setMax(maxProgress);
		progressBar.setProgress(1);
	}

	private void setupImageViews() {
		databaseImageView = (ImageView) this.findViewById(R.id.database_image);
		referenceImageView = (ImageView) this
				.findViewById(R.id.reference_image);
		keypointImageView = (ImageView) this.findViewById(R.id.keypoint_image);
	}

	private void setupCancelButton() {
		Button b = (Button) this.findViewById(R.id.button_cancel);

		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onComparissonCanceled();
				CompareProgressDialog.this.dismiss();
			}
		});

	}

	private void openUrWalking() {
		String url = BASE_URI + START_AREA + areaValue + "&" + START_NODE
				+ nodeValue;
		Uri uri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		this.getContext().startActivity(intent);
	}

	@Override
	public void onFancyAnimationEnded() {
		openUrWalking();
	}
}
