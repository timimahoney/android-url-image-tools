/**
 * Copyright (c) 2013, Tim Mahoney (tim.ivan.mahoney@gmail.com)
 * 
 * LICENSE: Do what you want with it.
 */

package com.timahoney.urlimage;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;

import com.timahoney.urlimage.UrlImageRequest.UrlImageRequestDelegate;

/**
 * A class that loads images from URLs. This is a singleton class, so call
 * {@link #getInstance} to get the instance.
 * 
 * To load an image, call any of the variants of {@link #loadImage}.
 * 
 * Any images loaded by this class will be cached in the {@link UrlImageCache}.
 * If you try to load an image that has already been cached, then the cached
 * image will be returned.
 */
public class UrlImageLoader implements UrlImageRequestDelegate {

	/**
	 * A delegate to be notified when the image loader has loaded an image.
	 */
	public interface UrlImageLoaderDelegate {
		/**
		 * Called when a request for an image has completed.
		 * 
		 * @param loader
		 *            the loader that loaded the image.
		 * @param url
		 *            the URL of the image that was loaded.
		 * @param image
		 *            the image that was loaded, or null if there was a problem.
		 */
		public void onLoadImage(UrlImageLoader loader, String url, Bitmap image);
	}

	/**
	 * The priority with which to load an image. A higher priority means that
	 * the image will be loaded first.
	 */
	public enum UrlImageLoaderPriority {
		/**
		 * The lowest priority for loading an image. Images with this priority
		 * will be loaded last.
		 */
		VERY_LOW,

		/**
		 * A low priority for loading images.
		 */
		LOW,

		/**
		 * The default priority for loading images.
		 */
		MEDIUM,

		/**
		 * A high priority for loading images.
		 */
		HIGH,

		/**
		 * The highest priority for loading images. Images with this priority
		 * will be loaded first.
		 */
		VERY_HIGH
	}

	private static UrlImageLoader sInstance;

	private Map<String, UrlImageRequest> mRequests;
	private ThreadPoolExecutor mExecutor;
	private BlockingQueue<Runnable> mQueue;
	private Map<Runnable, UrlImageLoaderPriority> mPriorities;

	private Map<String, Set<UrlImageLoaderDelegate>> mUrlDelegates;

	/**
	 * @return the singleton instance of this class.
	 */
	public static synchronized UrlImageLoader getInstance() {
		if (sInstance == null)
			sInstance = new UrlImageLoader();

		return sInstance;
	}

	/**
	 * Private constructor to initialize an image loader.
	 */
	private UrlImageLoader() {
		mRequests = new HashMap<String, UrlImageRequest>();
		mPriorities = new HashMap<Runnable, UrlImageLoaderPriority>();

		mQueue = new PriorityBlockingQueue<Runnable>(100, new Comparator<Runnable>() {
			@Override
			public int compare(Runnable lhs, Runnable rhs) {
				UrlImageLoaderPriority right = mPriorities.get(rhs);
				UrlImageLoaderPriority left = mPriorities.get(lhs);
				if (right == null)
					return -1;
				else if (left == null)
					return 1;
				else
					return right.ordinal() - left.ordinal();
			}
		});
		mExecutor = new ThreadPoolExecutor(3, 5, 15, TimeUnit.SECONDS, mQueue);

		mUrlDelegates = new HashMap<String, Set<UrlImageLoaderDelegate>>();
	}

	/**
	 * Loads an image from a URL.
	 * 
	 * @param url
	 *            the URL to load the image from. May not be null.
	 * @param delegate
	 *            the delegate to be notified when the request to load this
	 *            image is complete. May be null.
	 * @param priority
	 *            the priority by which to load this image. If null, then the
	 *            default priority of {@link UrlImageLoaderPriority#MEDIUM} will
	 *            be used.
	 */
	public synchronized void loadImage(String url, UrlImageLoaderDelegate delegate,
			UrlImageLoaderPriority priority) {

		if (url == null)
			return;

		// Use a default priority.
		if (priority == null)
			priority = UrlImageLoaderPriority.MEDIUM;

		// Load the image from this URL.
		// Check if we already have a request for the URL.
		UrlImageRequest request = mRequests.get(url);

		if (request == null) {

			// We aren't already requesting the URL.
			// Create a new request and put it in the queue.
			request = new UrlImageRequest(url);
			request.setDelegate(this);
			Set<UrlImageLoaderDelegate> delegates = new HashSet<UrlImageLoaderDelegate>();
			mUrlDelegates.put(url, delegates);
			if (delegate != null)
				delegates.add(delegate);
			mRequests.put(url, request);
			mPriorities.put(request, priority);
			mExecutor.execute(request);

		} else if (request.getBitmap() == null) {

			// We already have a request, but it hasn't finished yet.
			// If this priority is higher than the previous priority, then
			// change the priority of the old request.
			// Also, add the new delegate for this request.
			int newPriorityOrdinal = Math.max(priority.ordinal(), mPriorities.get(request)
					.ordinal());
			UrlImageLoaderPriority newPriority = UrlImageLoaderPriority.values()[newPriorityOrdinal];
			mPriorities.put(request, newPriority);
			if (delegate != null)
				mUrlDelegates.get(url).add(delegate);

		} else if (delegate != null) {

			// We already requested this image and it has already completed.
			// Notify the delegate that the request is complete.
			delegate.onLoadImage(this, url, request.getBitmap());
		}
	}

	/**
	 * Loads an image from a URL with a default priority of
	 * {@link UrlImageLoaderPriority#MEDIUM}.
	 * 
	 * @param url
	 *            the URL to load the image from. May not be null.
	 * @param delegate
	 *            the delegate to be notified when the request to load this
	 *            image is complete. May be null.
	 */
	public void loadImage(String url, UrlImageLoaderDelegate delegate) {
		loadImage(url, delegate, null);
	}

	@Override
	public void onLoadImage(UrlImageRequest request) {

		// The request finished. Notify all the delegates.
		mRequests.remove(request.getUrl());
		mPriorities.remove(request);
		Set<UrlImageLoaderDelegate> delegates = mUrlDelegates.remove(request.getUrl());

		if (delegates != null) {
			for (UrlImageLoaderDelegate delegate : delegates)
				delegate.onLoadImage(this, request.getUrl(), request.getBitmap());
		}
	}
}
