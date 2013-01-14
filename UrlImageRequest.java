/**
 * Copyright (c) 2013, Tim Mahoney (tim.ivan.mahoney@gmail.com)
 * 
 * LICENSE: Do what you want with it.
 */

package com.timahoney.urlimage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * A class that loads images from a URL.
 * <p>
 * Since this class implements {@link Runnable}, you can easily execute it in a
 * new thread or executor. You can do this yourself, but it is easier to use the
 * {@link UrlImageLoader} class, which will manage requests for you.
 * <p>
 * If you're running these requests in new threads, you may want to be notified
 * when a request has finished. To do so, use
 * {@link #setDelegate(UrlImageRequestDelegate)} or the constructor that takes a
 * {@link UrlImageRequestDelegate}.
 * <p>
 * This class will check the UrlImageCache to see if the image has already been
 * loaded and cached.
 */
public class UrlImageRequest implements Runnable {
	private static final String DEBUG_TAG = "UrlImageRequest";

	/**
	 * A delegate to be notified when the image request has completed.
	 */
	public interface UrlImageRequestDelegate {
		/**
		 * This is called when an image request has loaded an image. If the
		 * request was successful, then {@link UrlImageRequest#getBitmap()} will
		 * return the image that was loaded.
		 * 
		 * @param request
		 *            the request that has completed.
		 */
		public void onLoadImage(UrlImageRequest request);
	}

	private Bitmap mBitmap;
	private String mUrlString;
	private URL mUrl;
	private UrlImageRequestDelegate mDelegate;

	/**
	 * Creates a new request that will load an image from a URL.
	 * 
	 * @param url
	 *            the URL to load the image from. May not be null.
	 * @param delegate
	 *            the delegate to be notified when the request has completed.
	 *            May be null.
	 * @throws MalformedURLException
	 *             thrown when the URL provided is invalid.
	 */
	public UrlImageRequest(String url, UrlImageRequestDelegate delegate) {

		if (url == null)
			throw new RuntimeException("You must provide a URL when creating a UrlImageRequest.");

		mUrlString = url;
		try {
			mUrl = new URL(url);
		} catch (MalformedURLException e) {
			Log.e(DEBUG_TAG, "Invalid URL for UrlImageRequest: " + url);
			e.printStackTrace();
		}
		setDelegate(mDelegate);
	}

	/**
	 * Creates a new request that will load an image from a URL.
	 * 
	 * @param url
	 *            the URL to load the image from. May not be null.
	 * @throws MalformedURLException
	 *             thrown when the URL provided is invalid.
	 */
	public UrlImageRequest(String url) {
		this(url, null);
	}

	/**
	 * Decodes a bitmap stream into a bitmap.
	 * 
	 * @param bitmapStream
	 *            the stream that holds bitmap data.
	 * @return the decoded stream.
	 */
	private static synchronized Bitmap decodeStream(InputStream bitmapStream)
			throws OutOfMemoryError {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPurgeable = true;
		opts.inInputShareable = true;

		// IF we get an out of memory error in decoding the stream, try again
		// after clearing some memory.
		Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream, null, opts);
		if (bitmap != null)
			bitmap.prepareToDraw();
		return bitmap;
	}

	@Override
	public void run() {
		if (mUrl == null || mUrl.equals(""))
			return;

		// Check the cache for this bitmap.
		// If there is nothing, then check the network.
		mBitmap = UrlImageCache.getInstance().getImage(mUrlString);

		if (mBitmap == null) {
			mBitmap = requestImageWithRetries(mUrl);

			if (mBitmap != null)
				UrlImageCache.getInstance().addImage(mBitmap, mUrlString);
		}

		if (mDelegate != null) {
			
			// Notify the delegate on the main thread.
			Handler mainHandler = new Handler(Looper.getMainLooper());
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					mDelegate.onLoadImage(UrlImageRequest.this);
				}
			});
		}
	}

	/**
	 * Requests an image and retries once if we run out of memory.
	 * 
	 * @param url
	 *            the URL to request the image from.
	 * @return the image that was downloaded
	 */
	private static Bitmap requestImageWithRetries(URL url) {

		// This is a little tricky due to some memory problems on Android.
		// If we get an OutOfMemoryError, free up some space and try again.
		try {
			return requestImage(url);
		} catch (OutOfMemoryError e) {

			Log.w(DEBUG_TAG, "Out of memory when downloading image. Freeing space");
			UrlImageCache.getInstance().freeSomeSpace();

			try {
				return requestImage(url);
			} catch (OutOfMemoryError e2) {
				Log.e(DEBUG_TAG,
						"Out of memory after retrying. Did not load image at " + url.toString());
				return null;
			}
		}
	}

	/**
	 * Requests an image from a URL.
	 * 
	 * @param url
	 *            the URL to request the image from.
	 * @return the image that was downloaded. Null if there was a problem.
	 * @throws OutOfMemoryError
	 *             This will be thrown if the bitmap decoder runs out of memory.
	 *             Try to free up some space and try again.
	 */
	private static Bitmap requestImage(URL url) throws OutOfMemoryError {
		try {
			URLConnection connection = url.openConnection();
			connection.connect();
			Bitmap bitmap = decodeStream(connection.getInputStream());
			return bitmap;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @return the bitmap loaded by this request, null if nothing was received.
	 */
	public Bitmap getBitmap() {
		return mBitmap;
	}

	public String getUrl() {
		return mUrlString;
	}

	/**
	 * Sets the delegate to be notified when this request is complete.
	 * 
	 * @param delegate
	 *            the object to be notified when this image request is complete.
	 */
	public void setDelegate(UrlImageRequestDelegate delegate) {
		mDelegate = delegate;
	}
}