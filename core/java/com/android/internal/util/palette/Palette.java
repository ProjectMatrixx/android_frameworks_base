package com.android.internal.util.palette;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Palette {
    public static final Filter DEFAULT_FILTER = new Filter() {
        public boolean isAllowed(int i, float[] fArr) {
            return !isWhite(fArr) && !isBlack(fArr) && !isNearRedILine(fArr);
        }

        public final boolean isBlack(float[] fArr) {
            return fArr[2] <= 0.05f;
        }

        public final boolean isWhite(float[] fArr) {
            return fArr[2] >= 0.95f;
        }

        public final boolean isNearRedILine(float[] fArr) {
            float f = fArr[0];
            return f >= 10.0f && f <= 37.0f && fArr[1] <= 0.82f;
        }
    };
    public final Generator mGenerator;
    public final List<Swatch> mSwatches;

    public interface Filter {
        boolean isAllowed(int i, float[] fArr);
    }

    public static abstract class Generator {
        public abstract void generate(List<Swatch> list);

        public abstract Swatch getDarkVibrantSwatch();

        public abstract Swatch getLightVibrantSwatch();
    }

    public interface PaletteAsyncListener {
        void onGenerated(Palette palette);
    }

    public static Builder from(Bitmap bitmap) {
        return new Builder(bitmap);
    }

    @Deprecated
    public static AsyncTask<Bitmap, Void, Palette> generateAsync(Bitmap bitmap, PaletteAsyncListener paletteAsyncListener) {
        return from(bitmap).generate(paletteAsyncListener);
    }

    public Palette(List<Swatch> list, Generator generator) {
        this.mSwatches = list;
        this.mGenerator = generator;
    }

    public Swatch getLightVibrantSwatch() {
        return this.mGenerator.getLightVibrantSwatch();
    }

    public Swatch getDarkVibrantSwatch() {
        return this.mGenerator.getDarkVibrantSwatch();
    }

    public int getLightVibrantColor(int i) {
        Swatch lightVibrantSwatch = getLightVibrantSwatch();
        return lightVibrantSwatch != null ? lightVibrantSwatch.getRgb() : i;
    }

    public int getDarkVibrantColor(int i) {
        Swatch darkVibrantSwatch = getDarkVibrantSwatch();
        return darkVibrantSwatch != null ? darkVibrantSwatch.getRgb() : i;
    }

    public static Bitmap scaleBitmapDown(Bitmap bitmap, int i) {
        int max = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (max <= i) {
            return bitmap;
        }
        float f = ((float) i) / ((float) max);
        return Bitmap.createScaledBitmap(bitmap, Math.round(((float) bitmap.getWidth()) * f), Math.round(((float) bitmap.getHeight()) * f), false);
    }

    public static final class Swatch {
        public final int mBlue;
        public int mBodyTextColor;
        public boolean mGeneratedTextColors;
        public final int mGreen;
        public float[] mHsl;
        public final int mPopulation;
        public final int mRed;
        public final int mRgb;
        public int mTitleTextColor;

        public Swatch(int i, int i2) {
            this.mRed = Color.red(i);
            this.mGreen = Color.green(i);
            this.mBlue = Color.blue(i);
            this.mRgb = i;
            this.mPopulation = i2;
        }

        public int getRgb() {
            return this.mRgb;
        }

        public float[] getHsl() {
            if (this.mHsl == null) {
                float[] fArr = new float[3];
                this.mHsl = fArr;
                ColorUtils.RGBToHSL(this.mRed, this.mGreen, this.mBlue, fArr);
            }
            return this.mHsl;
        }

        public int getPopulation() {
            return this.mPopulation;
        }

        public int getTitleTextColor() {
            ensureTextColorsGenerated();
            return this.mTitleTextColor;
        }

        public int getBodyTextColor() {
            ensureTextColorsGenerated();
            return this.mBodyTextColor;
        }

        public final void ensureTextColorsGenerated() {
            int i;
            int i2;
            if (!this.mGeneratedTextColors) {
                int calculateMinimumAlpha = ColorUtils.calculateMinimumAlpha(-1, this.mRgb, 4.5f);
                int calculateMinimumAlpha2 = ColorUtils.calculateMinimumAlpha(-1, this.mRgb, 3.0f);
                if (calculateMinimumAlpha == -1 || calculateMinimumAlpha2 == -1) {
                    int calculateMinimumAlpha3 = ColorUtils.calculateMinimumAlpha(-16777216, this.mRgb, 4.5f);
                    int calculateMinimumAlpha4 = ColorUtils.calculateMinimumAlpha(-16777216, this.mRgb, 3.0f);
                    if (calculateMinimumAlpha3 == -1 || calculateMinimumAlpha3 == -1) {
                        if (calculateMinimumAlpha != -1) {
                            i = ColorUtils.setAlphaComponent(-1, calculateMinimumAlpha);
                        } else {
                            i = ColorUtils.setAlphaComponent(-16777216, calculateMinimumAlpha3);
                        }
                        this.mBodyTextColor = i;
                        if (calculateMinimumAlpha2 != -1) {
                            i2 = ColorUtils.setAlphaComponent(-1, calculateMinimumAlpha2);
                        } else {
                            i2 = ColorUtils.setAlphaComponent(-16777216, calculateMinimumAlpha4);
                        }
                        this.mTitleTextColor = i2;
                        this.mGeneratedTextColors = true;
                        return;
                    }
                    this.mBodyTextColor = ColorUtils.setAlphaComponent(-16777216, calculateMinimumAlpha3);
                    this.mTitleTextColor = ColorUtils.setAlphaComponent(-16777216, calculateMinimumAlpha4);
                    this.mGeneratedTextColors = true;
                    return;
                }
                this.mBodyTextColor = ColorUtils.setAlphaComponent(-1, calculateMinimumAlpha);
                this.mTitleTextColor = ColorUtils.setAlphaComponent(-1, calculateMinimumAlpha2);
                this.mGeneratedTextColors = true;
            }
        }

        public String toString() {
            return Swatch.class.getSimpleName() + " [RGB: #" + Integer.toHexString(getRgb()) + ']' + " [HSL: " + Arrays.toString(getHsl()) + ']' + " [Population: " + this.mPopulation + ']' + " [Title Text: #" + Integer.toHexString(getTitleTextColor()) + ']' + " [Body Text: #" + Integer.toHexString(getBodyTextColor()) + ']';
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || Swatch.class != obj.getClass()) {
                return false;
            }
            Swatch swatch = (Swatch) obj;
            if (this.mPopulation == swatch.mPopulation && this.mRgb == swatch.mRgb) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (this.mRgb * 31) + this.mPopulation;
        }
    }

    public static final class Builder {
        public Bitmap mBitmap;
        public final List<Filter> mFilters;
        public Generator mGenerator;
        public int mMaxColors;
        public int mResizeMaxDimension;
        public List<Swatch> mSwatches;

        public Builder(Bitmap bitmap) {
            this();
            if (bitmap == null || bitmap.isRecycled()) {
                throw new IllegalArgumentException("Bitmap is not valid");
            }
            this.mBitmap = bitmap;
        }

        public Builder() {
            this.mMaxColors = 16;
            this.mResizeMaxDimension = 192;
            ArrayList arrayList = new ArrayList();
            this.mFilters = arrayList;
            arrayList.add(Palette.DEFAULT_FILTER);
        }

        public Palette generate() {
            List<Swatch> list;
            Bitmap bitmap = this.mBitmap;
            if (bitmap != null) {
                int i = this.mResizeMaxDimension;
                if (i > 0) {
                    Bitmap r0 = Palette.scaleBitmapDown(bitmap, i);
                    int width = r0.getWidth();
                    int height = r0.getHeight();
                    int[] iArr = new int[(width * height)];
                    r0.getPixels(iArr, 0, width, 0, 0, width, height);
                    ColorCutQuantizer colorCutQuantizer = new ColorCutQuantizer(iArr, this.mMaxColors, this.mFilters.isEmpty() ? null : (Filter[]) this.mFilters.toArray(new Filter[0]));
                    if (r0 != this.mBitmap) {
                        r0.recycle();
                    }
                    list = colorCutQuantizer.getQuantizedColors();
                } else {
                    throw new IllegalArgumentException("Minimum dimension size for resizing should should be >= 1");
                }
            } else {
                list = this.mSwatches;
            }
            if (this.mGenerator == null) {
                this.mGenerator = new DefaultGenerator();
            }
            this.mGenerator.generate(list);
            return new Palette(list, this.mGenerator);
        }

        public AsyncTask<Bitmap, Void, Palette> generate(final PaletteAsyncListener paletteAsyncListener) {
            if (paletteAsyncListener != null) {
                AsyncTask<Bitmap, Void, Palette> asyncTask = new AsyncTask<Bitmap, Void, Palette>() {
                    @Override
                    protected Palette doInBackground(Bitmap... bitmapArr) {
                        return Builder.this.generate();
                    }

                    public void onPostExecute(Palette palette) {
                        paletteAsyncListener.onGenerated(palette);
                    }
                };
                asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Bitmap[]{this.mBitmap});
                return asyncTask;
            }
            throw new IllegalArgumentException("listener can not be null");
        }
    }
}
