package com.example.offlinenavigation;

import java.util.Vector;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import com.example.offlinenavigation.AssetsController.RefImage;

public class ImageCompare {
	private Mat m_Image;
	private AssetsController m_Assets;

	private int min_dist;

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
			CompareThreadStatusListener s, int min_dist) {
		m_Image = image;
		m_Assets = a;
		m_StatusListener = s;
		this.min_dist = min_dist;
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
			return compare();
		}

		private Mat extractDescription(DescriptorExtractor dex, Mat image,
				MatOfKeyPoint keypoints) {

			Mat descriptors = new Mat();

			dex.compute(image, keypoints, descriptors);

			return descriptors;
		}

		private MatOfKeyPoint detectKeypoints(FeatureDetector detector,
				Mat image) {
			MatOfKeyPoint keypoints = new MatOfKeyPoint();

			detector.detect(image, keypoints);

			return keypoints;
		}

		private MatOfDMatch matchKeypoint(Mat img1, Mat img2) {
			MatOfDMatch matches = new MatOfDMatch();

			descriptorMatcher.match(img1, img2, matches);

			return matches;
		}

		private Mat changeColorChannel(Mat image) {
			Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
			return image;
		}

		private MatOfDMatch extractGoodMatches(DMatch[] matches_arr,
				int min_dist) {
			MatOfDMatch good_matches = new MatOfDMatch();
			Vector<DMatch> good_matches_temp = new Vector<DMatch>();

			int numAllMatches = matches_arr.length;
			for (int k = 0; k < numAllMatches; k++) {
				DMatch object = matches_arr[k];
				if (object.distance <= min_dist) {
					good_matches_temp.add(object);
				}
			}

			good_matches.fromList(good_matches_temp);

			return good_matches;
		}

		private Mat getKeypointMat(MatOfKeyPoint keypointsCameraImage,
				Mat refImageCv, MatOfKeyPoint keypointsRefImage,
				MatOfDMatch good_matches) {
			Mat compareImgMat = new Mat();
			Features2d.drawMatches(m_Image, keypointsCameraImage, refImageCv,
					keypointsRefImage, good_matches, compareImgMat);

			return compareImgMat;
		}

		private RefImage compare() {
			checkDetector();
			checkDescriptor();
			checkMatcher();

			changeColorChannel(m_Image);

			MatOfKeyPoint keypointsCameraImage = detectKeypoints(detector,
					m_Image);

			Mat descriptorsCamImg = extractDescription(descriptorExtractor,
					m_Image, keypointsCameraImage);

			int[] results = new int[m_Assets.getImages().size()];

			for (int i = 0; i < m_Assets.getImages().size(); i++) {
				if (isCancelled())
					return null;

				RefImage curRefImage = m_Assets.getImages().get(i);

				Bitmap refImageBitmap = curRefImage.getImage();

				Mat refImageCv = new Mat();

				Utils.bitmapToMat(refImageBitmap, refImageCv);

				changeColorChannel(refImageCv);

				curRefImage.m_Keypoints =  detectKeypoints(detector, refImageCv);
				curRefImage.m_Descriptors = extractDescription(descriptorExtractor, refImageCv, curRefImage.m_Keypoints);

				MatOfDMatch matches = matchKeypoint(descriptorsCamImg,
						curRefImage.m_Descriptors);

				DMatch[] matches_arr = matches.toArray();

				MatOfDMatch good_matches = extractGoodMatches(matches_arr,
						min_dist);

				curRefImage.m_CompareImage = getKeypointMat(
						keypointsCameraImage, refImageCv,
						curRefImage.m_Keypoints, good_matches);

				results[i] = good_matches.toList().size();

				curRefImage.m_matches = results[i];

				publishProgress(curRefImage);

				refImageBitmap.recycle();
			}

			return getBestFittingImage(results);
		}

		private RefImage getBestFittingImage(int[] results) {
			int maxValue = 0;
			int bestId = -1;
			for (int i = 0; i < results.length; i++) {
				if (results[i] > maxValue) {
					maxValue = results[i];
					bestId = i;
				}
			}

			if (bestId != -1) {
				return m_Assets.getImages().get(bestId);
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
			if (result == null || (int) result.m_matches < 7) {
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
			detector = FeatureDetector.create(FeatureDetector.ORB);
		}
	}

	public void checkMatcher() {
		if (descriptorMatcher == null) {
			descriptorMatcher = DescriptorMatcher
					.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
		}
	}

	public void checkDescriptor() {
		if (descriptorExtractor == null) {
			descriptorExtractor = DescriptorExtractor
					.create(DescriptorExtractor.BRIEF);
		}
	}

}
