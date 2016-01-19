package com.example.offlinenavigation;

import java.util.ArrayList;
import java.util.List;

import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class CustomSpinner extends Spinner {

	public enum Type {
		DESCRIPTOR(0), DETECTOR(1), MATCHER(2);

		private final int value;
		private final int[] DETECTOR_LIST = { FeatureDetector.SIFT,
				FeatureDetector.SURF,  FeatureDetector.ORB };
		private final int[] DESCRIPTOR_LIST = { DescriptorExtractor.SIFT,
				DescriptorExtractor.SURF, DescriptorExtractor.BRIEF };
		private final int[] MATCHER_LIST = { DescriptorMatcher.FLANNBASED, DescriptorMatcher.BRUTEFORCE_HAMMING };

		private final List<int[]> TYPE_LISTS;

		@SuppressWarnings("serial")
		private Type(int value) {
			this.value = value;

			TYPE_LISTS = new ArrayList<int[]>() {
				{
					add(DESCRIPTOR_LIST);
					add(DETECTOR_LIST);
					add(MATCHER_LIST);
				}
			};
		}

		public int getValue() {
			return value;
		}

		public int[] getType() {
			return TYPE_LISTS.get(this.value);
		}
	}

	@SuppressWarnings("serial")
	public static final ArrayList<String> DETECTOR_LIST = new ArrayList<String>() {
		{
			add("SIFT");
			add("SURF");
			add("ORB");
		}
	};
	@SuppressWarnings("serial")
	public static final ArrayList<String> DESCRIPTOR_LIST = new ArrayList<String>() {
		{
			add("SIFT");
			add("SURF");
			add("BRIEF");
		}
	};
	@SuppressWarnings("serial")
	public static final ArrayList<String> MATCHER_LIST = new ArrayList<String>() {
		{
			add("FLANN");
			add("BRUT_HAM");
		}
	};

	private Type type;

	public CustomSpinner(Context context) {
		super(context);
	}

	public CustomSpinner(Context context, int mode) {
		super(context, mode);
	}

	public CustomSpinner(Context context, AttributeSet attrs, int defStyleAttr,
			int mode) {
		super(context, attrs, defStyleAttr, mode);
	}

	public CustomSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public CustomSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setType(Type type) {
		this.type = type;
	}

	public void setupSpinner(Context context, List<String> displayList) {
		ArrayAdapter<String> descriptorAdapter = new ArrayAdapter<String>(
				context, android.R.layout.simple_spinner_item, displayList);

		descriptorAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		this.setAdapter(descriptorAdapter);
	}

	public void setListener() {
		this.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {

				switch (type) {
				case DESCRIPTOR:
					ImageCompare.setDescriptorExtractor(type.getType()[position]);
					break;
				case DETECTOR:
					ImageCompare.setFeatureDetector(type.getType()[position]);
					break;
				case MATCHER:
					ImageCompare.setDescriptorMatcher(type.getType()[position]);
					break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}
		});
	}
}
