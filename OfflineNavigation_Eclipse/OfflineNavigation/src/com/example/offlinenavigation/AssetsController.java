package com.example.offlinenavigation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.android.*;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class AssetsController {
	
	public class RefImage {
		public String m_Name;
		public Mat m_CvImage;
		public Bitmap m_Bitmap;
	}
	
	private ArrayList<RefImage> m_ReferenceImages;
	
	private Context m_Context;
	private AssetManager m_AssetManager;
	
	public AssetsController(Context context) {
		m_Context = context;
		m_ReferenceImages = new ArrayList<RefImage>();
		m_AssetManager = m_Context.getAssets();
		loadAssets();
	}
	
	private void loadAssets() {
		loadImage("ZHG_2_33_2.JPG");
		loadImage("ZHG_2_33_3.JPG");
		loadImage("ZHG_2_33_4.JPG");
		loadImage("ZHG_2_33_5.JPG");
		
		loadImage("ZHG_2_34_1.JPG");
		loadImage("ZHG_2_34_2.JPG");
		loadImage("ZHG_2_34_3.JPG");
		loadImage("ZHG_2_34_4.JPG");
		
		loadImage("ZHG_2_36_1.JPG");
		loadImage("ZHG_2_36_2.JPG");
		loadImage("ZHG_2_36_3.JPG");
		loadImage("ZHG_2_36_4.JPG");
		
		loadImage("ZHG_2_39_1.JPG");
		loadImage("ZHG_2_39_2.JPG");
		loadImage("ZHG_2_39_3.JPG");
		loadImage("ZHG_2_39_4.JPG");
		
		loadImage("ZHG_2_45_1.JPG");
		loadImage("ZHG_2_45_2.JPG");
		loadImage("ZHG_2_45_3.JPG");
		loadImage("ZHG_2_45_4.JPG");
		loadImage("ZHG_2_45_5.JPG");
		
		loadImage("ZHG_2_47_1.JPG");
		loadImage("ZHG_2_47_2.JPG");
		loadImage("ZHG_2_47_3.JPG");
		loadImage("ZHG_2_47_4.JPG");
		
		loadImage("ZHG_2_48_1.JPG");
		loadImage("ZHG_2_48_2.JPG");
		loadImage("ZHG_2_48_3.JPG");
		loadImage("ZHG_2_48_4.JPG");
		loadImage("ZHG_2_48_5.JPG");
		
		loadImage("ZHG_2_49_1.JPG");
		loadImage("ZHG_2_49_2.JPG");
		loadImage("ZHG_2_49_3.JPG");
		loadImage("ZHG_2_49_4.JPG");
		loadImage("ZHG_2_49_5.JPG");
		
		loadImage("ZHG_2_50_1.JPG");
		loadImage("ZHG_2_50_2.JPG");
		loadImage("ZHG_2_50_3.JPG");
		loadImage("ZHG_2_50_4.JPG");
		loadImage("ZHG_2_50_5.JPG");
	}
	
	public ArrayList<RefImage> getImages() {
		return m_ReferenceImages;
	}
	
	private void loadImage(String name) {
		Bitmap image = getBitmapFromAsset(name);
		Mat cvImage = new Mat();
		Utils.bitmapToMat(image, cvImage);
		name = name.replace(".JPG", "");
		
		RefImage object = new RefImage();
		object.m_Bitmap = image;
		object.m_CvImage = cvImage;
		object.m_Name = name;
		
		m_ReferenceImages.add(object);
	}
	
	// Get bitmap from assets folder
	private Bitmap getBitmapFromAsset(String filePath) {

		InputStream istr;
		Bitmap bitmap = null;
		try {
			istr = m_AssetManager.open(filePath);
			bitmap = BitmapFactory.decodeStream(istr);
		} catch (IOException e) {
			// handle exception
		}

		return bitmap;
	}
	

}
