package com.example.offlinenavigation;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.example.offlinenavigation.AssetsController.RefImage;

public class ImageCompare {
	private Mat m_Image;
	private AssetsController m_Assets;

	private static FeatureDetector detector;
	private static DescriptorExtractor descriptorExtractor;
	private static DescriptorMatcher descriptorMatcher;

	public interface CompareThreadStatusListener {
		void onComparingStarted();

		void onCompareFinished(RefImage bestFittingImage);

		void onCompareSinglePictureFinished(RefImage image);
	}

	private CompareThreadStatusListener m_StatusListener;

	public ImageCompare(Mat image, AssetsController a,
			CompareThreadStatusListener s) {
		m_Image = image;
		m_Assets = a;
		m_StatusListener = s;
	}

	public CompareImages startComparing() {
		CompareImages thread = new CompareImages();
		thread.execute();
		return thread;
	}

	public static void setFeatureDetector(int featureDetector) {
		detector = FeatureDetector.create(featureDetector);
	}

	public static void setDescriptorExtractor(int descriptor) {
		descriptorExtractor = DescriptorExtractor.create(descriptor);
	}

	public static void setDescriptorMatcher(int matcher) {
		descriptorMatcher = DescriptorMatcher.create(matcher);
	}

	public class CompareImages extends AsyncTask<Void, RefImage, RefImage> {

		@Override
		protected RefImage doInBackground(Void... params) {
			return useSURFAlgorithm();
		}
		
		private void extractDecription(DescriptorExtractor dex, Mat image,
				MatOfKeyPoint keypoints, Mat descriptors) {
			dex.compute(image, keypoints, descriptors);
		}
		
		private MatOfKeyPoint detectKeypoints(FeatureDetector detector, Mat image){
			MatOfKeyPoint keypoints = new MatOfKeyPoint();
			detector.detect(image, keypoints);
			
			return keypoints;
		}
		
		private MatOfDMatch matchKeypoint(MatOfKeyPoint img1, MatOfKeyPoint img2, MatOfDMatch matches){
			descriptorMatcher.match(img1, img2, matches);
			return matches;
		}
		
		private Mat changeColorChannel(Mat image){
			Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
			return image;
		}
		
		private MatOfDMatch extractGoodMatches(MatOfDMatch matches, DMatch[] matches_arr, int min_dist){
			MatOfDMatch good_matches = new MatOfDMatch();
			Vector<DMatch> good_matches_temp = new Vector<DMatch>();

			int numAllMatches = matches.toArray().length;
			for (int k = 0; k < numAllMatches; k++) {
				DMatch object = matches_arr[k];
				if (object.distance <= Math.max(1.3 * min_dist, 0.02)) {
					good_matches_temp.add(object);
				}
			}

			good_matches.fromList(good_matches_temp);
			return null;
		}
		
		private void drawGoodMatches(){
			
		}

		private RefImage useSURFAlgorithm() {
			checkDetector();
			checkDescriptor();
			checkMatcher();

			MatOfKeyPoint keypointsCameraImage = new MatOfKeyPoint();
			Mat descriptorsCamImg = new Mat();

			Imgproc.cvtColor(m_Image, m_Image, Imgproc.COLOR_BGR2GRAY);

			detector.detect(m_Image, keypointsCameraImage);

			extractDecription(descriptorExtractor, m_Image,
					keypointsCameraImage, descriptorsCamImg);

			double[] results = new double[m_Assets.getImages().size()];

			for (int i = 0; i < m_Assets.getImages().size(); i++) {
				if (isCancelled())
					return null;

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
					extractDecription(descriptorExtractor, refImageCv,
							keypointsRefImage, descriptorsRefImg);
					curRefImage.m_Descriptors = descriptorsRefImg;
				}

				MatOfDMatch matches = new MatOfDMatch();

				descriptorMatcher.match(descriptorsCamImg, descriptorsRefImg,
						matches);

				double max_dist = 0;
				double min_dist = 100;

				DMatch[] matches_arr = matches.toArray();

				// -- Quick calculation of max and min distances between
				// keypoints
				for (int j = 0; j < matches_arr.length; j++) {
					double dist = matches_arr[j].distance;
					if (dist < min_dist)
						min_dist = dist;
					if (dist > max_dist)
						max_dist = dist;
				}

				// -- Draw only "good" matches
				MatOfDMatch good_matches = new MatOfDMatch();
				Vector<DMatch> good_matches_temp = new Vector<DMatch>();

				int numAllMatches = matches.toArray().length;
				for (int k = 0; k < numAllMatches; k++) {
					DMatch object = matches_arr[k];
					if (object.distance <= Math.max(1.3 * min_dist, 0.02)) {
						good_matches_temp.add(object);
					}
				}

				good_matches.fromList(good_matches_temp);

				Mat compareImgMat = new Mat();
				Features2d.drawMatches(m_Image, keypointsCameraImage,
						refImageCv, keypointsRefImage, good_matches,
						compareImgMat);

				curRefImage.m_CompareImage = compareImgMat;

				results[i] = (double) good_matches_temp.size();
				curRefImage.m_Percentage = results[i];

				publishProgress(curRefImage);

				refImageBitmap.recycle();
			}

			return getBestFittingImage(results);
		}
		
		void compare(){
		    FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
		    DescriptorExtractor descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
		    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);

		    //set up img1 (scene)
		    Mat descriptors1 = new Mat();
		    MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
		    //calculate descriptor for img1
		    detector.detect(m_Image, keypoints1);
		    descriptor.compute(m_Image, keypoints1, descriptors1);

