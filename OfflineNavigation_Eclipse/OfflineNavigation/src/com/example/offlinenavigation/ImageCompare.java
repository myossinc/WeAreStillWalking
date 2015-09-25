package com.example.offlinenavigation;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import com.example.offlinenavigation.AssetsController.RefImage;

import android.os.AsyncTask;

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

		@Override
		protected RefImage doInBackground(Void... params) {
			
			FeatureDetector detector = FeatureDetector.create(FeatureDetector.SURF);
			DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
			DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
			
			MatOfKeyPoint keypointsCameraImage = new MatOfKeyPoint();
			Mat descriptorsCamImg = new Mat();
					
			detector.detect(m_Image, keypointsCameraImage);
			
			descriptorExtractor.compute(m_Image, keypointsCameraImage, descriptorsCamImg);
			
			for (int i = 0; i < m_Assets.getImages().size(); i++) {
				RefImage curRefImage = m_Assets.getImages().get(i);
				
				MatOfKeyPoint keypointsRefImage = new MatOfKeyPoint();
				
				detector.detect(curRefImage.m_CvImage, keypointsRefImage);
				
				Mat descriptorsRefImg = new Mat();
				
				descriptorExtractor.compute(curRefImage.m_CvImage, keypointsRefImage, descriptorsRefImg);
				
				MatOfDMatch matches = new MatOfDMatch();
				
				descriptorMatcher.match(descriptorsCamImg, descriptorsRefImg, matches);
				
				double max_dist = 0; 
				double min_dist = 100;

				//-- Quick calculation of max and min distances between keypoints
				for( int j = 0; j < descriptorsCamImg.height(); j++ )
				{ 
					double dist = matches.toArray()[j].distance;
					if( dist < min_dist ) min_dist = dist;
					if( dist > max_dist ) max_dist = dist;
				}
				
				//-- Draw only "good" matches (i.e. whose distance is less than 3*min_dist )
				MatOfDMatch good_matches;

				for( int k = 0; k < descriptorsCamImg.height(); k++ )
				{
					if( matches.toArray()[k].distance < 3*min_dist )
					{ 
						//good_matches.push_back(matches.toArray()[k].); 
					}
				}
				
				
				
			}
			return null;
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
			m_StatusListener.onCompareSinglePictureFinished(values[0]);
			super.onProgressUpdate(values);
		}
	}
	
	
}
