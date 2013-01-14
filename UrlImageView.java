/**
 * Copyright (c) 2013, Tim Mahoney (tim.ivan.mahoney@gmail.com)
 * 
 * LICENSE: Do what you want with it.
 */

package com.timahoney.urlimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.timahoney.urlimage.UrlImageLoader.UrlImageLoaderDelegate;

/**
 * An image view that loads its image from a URL. You can set the URL for the
 * image to load using {@link #setUrl(String)}. This will take care of loading
 * the image and showing it for you. While the image is loading, you can show a
 * placeholder image by calling {@link #setPlaceholder}. You can be notified
 * when the image has been loaded by using
 * {@link #setListener(UrlImageViewListener)}.
 */
public class UrlImageView extends ImageView implements UrlImageLoaderDelegate {
	private Drawable mPlaceholder;
	private Bitmap mImage;
	private String mUrl;
	private ScaleType mUrlImageScale;
	private ScaleType mPlaceholderScale;
	private UrlImageViewListener mListener;

	/**
	 * An interface for being notified when a {@link UrlImageView} has loaded
	 * its image.
	 */
	public interface UrlImageViewListener {
		/**
		 * Called when the image finishes loading in a {@link UrlImageView}.
		 * 
		 * @param view
		 *            the view that loaded the image.
		 * @param didLoad
		 *            true if the image loaded, or false if something went
		 *            wrong.
		 */
		public void onLoadImage(UrlImageView view, boolean didLoad);
	}

	public UrlImageView(Context context) {
		super(context);
		init();
	}

	public UrlImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public UrlImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		mUrlImageScale = getScaleType();
		mPlaceholderScale = getScaleType();
	}

	/**
	 * Sets the URL for the image to be shown in this view. This will
	 * automatically start downloading the image. While the image downloads, a
	 * placeholder will be shown.
	 * 
	 * @param url
	 *            the URL of the image to show.
	 */
	public synchronized void setUrl(String url) {
		if (url != null && url.equals(mUrl))
			return;

		mUrl = url;
		UrlImageCache cache = UrlImageCache.getInstance();
		mImage = cache.getImage(mUrl);
		if (mImage == null) {
			setImageDrawable(mPlaceholder);
			setScaleType(mPlaceholderScale);
			UrlImageLoader.getInstance().loadImage(url, this);
		} else {
			setScaleType(mUrlImageScale);
			setImageBitmap(mImage);

			if (mListener != null)
				mListener.onLoadImage(this, true);
		}
	}

	/**
	 * @return the URL of the image.
	 */
	public String getUrl() {
		return mUrl;
	}

	/**
	 * Sets the scale type to be used when showing the placeholder image.
	 * 
	 * @param scaleType
	 *            the scale type for the placeholder image.
	 */
	public void setPlaceholderScaleType(ScaleType scaleType) {
		mPlaceholderScale = scaleType;

		// If we're currently showing the placeholder, set the correct scale
		// type.
		if (mPlaceholder != null && mImage == null)
			setScaleType(mPlaceholderScale);
	}

	/**
	 * @param listener
	 *            the object to be notified when the image view finishes
	 *            loading.
	 */
	public void setListener(UrlImageViewListener listener) {
		mListener = listener;
	}

	/**
	 * Sets the scale type to be used when showing the image from the URL. If
	 * the image is already showing, then this will change the scale type to the
	 * supplied value.
	 * 
	 * @param scaleType
	 *            the scale type for the URL loaded image.
	 */
	public void setUrlImageScaleType(ScaleType scaleType) {
		mUrlImageScale = scaleType;

		if (mImage != null)
			setScaleType(mUrlImageScale);
	}

	@Override
	public void onLoadImage(UrlImageLoader loader, String url, Bitmap image) {

		// If we loaded the image, it's the correct URL, then show the new
		// image.
		if (image != null && url.equals(mUrl) && mImage != image) {
			mImage = image;
			setScaleType(mUrlImageScale);
			setImageBitmap(mImage);

			if (mListener != null)
				mListener.onLoadImage(this, true);
		}
	}

	/**
	 * Sets the image to be shown as a placeholder while the other image is
	 * loading.
	 * 
	 * @param bitmap
	 *            the image to be shown while the other image is loading.
	 */
	public void setPlaceholder(Bitmap bitmap) {
		BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
		setPlaceholder(drawable);
	}

	/**
	 * Sets the image to be shown as a placeholder while the other image is
	 * loading.
	 * 
	 * @param drawable
	 *            the image to be shown while the other image is loading.
	 */
	public void setPlaceholder(Drawable drawable) {
		mPlaceholder = drawable;
		if (mImage == null) {
			setScaleType(mPlaceholderScale);
			setImageDrawable(mPlaceholder);
		}
	}

	/**
	 * Sets the image to be shown as a placeholder while the other image is
	 * loading.
	 * 
	 * @param resId
	 *            the resource ID of the image to show while the other image is
	 *            loading.
	 */
	public void setPlaceholder(int resId) {
		Drawable drawable = getContext().getResources().getDrawable(resId);
		setPlaceholder(drawable);
	}

	/**
	 * @return the image loaded for this current URL. This may be null.
	 */
	public Bitmap getUrlImage() {
		return mImage;
	}
}