		    for (int i = 0; i < m_Assets.getImages().size(); i++) {
				if (isCancelled())
					return;
		    
		    //set up img2 (template)
		    Mat descriptors2 = new Mat();
		    MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
		    //calculate descriptor for img2
		   
//		    RefImage curRefImage = m_Assets.getImages().get(i);
//
//			Bitmap refImageBitmap = curRefImage.getImage();

			Mat refImageCv = m_Image.clone();
		    
//		    Utils.bitmapToMat(refImageBitmap, refImageCv);
		    
		    detector.detect(refImageCv, keypoints2);
		    descriptor.compute(refImageCv, keypoints2, descriptors2);

		    //match 2 images' descriptors
		    MatOfDMatch matches = new MatOfDMatch();
		    matcher.match(descriptors1, descriptors2,matches);

		    //calculate max and min distances between keypoints
		    double max_dist=0;double min_dist=99;

		    List<DMatch> matchesList = matches.toList();
		    for(int j=0;j<descriptors1.rows();j++)
		    {
		        double dist = matchesList.get(j).distance;
		        if (dist<min_dist) min_dist = dist;
		        if (dist>max_dist) max_dist = dist;
		    }

		    //set up good matches, add matches if close enough
		    LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
		    MatOfDMatch gm = new MatOfDMatch();
		    for (int k=0;k<descriptors2.rows();k++)
		    {
		        if(matchesList.get(k).distance<3*min_dist)
		        {
		            good_matches.addLast(matchesList.get(k));
		        }
		    }
		    gm.fromList(good_matches);

		    //put keypoints mats into lists
		    List<KeyPoint> keypoints1_List = keypoints1.toList();
		    List<KeyPoint> keypoints2_List = keypoints2.toList();

		    //put keypoints into point2f mats so calib3d can use them to find homography
		    LinkedList<Point> objList = new LinkedList<Point>();
		    LinkedList<Point> sceneList = new LinkedList<Point>();
		    for(int l=0;l<good_matches.size();l++)
		    {
		        objList.addLast(keypoints2_List.get(good_matches.get(l).queryIdx).pt);
		        sceneList.addLast(keypoints1_List.get(good_matches.get(l).trainIdx).pt);
		    }
		    MatOfPoint2f obj = new MatOfPoint2f();
		    MatOfPoint2f scene = new MatOfPoint2f();
		    obj.fromList(objList);
		    scene.fromList(sceneList);

		    //output image
		    Mat outputImg = new Mat();
		    MatOfByte drawnMatches = new MatOfByte();
		    Features2d.drawMatches(m_Image, keypoints1, refImageCv, keypoints2, gm, outputImg, Scalar.all(-1), Scalar.all(-1), drawnMatches,Features2d.NOT_DRAW_SINGLE_POINTS);

		    //run homography on object and scene points
		    Mat H = Calib3d.findHomography(obj, scene,Calib3d.RANSAC, 5);
//		    Mat tmp_corners = new Mat(4,1,CvType.CV_32FC2);
//		    Mat scene_corners = new Mat(4,1,CvType.CV_32FC2);
//
//		    //get corners from object
//		    tmp_corners.put(0, 0, new double[] {0,0});
//		    tmp_corners.put(1, 0, new double[] {refImageCv.cols(),0});
//		    tmp_corners.put(2, 0, new double[] {refImageCv.cols(),refImageCv.rows()});
//		    tmp_corners.put(3, 0, new double[] {0,refImageCv.rows()});
//
//		    Core.perspectiveTransform(tmp_corners,scene_corners, H);
//
//
//		    Core.line(outputImg, new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)), new Scalar(0, 255, 0),4);
//		    Core.line(outputImg, new Point(scene_corners.get(1,0)), new Point(scene_corners.get(2,0)), new Scalar(0, 255, 0),4);
//		    Core.line(outputImg, new Point(scene_corners.get(2,0)), new Point(scene_corners.get(3,0)), new Scalar(0, 255, 0),4);
//		    Core.line(outputImg, new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)), new Scalar(0, 255, 0),4);
		    }
		}

		private RefImage getBestFittingImage(double[] results) {
			double maxValue = 0;
			int bestId = -1;
			for (int i = 0; i < results.length; i++) {
				if (results[i] > maxValue) {
					maxValue = results[i];
					bestId = i;
				}
			}

			if (bestId != -1) {
				RefImage finalimg = m_Assets.getImages().get(bestId);
				return finalimg;
			} else {
				return null;
			}
		}

		@Override
		protected void onPreExecute() {
			m_StatusListener.onComparingStarted();
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(RefImage result) {
			if (result == null || (int) result.m_Percentage < 10) {
				result = null;
			}
			m_StatusListener.onCompareFinished(result);
			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(RefImage... values) {
			m_StatusListener
					.onCompareSinglePictureFinished(values == null ? null
							: values[0]);
			super.onProgressUpdate(values);
		}
	}

	public void checkDetector() {
		if (detector == null) {
			detector = FeatureDetector.create(FeatureDetector.SURF);
		}
	}

	public void checkMatcher() {
		if (descriptorMatcher == null) {
			descriptorMatcher = DescriptorMatcher
					.create(DescriptorMatcher.FLANNBASED);
		}
	}

	public void checkDescriptor() {
		if (descriptorExtractor == null) {
			descriptorExtractor = DescriptorExtractor
					.create(DescriptorExtractor.SURF);
		}
	}

}
