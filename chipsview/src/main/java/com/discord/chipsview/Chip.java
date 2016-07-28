/*
 * Copyright (C) 2016 Doodle AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.discord.chipsview;

import android.graphics.PorterDuff;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Chip<K, T extends ChipsView.DataContract> implements View.OnClickListener {

    private static final int MAX_LABEL_LENGTH = 30;
    private final ChipsView<K, T> container;

    private String mLabel;
    private final Uri mPhotoUri;
    private final K key;
    private final T data;
    private final boolean mIsIndelible;

    private RelativeLayout mView;
    private TextView mTextView;

    private ImageView mImageView;

    private boolean mIsSelected = false;

    private ChipParams params;

    public Chip(String label, Uri photoUri, K key, T data, boolean isIndelible, ChipParams params, ChipsView<K, T> container) {
        this.mLabel = label;
        this.mPhotoUri = photoUri;
        this.key = key;
        this.data = data;
        this.mIsIndelible = isIndelible;
        this.params = params;
        this.container = container;

        if (mLabel == null) {
            mLabel = data.getDisplayString();
        }

        if (mLabel.length() > MAX_LABEL_LENGTH) {
            mLabel = mLabel.substring(0, MAX_LABEL_LENGTH) + "...";
        }
    }

    public View getView() {
        if (mView == null) {
            mView = (RelativeLayout) View.inflate(container.getContext(), params.chipLayout, null);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (params.chipHeight * params.density));
            layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin, (int)(4 * params.density), layoutParams.bottomMargin);
            mView.setLayoutParams(layoutParams);
            mImageView = (ImageView) mView.findViewById(R.id.chip_image);
            mTextView = (TextView) mView.findViewById(R.id.chip_text);

            // set initial res & attrs
            mView.setBackgroundResource(params.chipsBgRes);
            mView.post(new Runnable() {
                @Override
                public void run() {
                    mView.getBackground().setColorFilter(params.chipsBgColor, PorterDuff.Mode.SRC_ATOP);
                }
            });

            if (mImageView != null) {
                mImageView.setBackgroundResource(R.drawable.drawable_chip_circle);
                mImageView.setOnClickListener(this);
            }

            mTextView.setTextColor(params.chipsTextColor);
            mView.setOnClickListener(this);
        }
        updateViews();
        return mView;
    }

    private void updateViews() {
        mTextView.setText(mLabel);

        if (mPhotoUri != null && mImageView != null) {
            ImageUtil.setImage(mImageView, mPhotoUri.toString(), mImageView.getResources().getDimensionPixelSize(R.dimen.image_size));
        }

        if (isSelected()) {
            mView.getBackground().setColorFilter(params.chipsBgColorClicked, PorterDuff.Mode.SRC_ATOP);
            mTextView.setTextColor(params.chipsTextColorClicked);

            if (mImageView != null) {
                mImageView.getBackground().setColorFilter(params.chipsColorClicked, PorterDuff.Mode.SRC_ATOP);
                mImageView.setImageResource(params.chipsDeleteResId);
            }
        } else {
            mView.getBackground().setColorFilter(params.chipsBgColor, PorterDuff.Mode.SRC_ATOP);
            mTextView.setTextColor(params.chipsTextColor);

            if (mImageView != null) {
                mImageView.getBackground().setColorFilter(params.chipsColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    @Override
    public void onClick(View v) {
        container.clearEditTextFocus();
        container.onChipInteraction(this);
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean isSelected) {
        if (mIsIndelible) {
            return;
        }
        this.mIsSelected = isSelected;
    }

    public K getKey() {
        return key;
    }

    public T getData() {
        return data;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Chip) {
            Chip other = (Chip) o;
            return data.equals(other.getData());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{"
            + "[Data: " + data + "]"
            + "[Label: " + mLabel + "]"
            + "[PhotoUri: " + mPhotoUri + "]"
            + "[IsIndelible" + mIsIndelible + "]"
            + "}"
            ;
    }

    public static class ChipParams {
        public final int chipsBgColorClicked;
        public final float density;
        public final int chipsBgRes;
        public final int chipsBgColor;
        public final int chipsTextColor;
        public final int chipsPlaceholderResId;
        public final int chipsDeleteResId;
        public final int chipsTextColorClicked;
        public final int chipsColorClicked;
        public final int chipsColor;
        public final int chipHeight;
        public final int chipLayout;

        public ChipParams(int chipsBgColorClicked, float density, int chipsBgRes, int chipsBgColor, int chipsTextColor, int chipsPlaceholderResId, int chipsDeleteResId, int chipsTextColorClicked, int chipsColorClicked, int chipsColor, int chipHeight, int chipLayout) {
            this.chipsBgColorClicked = chipsBgColorClicked;
            this.density = density;
            this.chipsBgRes = chipsBgRes;
            this.chipsBgColor = chipsBgColor;
            this.chipsTextColor = chipsTextColor;
            this.chipsPlaceholderResId = chipsPlaceholderResId;
            this.chipsDeleteResId = chipsDeleteResId;
            this.chipsTextColorClicked = chipsTextColorClicked;
            this.chipsColorClicked = chipsColorClicked;
            this.chipsColor = chipsColor;
            this.chipHeight = chipHeight;
            this.chipLayout = chipLayout;
        }
    }
}
