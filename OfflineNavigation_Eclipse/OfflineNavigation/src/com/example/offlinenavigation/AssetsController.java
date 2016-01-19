package com.example.offlinenavigation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class AssetsController {

	public class RefImage {
		public String m_Name;
		public String m_FullName;
		public Mat m_CompareImage;
		public double m_Percentage = 0.0f;

		public MatOfKeyPoint m_Keypoints = null;
		public Mat m_Descriptors = null;

		private String areaValue = "";
		private String nodeValue = "";

		public RefImage(String name){
			this.m_FullName = name;
			
			removeImageEnding();
			extractNodeFromName();
			extractAreaFromName();
		}
		
		public Bitmap getImage() {
			return getBitmapFromAsset(m_FullName);
		}
	
		private void removeImageEnding(){
			this.m_Name = this.m_FullName.replace(".jpg", "");
		}
		
		private void extractNodeFromName(){
			try {
				String[] data = this.m_Name.split("_");
				nodeValue = data[2];
			} catch (Exception e) {
				nodeValue = "ährohr";
			}
		}
		
		private void extractAreaFromName(){
			try {
				String[] data = this.m_Name.split("_");
				areaValue = data[0];
			} catch (Exception e) {
				areaValue = "ährohr";
			}
		}

		public String getAreaValue() {
			return areaValue;
		}

		public String getNodeValue() {
			return nodeValue;
		}
	}

	private ArrayList<RefImage> m_ReferenceImages;

	private Context m_Context;
	private AssetManager m_AssetManager;
	private int m_width;
	private int m_height;

	public AssetsController(Context context, int width, int height) {
		m_Context = context;
		m_ReferenceImages = new ArrayList<RefImage>();
		m_AssetManager = m_Context.getAssets();
		m_width = width;
		m_height = height;
		loadAssets();
	}

	private void loadAssets() {
		addImage("doors/single/inside/hoch/moli.jpg");
		addImage("doors/single/inside/hoch/mori.jpg");
		addImage("doors/single/inside/hoch/muli.jpg");
		addImage("doors/single/inside/hoch/muri.jpg");
		addImage("doors/single/inside/hoch/oli.jpg");
		addImage("doors/single/inside/hoch/ori.jpg");
		addImage("doors/single/inside/hoch/ui.jpg");

		addImage("doors/single/inside/quer/moli.jpg");
		addImage("doors/single/inside/quer/mori.jpg");
		addImage("doors/single/inside/quer/muli.jpg");
		addImage("doors/single/inside/quer/muri.jpg");
		addImage("doors/single/inside/quer/oli.jpg");
		addImage("doors/single/inside/quer/ori.jpg");
		addImage("doors/single/inside/quer/ui.jpg");

		addImage("doors/single/outside/hoch/mola.jpg");
		addImage("doors/single/outside/hoch/mora.jpg");
		addImage("doors/single/outside/hoch/mula.jpg");
		addImage("doors/single/outside/hoch/mura.jpg");
		addImage("doors/single/outside/hoch/ola.jpg");
		addImage("doors/single/outside/hoch/ora.jpg");
		addImage("doors/single/outside/hoch/ua.jpg");

		addImage("doors/single/outside/quer/mola.jpg");
		addImage("doors/single/outside/quer/mora.jpg");
		addImage("doors/single/outside/quer/mula.jpg");
		addImage("doors/single/outside/quer/mura.jpg");
		addImage("doors/single/outside/quer/ola.jpg");
		addImage("doors/single/outside/quer/ora.jpg");
		addImage("doors/single/outside/quer/ua.jpg");
	}

	private void addImage(String name) {
		RefImage image = new RefImage(name);
	
		m_ReferenceImages.add(image);
	}

	public ArrayList<RefImage> getImages() {
		return m_ReferenceImages;
	}

	// Get bitmap from assets folder
	private Bitmap getBitmapFromAsset(String filePath) {

		InputStream istr = null;
		Bitmap bitmap = null;
		try {
			istr = m_AssetManager.open(filePath);
			bitmap = BitmapFactory.decodeStream(istr);
			istr.close();
			bitmap = Bitmap
					.createScaledBitmap(bitmap, m_width, m_height, false);
		} catch (IOException e) {
			// handle exception
		}

		return bitmap;
	}

}
