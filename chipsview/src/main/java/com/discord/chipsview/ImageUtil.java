package com.discord.chipsview;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.view.DraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

public class ImageUtil {

    private static final int SMALL_IMAGE_MAX_SIZE = 200;

    public static void setImage(ImageView view, @Nullable String url, int widthAndHeight) {
        setImage(view, url, widthAndHeight, widthAndHeight);
    }

    public static void setImage(ImageView view, @Nullable String url, int width, int height) {

        DraweeView draweeView = (DraweeView) view;

        if (url == null) {
            draweeView.setController(null);
            return;
        }

        // Create URI.
        Uri uri = Uri.parse(url);

        // Create an image controller builder.
        PipelineDraweeControllerBuilder builder = Fresco.newDraweeControllerBuilder();

        // Provide some s5andard config.
        builder = builder.setOldController(draweeView.getController()).setUri(uri).setAutoPlayAnimations(true);

        // Get image request.
        ImageRequestBuilder request = getImageRequest(url, width, height);

        // Generate the final controller with image request.
        ((DraweeView) view).setController(builder.setImageRequest(request.build()).build());
    }

    /**
     * Gets an image request with some commonly used parameters
     * such as the desired width and height.
     */
    static ImageRequestBuilder getImageRequest(String url, int width, int height) {

        // Create a resize image request, allow full cache lookups.
        ImageRequestBuilder request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH);

        boolean smallImage = !url.contains("gif") && width <= SMALL_IMAGE_MAX_SIZE && height <= SMALL_IMAGE_MAX_SIZE;

        // Use a smaller cache for everything else.
        request = request.setCacheChoice(smallImage ? ImageRequest.CacheChoice.SMALL : ImageRequest.CacheChoice.DEFAULT);

        if (width > 0 && height > 0) {

            request = request.setResizeOptions(new ResizeOptions(width, height));
        }

        return request;
    }
}
