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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChipsView<K, V extends ChipsView.DataContract> extends ScrollView implements ChipsEditText.InputConnectionWrapperInterface {

    private static final String TAG = "ChipsView";
    private static final int CHIP_HEIGHT = 24; // dp
    private static final int SPACING_TOP = 4; // dp
    public static final int DEFAULT_VERTICAL_SPACING = 1; // dp
    private static final int DEFAULT_MAX_HEIGHT = -1;

    private int mChipsBgRes = R.drawable.drawable_chip_background;

    private int mMaxHeight; // px
    private int mVerticalSpacing;

    private int mChipsColor;
    private int mChipsColorClicked;
    private int mChipsBgColor;
    private int mChipsBgColorClicked;
    private int mChipsTextColor;
    private int mChipsTextColorClicked;
    private int mChipsPlaceholderResId;
    private int mChipsDeleteResId;
    private int mChipsSearchTextColor;
    private float mChipsSearchTextSize;
    private int mChipLayout;

    private float mDensity;
    private RelativeLayout mChipsContainer;
    private ChipsEditText mEditText;
    private ChipsVerticalLinearLayout mRootChipsLayout;
    private LinkedHashMap<K, Chip<K, V>> mChipList = new LinkedHashMap<>();
    private Object mCurrentEditTextSpan;

    private ChipAddedListener<V> mChipAddedListener;
    private ChipDeletedListener<V> mChipDeletedListener;
    private TextChangedListener<V> mTextChangedListener;

    public ChipsView(Context context) {
        super(context);
        init();
    }

    public ChipsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
        init();
    }

    public ChipsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChipsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttr(context, attrs);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST));
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return true;
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ChipsView, 0, 0);

        try {
            mMaxHeight = a.getDimensionPixelSize(R.styleable.ChipsView_cv_max_height, DEFAULT_MAX_HEIGHT);

            mVerticalSpacing = a.getDimensionPixelSize(R.styleable.ChipsView_cv_vertical_spacing, (int) (DEFAULT_VERTICAL_SPACING * mDensity));

            mChipsColor = a.getColor(R.styleable.ChipsView_cv_color,
                    ContextCompat.getColor(context, android.R.color.darker_gray));

            mChipsColorClicked = a.getColor(R.styleable.ChipsView_cv_color_clicked,
                    ContextCompat.getColor(context, android.R.color.white));

            mChipsBgColor = a.getColor(R.styleable.ChipsView_cv_bg_color,
                    ContextCompat.getColor(context, android.R.color.white));

            mChipsBgColorClicked = a.getColor(R.styleable.ChipsView_cv_bg_color_clicked,
                    ContextCompat.getColor(context, android.R.color.holo_blue_dark));

            mChipsTextColor = a.getColor(R.styleable.ChipsView_cv_text_color,
                    Color.BLACK);

            mChipsTextColorClicked = a.getColor(R.styleable.ChipsView_cv_text_color_clicked,
                    Color.WHITE);

            mChipsPlaceholderResId = a.getResourceId(R.styleable.ChipsView_cv_icon_placeholder,
                    0);

            mChipsDeleteResId = a.getResourceId(R.styleable.ChipsView_cv_icon_delete,
                    R.drawable.drawable_chip_delete);

            mChipsSearchTextColor = a.getColor(R.styleable.ChipsView_cv_search_text_color,
                Color.BLACK);

            mChipsSearchTextSize = a.getDimensionPixelSize(R.styleable.ChipsView_cv_search_text_size, 49);

            mChipLayout = a.getResourceId(R.styleable.ChipsView_cv_chip_layout, R.layout.view_chip_default);

        } finally {
            a.recycle();
        }
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;

        mChipsContainer = new RelativeLayout(getContext());
        addView(mChipsContainer);

        // Dummy item to prevent AutoCompleteTextView from receiving focus
        LinearLayout linearLayout = new LinearLayout(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
        linearLayout.setLayoutParams(params);
        linearLayout.setFocusable(true);
        linearLayout.setFocusableInTouchMode(true);

        mChipsContainer.addView(linearLayout);

        mEditText = new ChipsEditText(getContext(), this);

        final int chipHeightWithPadding = (int) ((CHIP_HEIGHT * mDensity) + mVerticalSpacing);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, chipHeightWithPadding);
        layoutParams.leftMargin = (int) (5 * mDensity);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        mEditText.setLayoutParams(layoutParams);
        mEditText.setPadding(0, 0, 0, mVerticalSpacing);
        mEditText.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        mEditText.setTextColor(mChipsSearchTextColor);
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mChipsSearchTextSize);

        mChipsContainer.addView(mEditText);

        mRootChipsLayout = new ChipsVerticalLinearLayout(getContext(), chipHeightWithPadding);
        mRootChipsLayout.setOrientation(LinearLayout.VERTICAL);
        mRootChipsLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mRootChipsLayout.setPadding(0, (int) (SPACING_TOP * mDensity), 0, 0);
        mChipsContainer.addView(mRootChipsLayout);

        initListener();
        onChipsChanged(false);
    }

    private void initListener() {
        mChipsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.requestFocus();
                ChipsView.this.unselectAllChips();
            }
        });

        mEditText.addTextChangedListener(new EditTextListener());
        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ChipsView.this.unselectAllChips();
                }
            }
        });
    }

    public void addChip(String displayName, Uri avatarUrl, K key, V data) {
        if (mChipList.containsKey(key)) {
            return; //don't add duplicate chips
        }

        addChip(displayName, avatarUrl, key, data, false);
        mEditText.setText("");
        addLeadingMarginSpan();
    }

    public void addChip(String displayName, Uri avatarUrl, K key, V data, boolean isIndelible) {
        Chip<K, V> chip = new Chip<>(displayName, avatarUrl, key, data, isIndelible, new Chip.ChipParams(mChipsBgColorClicked, mDensity, mChipsBgRes, mChipsBgColor, mChipsTextColor, mChipsPlaceholderResId, mChipsDeleteResId, mChipsTextColorClicked, mChipsColorClicked, mChipsColor, CHIP_HEIGHT, mChipLayout), this);
        mChipList.put(key, chip);
        if (mChipAddedListener != null) {
            mChipAddedListener.onChipAdded(chip.getData());
        }

        onChipsChanged(true);
        post(new Runnable() {
            @Override
            public void run() {
                ChipsView.this.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void clear() {
        mChipList.clear();
        onChipsChanged(true);
    }

    public void setChipAddedListener(final ChipAddedListener<V> chipAddedListener) {
        mChipAddedListener = chipAddedListener;
    }

    public void setChipDeletedListener(final ChipDeletedListener<V> chipDeletedListener) {
        mChipDeletedListener = chipDeletedListener;
    }

    public void setTextChangedListener(final TextChangedListener<V> textChangedListener) {
        mTextChangedListener = textChangedListener;
    }
    /**
     * rebuild all chips and place them right
     */
    private void onChipsChanged(final boolean moveCursor) {
        ChipsVerticalLinearLayout.TextLineParams textLineParams = mRootChipsLayout.onChipsChanged(mChipList.values());

        // if null then run another layout pass
        if (textLineParams == null) {
            post(new Runnable() {
                @Override
                public void run() {
                    ChipsView.this.onChipsChanged(moveCursor);
                }
            });
            return;
        }

        addLeadingMarginSpan(textLineParams.lineMargin);
        if (moveCursor) {
            mEditText.setSelection(mEditText.length());
        }
    }

    private void addLeadingMarginSpan(int margin) {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        mCurrentEditTextSpan = new android.text.style.LeadingMarginSpan.LeadingMarginSpan2.Standard(margin, 0);
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mEditText.setText(spannable);
    }

    private void addLeadingMarginSpan() {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mEditText.setText(spannable);
    }

    private void selectOrDeleteLastChip() {
        if (mChipList.size() > 0) {
            try {
                //get last chip
                Iterator<Map.Entry<K, Chip<K, V>>> iter = mChipList.entrySet().iterator();
                Chip<K, V> lastChip = null;
                while (iter.hasNext()) {
                    lastChip = iter.next().getValue();
                }

                if (lastChip != null) {
                    onChipInteraction(lastChip);
                }
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Out of bounds", e);
            }
        }
    }

    public void onChipInteraction(Chip<K, V> chip) {
        unselectChipsExcept(chip);
        if (chip.isSelected()) {
            mChipList.remove(chip.getKey());
            if (mChipDeletedListener != null) {
                mChipDeletedListener.onChipDeleted(chip.getData());
            }
            onChipsChanged(true);
        } else {
            chip.setSelected(true);
            onChipsChanged(false);
        }
    }

    private void unselectChipsExcept(Chip rootChip) {
        for (Chip chip : mChipList.values()) {
            if (chip != rootChip) {
                chip.setSelected(false);
            }
        }
        onChipsChanged(false);
    }

    private void unselectAllChips() {
        unselectChipsExcept(null);
    }

    @Override
    public InputConnection getInputConnection(InputConnection target) {
        return new KeyInterceptingInputConnection(target);
    }

    public void clearEditTextFocus() {
        mEditText.clearFocus();
    }

    public void setText(String text) {
        mEditText.setText(text);
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    public void prune(final Collection<?> pruneData) {
        boolean changed = false;
        Iterator<Map.Entry<K, Chip<K, V>>> iter = mChipList.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<K, Chip<K, V>> entry = iter.next();
            if (!pruneData.contains(entry.getKey())) {
                iter.remove();
                changed = true;
            }
        }

        if (changed) {
            onChipsChanged(true);
        }
    }

    private class EditTextListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (mTextChangedListener != null) {
                mTextChangedListener.onTextChanged(s);
            }
        }
    }

    private class KeyInterceptingInputConnection extends InputConnectionWrapper {

        public KeyInterceptingInputConnection(InputConnection target) {
            super(target, true);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            return super.commitText(text, newCursorPosition);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (mEditText.length() == 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        selectOrDeleteLastChip();
                        return true;
                    }
                }
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
            if (mEditText.length() == 0 && beforeLength == 1 && afterLength == 0) {
                // backspace
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }

    }

    public interface ChipAddedListener <V extends DataContract> {
        void onChipAdded(V data);
    }

    public interface ChipDeletedListener <V extends DataContract> {
        void onChipDeleted(V data);
    }

    public interface TextChangedListener <V extends DataContract> {
        void onTextChanged(CharSequence text);
    }

    public interface DataContract {
        String getDisplayString();
    }
}
