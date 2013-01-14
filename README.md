Android URL Image Tools
=======================

These are some tools you can use in Android to load images from the internet. You get the following classes:

- **`UrlImageView`.** A view that shows an image from a URL.
- **`UrlImageLoader`.** A class that manages loading images from URLs.
- **`UrlImageCache`.** A class that stores images locally.
- **`UrlImageRequest`.** A `Runnable` that loads an image from a URL and manages Android’s pesky `OutOfMemoryError`.

## Using `UrlImageView`
The most common use case is to show a remote image in a view. In this case, you’ll use `UrlImageView`, which will take care of loading the image and displaying it. Here is an example:

	UrlImageView imageView = new UrlImageView(context);
	imageView.setUrl(“http://example.com/image.jpg”);
	layout.addView(imageView);
	
While the image is loading, nothing will be shown. If you want, you can show a placeholder image until the loading is complete:

	imageView.setPlaceholder(R.drawable.placeholder_image);

If you want, you can change the scale of the placeholder and remote images:

	imageView.setPlaceholderScaleType(ScaleType.CENTER);
	imageView.setUrlImageScaleType(ScaleType.CENTER_CROP);
	
If you want to know when the image has been loaded, then you can set a listener on the view:

	imageView.setListener(new UrlImageViewListener() {
		public void onLoadImage(UrlImageView view, boolean didLoad) {
			if (didLoad)
				Log.d(“UrlImageView”, “The image at “ + view.getUrl() + “loaded.”);
		}
	});
	
That’s about it for the `UrlImageView`. It will take care of everything else.

## Using everything else 
The `UrlImageView` utilizes `UrlImageLoader` and `UrlImageCache`. If you want to use these classes on their own, check out the JavaDocs.