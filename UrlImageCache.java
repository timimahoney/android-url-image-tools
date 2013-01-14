/**
 * Copyright (c) 2013, Tim Mahoney (tim.ivan.mahoney@gmail.com)
 * 
 * LICENSE: Do what you want with it.
 */

package com.timahoney.urlimage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import android.graphics.Bitmap;

/**
 * A cache for downloaded images. This is a singleton class, so call
 * {@link #getInstance} to get the instance.
 * <p>
 * You can add images to the cache using {@link #addImage(Bitmap, String)}. You
 * can get a cached image using {@link #getImage(String)}.
 * <p>
 * Note that this will only store images in RAM, not in any persistent storage.
 */
public class UrlImageCache {
	private static final int MAX_LOCAL_PIXEL_COUNT = 5000000;

	private static UrlImageCache sInstance;

	private HashMap<String, Bitmap> mCache;
	private Queue<String> mCacheQueue;

	/**
	 * @return the shared instance of the image cache.
	 */
	public static synchronized UrlImageCache getInstance() {
		if (sInstance == null)
			sInstance = new UrlImageCache();

		return sInstance;
	}

	private UrlImageCache() {
		mCache = new HashMap<String, Bitmap>();
		mCacheQueue = new LinkedList<String>();
	}

	/**
	 * Retrieves an image from the cache.
	 * 
	 * @param url
	 *            the URL of the image to retrieve.
	 * @return the image for the specified URL. Null if not found.
	 */
	public synchronized Bitmap getImage(String url) {
		if (url == null)
			return null;

		return mCache.get(url);
	}

	/**
	 * Adds an image to the cache. You can retrieve this image again by using
	 * {@link #getImage(String)}.
	 * 
	 * @param image
	 *            the image to store in the cache.
	 * @param url
	 *            the URL of the image.
	 */
	public synchronized void addImage(Bitmap image, String url) {
		storeInMemory(image, url);
	}

	/**
	 * Stores an image in the local cache.
	 * 
	 * @param image
	 *            the image to store.
	 * @param url
	 *            the URL of the image.
	 */
	private void storeInMemory(Bitmap image, String url) {
		if (image == null || url == null)
			return;

		// Hold all the local images in a queue.
		// When space is needed, we'll remove the least recently used images.
		// If the cache already contains the URL, then move it to the back of
		// the queue.
		if (!mCache.containsKey(url)) {
			mCache.put(url, image);
			mCacheQueue.offer(url);
		} else {
			mCacheQueue.remove(url);
			mCacheQueue.offer(url);
		}

		// Try to limit the amount of pixels in our cache.
		if (getPixelCount() >= MAX_LOCAL_PIXEL_COUNT)
			freeSomeSpace();
	}

	/**
	 * Frees some space from this image cache. You probably shouldn't need to
	 * use this, but try it if you're having out of memory errors.
	 */
	public void freeSomeSpace() {
		int pixelsToRemove = MAX_LOCAL_PIXEL_COUNT / 2;
		int pixelsRemoved = 0;
		for (int i = 0; (i < mCache.size()) && (pixelsRemoved < pixelsToRemove); i++) {
			String urlToRemove = mCacheQueue.poll();
			Bitmap removed = mCache.remove(urlToRemove);
			pixelsRemoved += (removed.getWidth() * removed.getHeight());
		}

		System.gc();
	}

	/**
	 * @return the number of pixels we have in our local cache.
	 */
	private int getPixelCount() {
		int count = 0;
		for (Bitmap bitmap : mCache.values())
			count += (bitmap.getWidth() * bitmap.getHeight());
		return count;
	}

	/**
	 * Clears the cache of any images. You may want to do a {@link System#gc()}
	 * after calling this.
	 */
	public void clearLocalCache() {
		mCache.clear();
		mCacheQueue.clear();
	}

	/**
	 * @return the amount of images in the local cache.
	 */
	public int size() {
		return mCache.size();
	}
}
