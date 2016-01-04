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
//return compare();
			return useSURFAlgorithm();
			// return useMSSIMAlgorithm();
		}

		/*
		 * private RefImage useMSSIMAlgorithm() { // Source:
		 * http://docs.opencv.org
		 * /doc/tutorials/highgui/video-input-psnr-ssim/video
		 * -input-psnr-ssim.html#videoinputpsnrmssim
		 * 
		 * final double C1 = 6.5025, C2 = 58.5225;
		 * 
		 * Mat I1 = new Mat();
		 * 
		 * m_Image.convertTo(I1, CvType.CV_32F); // cannot calculate on one byte
		 * large values m_Image.release();
		 * 
		 * publishProgress(null);
		 * 
		 * double[] results = new double[m_Assets.getImages().size()];
		 * 
		 * Log.e("Bla", "1"); for (int i = 0; i < m_Assets.getImages().size();
		 * i++) {
		 * 
		 * RefImage curRefImage = m_Assets.getImages().get(i); Bitmap
		 * refImageBitmap = curRefImage.getImage(); Mat refImageCv = new Mat();
		 * Utils.bitmapToMat(refImageBitmap, refImageCv);
		 * refImageBitmap.recycle();
		 * 
		 * Log.e("Bla", "2"); Mat I2 = new Mat(); refImageCv.convertTo(I2,
		 * CvType.CV_32F); refImageCv.release(); Log.e("Bla", "3"); Mat I2_2 =
		 * I2.mul(I2); // I2^2 Mat I1_2 = I1.mul(I1); // I1^2 Mat I1_I2 =
		 * I1.mul(I2); // I1 * I2
		 * 
		 * /***********************PRELIMINARY COMPUTING
		 * *****************************
		 */

		/*
		 * Mat mu1 = new Mat(); Mat mu2 = new Mat(); Log.e("Bla", "4");
		 * Imgproc.GaussianBlur(I1, mu1, new Size(11, 11), 1.5);
		 * Imgproc.GaussianBlur(I2, mu2, new Size(11, 11), 1.5); Log.e("Bla",
		 * "5"); Mat mu1_2 = mu1.mul(mu1); Mat mu2_2 = mu2.mul(mu2); Mat mu1_mu2
		 * = mu1.mul(mu2);
		 * 
		 * mu1.release(); mu2.release();
		 * 
		 * Mat sigma1_2 = new Mat(); Mat sigma2_2 = new Mat(); Mat sigma12 = new
		 * Mat();
		 * 
		 * Imgproc.GaussianBlur(I1_2, sigma1_2, new Size(11, 11), 1.5);
		 * Core.subtract(sigma1_2, mu1_2, sigma1_2);
		 * 
		 * Imgproc.GaussianBlur(I2_2, sigma2_2, new Size(11, 11), 1.5);
		 * Core.subtract(sigma2_2, mu2_2, sigma2_2);
		 * 
		 * Imgproc.GaussianBlur(I1_I2, sigma12, new Size(11, 11), 1.5);
		 * Core.subtract(sigma12, mu1_mu2, sigma12); Log.e("Bla", "6");
		 * ///////////////////////////////// FORMULA
		 * ////////////////////////////////
		 * 
		 * Mat t1 = new Mat(); Mat t2 = new Mat(); Mat t3 = new Mat();
		 * 
		 * Mat nullmat = Mat.zeros(mu1_mu2.cols(), mu1_mu2.rows(),
		 * CvType.CV_32F); Mat onemat = Mat.ones(mu1_mu2.cols(), mu1_mu2.rows(),
		 * CvType.CV_32F);
		 * 
		 * //t1 = mu1_mu2.mul(onemat, 2.0f); Core.multiply(mu1_mu2, new
		 * Scalar(2), t1); Core.add(t1, new Scalar(C1), t1); //t2 =
		 * sigma12.mul(onemat, 2.0f); Core.multiply(sigma12, new Scalar(2), t2);
		 * Core.add(t2, new Scalar(C2), t2);
		 * 
		 * //Core.gemm(mu1_mu2, onemat, 2.0f, nullmat, C1, t1);
		 * //Core.gemm(sigma12, onemat, 2.0f, nullmat, C2, t1);
		 * 
		 * 
		 * t3 = t1.mul(t2); // t3 = ((2*mu1_mu2 + C1).*(2*sigma12 + C2))
		 * 
		 * Log.e("Bla", "7"); Core.add(mu1_2, mu2_2, t1); Core.add(t1, new
		 * Scalar(C1), t1); Core.add(sigma1_2, sigma2_2, t2); Core.add(t2, new
		 * Scalar(C2), t2);
		 * 
		 * t1 = t1.mul(t2); // t1 =((mu1_2 + mu2_2 + C1).*(sigma1_2 + sigma2_2 +
		 * C2)) Log.e("Bla", "8"); Mat ssim_map = new Mat(); Core.divide(t3, t1,
		 * ssim_map); Log.e("Bla", "9"); Scalar mssim = new Scalar(0, 0, 0);
		 * mssim = Core.mean( ssim_map ); // mssim = average of ssim map
		 * Log.e("Bla", "10"); results[i] = (mssim.val[0] + mssim.val[1] +
		 * mssim.val[2]) / 3.0f; curRefImage.m_Percentage = results[i];
		 * Log.e("Bla", "11"); publishProgress(curRefImage); }
		 * 
		 * return getBestFittingImage(results); }
		 */

		private void extractDecription(DescriptorExtractor dex, Mat image,
				MatOfKeyPoint keypoints, Mat descriptors) {
			dex.compute(image, keypoints, descriptors);
		}

		private RefImage useSURFAlgorithm() {

			// Use SIFT instead of SURF for better results but slower
			// calculation
			checkDetector();
			checkDescriptor();
			checkMatcher();

			MatOfKeyPoint keypointsCameraImage = new MatOfKeyPoint();
			Mat descriptorsCamImg = new Mat();

			Imgproc.cvtColor(m_Image, m_Image, Imgproc.COLOR_BGR2GRAY);

			detector.detect(m_Image, keypointsCameraImage);

			extractDecription(descriptorExtractor, m_Image,
					keypointsCameraImage, descriptorsCamImg);

			// publishProgress(null);

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

		private RefImage compare() {
			// Load images to compare
			Imgproc.cvtColor(m_Image, m_Image, Imgproc.COLOR_BGR2GRAY);

			// Declare key point of images
			MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
			Mat descriptors1 = new Mat();

			// Definition of ORB key point detector and descriptor extractors
			FeatureDetector detector = FeatureDetector
					.create(FeatureDetector.ORB);
			DescriptorExtractor extractor = DescriptorExtractor
					.create(DescriptorExtractor.ORB);

			// Detect key points
			detector.detect(m_Image, keypoints1);

			// Extract descriptors
			extractor.compute(m_Image, keypoints1, descriptors1);

			double[] results = new double[m_Assets.getImages().size()];
			
			for (int i = 0; i < m_Assets.getImages().size(); i++) {
				if (isCancelled())
					return null;

				RefImage curRefImage = m_Assets.getImages().get(i);

				MatOfKeyPoint keypoints2 = curRefImage.m_Keypoints;

				Bitmap refImageBitmap = curRefImage.getImage();

				Mat descriptors2 = new Mat();

				Utils.bitmapToMat(refImageBitmap, descriptors2);

				Imgproc.cvtColor(descriptors2, descriptors2,
						Imgproc.COLOR_BGR2GRAY);

				if (keypoints2 == null) {
					keypoints2 = new MatOfKeyPoint();
					detector.detect(descriptors2, keypoints2);
					curRefImage.m_Keypoints = keypoints2;
				}

				// Definition of descriptor matcher
				DescriptorMatcher matcher = DescriptorMatcher
						.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

				// Match points of two images
				MatOfDMatch matches = new MatOfDMatch();
				// System.out.println("Type of Image1= " + descriptors1.type() +
				// ", Type of Image2= " + descriptors2.type());
				// System.out.println("Cols of Image1= " + descriptors1.cols() +
				// ", Cols of Image2= " + descriptors2.cols());

				// Avoid to assertion failed
				// Assertion failed (type == src2.type() && src1.cols ==
				// src2.cols && (type == CV_32F || type == CV_8U)
				if (descriptors2.cols() == descriptors1.cols()) {
					matcher.match(descriptors1, descriptors2, matches);

					// Check matches of key points
					DMatch[] match = matches.toArray();
					double max_dist = 0;
					double min_dist = 100;

					for (int j = 0; j < descriptors1.rows(); j++) {
						double dist = match[j].distance;
						if (dist < min_dist)
							min_dist = dist;
						if (dist > max_dist)
							max_dist = dist;
					}
					System.out.println("max_dist=" + max_dist + ", min_dist="
							+ min_dist);

					MatOfDMatch good_matches = new MatOfDMatch();
					Vector<DMatch> good_matches_temp = new Vector<DMatch>();

					// Extract good images (distances are under 10)
					for (int k = 0; k < descriptors1.rows(); k++) {
						if (match[i].distance <= 10) {
							good_matches_temp.add(match[i]);
						}
					}

					good_matches.fromList(good_matches_temp);

					Mat compareImgMat = new Mat();
					Features2d.drawMatches(descriptors1, keypoints1,
							descriptors2, keypoints2, good_matches,
							compareImgMat);

					curRefImage.m_CompareImage = compareImgMat;

					results[i] = (double) good_matches_temp.size();
					curRefImage.m_Percentage = results[i];

					publishProgress(curRefImage);

					refImageBitmap.recycle();
				}
			}

			return getBestFittingImage(results);
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
