package com.example.offlinenavigation;

import java.util.List;
import java.util.Vector;

import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import com.example.offlinenavigation.AssetsController.RefImage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;

public class ImageCompare {
	private Mat m_Image;
	private AssetsController m_Assets;
	
	public interface CompareThreadStatusListener {
		void onComparingStarted();
		void onCompareFinished(RefImage bestFittingImage);
		void onCompareSinglePictureFinished(RefImage image);
	}
	
	private CompareThreadStatusListener m_StatusListener;
	
	public ImageCompare(Mat image, AssetsController a, CompareThreadStatusListener s) {
		m_Image = image;
		m_Assets = a;
		m_StatusListener = s;
	}
	
	public void startComparing() {
		CompareImages thread = new CompareImages();
		thread.execute();
	}
	
	private class CompareImages extends AsyncTask<Void, RefImage, RefImage> {

		public Bitmap toGrayscale(Bitmap bmpOriginal)
	    {        
	        int width, height;
	        height = bmpOriginal.getHeight();
	        width = bmpOriginal.getWidth();    

	        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	        Canvas c = new Canvas(bmpGrayscale);
	        Paint paint = new Paint();
	        ColorMatrix cm = new ColorMatrix();
	        cm.setSaturation(0);
	        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
	        paint.setColorFilter(f);
	        c.drawBitmap(bmpOriginal, 0, 0, paint);
	        return bmpGrayscale;
	    }
		
		@Override
		protected RefImage doInBackground(Void... params) {
			
			FeatureDetector detector = FeatureDetector.create(FeatureDetector.SURF);
			DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
			DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

			MatOfKeyPoint keypointsCameraImage = new MatOfKeyPoint();
			Mat descriptorsCamImg = new Mat();

			detector.detect(m_Image, keypointsCameraImage);
			
			descriptorExtractor.compute(m_Image, keypointsCameraImage, descriptorsCamImg);
			
			publishProgress(null);
			
			int[] results = new int[m_Assets.getImages().size()];
			
			for (int i = 0; i < m_Assets.getImages().size(); i++) {
				RefImage curRefImage = m_Assets.getImages().get(i);
				
				MatOfKeyPoint keypointsRefImage = curRefImage.m_Keypoints;
				
				Bitmap refImageBitmap = curRefImage.getImage();
				
				Mat refImageCv = new Mat();
				
				Utils.bitmapToMat(refImageBitmap, refImageCv);
				
				Imgproc.cvtColor(refImageCv, refImageCv, Imgproc.COLOR_BGR2GRAY);

				if (keypointsRefImage == null) {
					keypointsRefImage = new MatOfKeyPoint();
					detector.detect(refImageCv, keypointsRefImage);
					curRefImage.m_Keypoints = keypointsRefImage;
				}

				
				Mat descriptorsRefImg = curRefImage.m_Descriptors;
				
				if (descriptorsRefImg == null) {
					descriptorsRefImg = new Mat();
					descriptorExtractor.compute(refImageCv, keypointsRefImage, descriptorsRefImg);
					curRefImage.m_Descriptors = descriptorsRefImg;
				}
				
				MatOfDMatch matches = new MatOfDMatch();
				
				descriptorMatcher.match(descriptorsCamImg, descriptorsRefImg, matches);

				double max_dist = 0; 
				double min_dist = 100;

				DMatch[] matches_arr = matches.toArray();
				//-- Quick calculation of max and min distances between keypoints
				for( int j = 0; j < matches_arr.length; j++ )
				{ 
					double dist = matches_arr[j].distance;
					if( dist < min_dist ) min_dist = dist;
					if( dist > max_dist ) max_dist = dist;
				}
				
				//-- Draw only "good" matches (i.e. whose distance is less than 3*min_dist )
				MatOfDMatch good_matches = new MatOfDMatch();
				Vector<DMatch> good_matches_temp = new Vector<DMatch>();

				for( int k = 0; k < matches.toArray().length; k++ )
				{
					DMatch object = matches_arr[k];
					if( object.distance < 4*min_dist )
					{ 
						good_matches_temp.add(object);
					}
				}
				
				good_matches.fromList(good_matches_temp);
				
				Mat compareImgMat = new Mat();
				Features2d.drawMatches(m_Image, keypointsCameraImage, refImageCv, keypointsRefImage, good_matches, compareImgMat);
				curRefImage.m_CompareImage = compareImgMat;
				
				results[i] = good_matches_temp.size();
				
				publishProgress(curRefImage);
				
				refImageBitmap.recycle();
			}
			
			int maxValue = -1;
			int bestId = -1;
			for (int i = 0; i < results.length; i++) {
				if (results[i] > maxValue) {
					maxValue = results[i];
					bestId = i;
				}
			}
			
			RefImage finalimg = m_Assets.getImages().get(bestId);
			
			return finalimg;
		}
		

		@Override
		protected void onPreExecute() {
			m_StatusListener.onComparingStarted();
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(RefImage result) {
			m_StatusListener.onCompareFinished(result);
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(RefImage... values) {
			m_StatusListener.onCompareSinglePictureFinished(values == null ? null : values[0]);
			super.onProgressUpdate(values);
		}
	}
	
	
}
