package com.example.offlinenavigation;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.NumberPicker;

public class NumberPickerDialog extends Dialog {

	private NumberPicker minDistancePicker;
	
	private Button okButton;

	public NumberPickerDialog(Context context, int theme) {
		super(context, theme);
		setupDialogView();
		setupNumberPicker();
	}

	public NumberPickerDialog(Context context) {
		super(context);
		setupDialogView();
		setupViews();
	}

	private void setupViews() {
		setupNumberPicker();
		setupOkButton();
		setupButtonListener();
	}

	private void setupNumberPicker() {
		minDistancePicker = (NumberPicker) this
				.findViewById(R.id.min_distance_picker);
		
		minDistancePicker.setValue(15);
		minDistancePicker.setMinValue(0);
		minDistancePicker.setMaxValue(500);
	}

	private void setupOkButton(){
		okButton = (Button) this.findViewById(R.id.ok_button);
	}
	
	private void setupButtonListener(){
		okButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				NumberPickerDialog.this.dismiss();
			}
		});
	}
	
	private void setupDialogView() {
		boolean windowFeatureRequested = this
				.requestWindowFeature(Window.FEATURE_NO_TITLE);

		if (windowFeatureRequested) {
			this.setContentView(getLayoutInflater().inflate(
					R.layout.number_picker_dialog, null));
			this.setCancelable(false);
		}
	}

	public int getValue() {
		return minDistancePicker.getValue();
	}

}
