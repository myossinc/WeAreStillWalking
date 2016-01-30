package com.example.offlinenavigation;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.offlinenavigation.AssetsController.RefImage;

public class CompareProgressDialog extends Dialog {

	public interface CancelComparissonListener {
		public void onComparissonCanceled();
	}

	private static final String USER_NODE = "Aktueller Node: ";
	private static final String MATCHES = "Matches: ";

	private CancelComparissonListener listener;

	private ProgressBar progressBar;
	private ImageView keypointImageView;
	private TextView nodeNameView;
	private TextView matchesView;

	private int maxProgress;

	private boolean windowFeatureRequested = false;

	public CompareProgressDialog(Context context, int theme, int maxProgress,
			CancelComparissonListener listener) {
		super(context, theme);
		this.maxProgress = maxProgress;
		this.listener = listener;

		setupViews(context);
	}

	public CompareProgressDialog(Context context, int maxProgress,
			CancelComparissonListener listener) {
		super(context);
		this.maxProgress = maxProgress;
		this.listener = listener;

		setupViews(context);
	}

	public void setListener(CancelComparissonListener listener) {
		this.listener = listener;
	}

	public void resetDialog() {
		progressBar.setProgress(0);
		setText(nodeNameView, "");
		setText(matchesView, "");
		setBitmap(keypointImageView, null);
	}

	public void updateProgress() {
		progressBar.setProgress(progressBar.getProgress() + 1);
	}
	
	public void setCurrentNodeInformation(RefImage image){
		setText(nodeNameView, USER_NODE +  image.m_Name);
		setText(matchesView, MATCHES + (int) image.m_matches);
	}

	public void setImageViewContent(Bitmap bm, int id) {
		switch (id) {
		case R.id.keypoint_image:
			setBitmap(keypointImageView, bm);
		}
	}

	private void setBitmap(ImageView view, Bitmap bm) {
		view.setImageBitmap(bm);
	}
	
	public void setTextViewContent(String content, int id) {
		switch (id) {
		case R.id.node_name:
			setText(nodeNameView, USER_NODE + " " + content);
			break;
		case R.id.matches:
			setText(matchesView, content);
			break;
		}
	}

	private void setText(TextView view, String content){
		view.setText(content);
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

	private void setDialogFullscreen() {
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
		setupTextViews();
	}

	private void setupTextViews() {
		nodeNameView = (TextView) this.findViewById(R.id.node_name);
		matchesView = (TextView) this.findViewById(R.id.matches);
	}

	private void setupProgressBar() {
		progressBar = (ProgressBar) this
				.findViewById(R.id.image_compare_progress);

		progressBar.setMax(maxProgress);
		progressBar.setProgress(1);
	}

	private void setupImageViews() {
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
}
