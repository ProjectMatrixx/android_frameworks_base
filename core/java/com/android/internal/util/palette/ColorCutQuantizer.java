package com.android.internal.util.palette;

import android.graphics.Color;
import android.util.TimingLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import com.android.internal.util.palette.Palette;

public final class ColorCutQuantizer {
    public static final Comparator<Vbox> VBOX_COMPARATOR_VOLUME = new Comparator<Vbox>() {
        public int compare(Vbox vbox, Vbox vbox2) {
            return vbox2.getVolume() - vbox.getVolume();
        }
    };
    public final int[] mColors;
    public final Palette.Filter[] mFilters;
    public final int[] mHistogram;
    public final List<Palette.Swatch> mQuantizedColors;
    public final float[] mTempHsl = new float[3];
    public final TimingLogger mTimingLogger = null;

    public static int modifyWordWidth(int i, int i2, int i3) {
        return (i3 > i2 ? i << (i3 - i2) : i >> (i2 - i3)) & ((1 << i3) - 1);
    }

    public static int quantizedBlue(int i) {
        return i & 31;
    }

    public static int quantizedGreen(int i) {
        return (i >> 5) & 31;
    }

    public static int quantizedRed(int i) {
        return (i >> 10) & 31;
    }

    public ColorCutQuantizer(int[] iArr, int i, Palette.Filter[] filterArr) {
        this.mFilters = filterArr;
        int[] iArr2 = new int[32768];
        this.mHistogram = iArr2;
        for (int i2 = 0; i2 < iArr.length; i2++) {
            int quantizeFromRgb888 = quantizeFromRgb888(iArr[i2]);
            iArr[i2] = quantizeFromRgb888;
            iArr2[quantizeFromRgb888] = iArr2[quantizeFromRgb888] + 1;
        }
        int i3 = 0;
        for (int i4 = 0; i4 < 32768; i4++) {
            if (iArr2[i4] > 0 && shouldIgnoreColor(i4)) {
                iArr2[i4] = 0;
            }
            if (iArr2[i4] > 0) {
                i3++;
            }
        }
        int[] iArr3 = new int[i3];
        this.mColors = iArr3;
        int i5 = 0;
        for (int i6 = 0; i6 < 32768; i6++) {
            if (iArr2[i6] > 0) {
                iArr3[i5] = i6;
                i5++;
            }
        }
        if (i3 <= i) {
            this.mQuantizedColors = new ArrayList();
            for (int i7 = 0; i7 < i3; i7++) {
                int i8 = iArr3[i7];
                this.mQuantizedColors.add(new Palette.Swatch(approximateToRgb888(i8), iArr2[i8]));
            }
            return;
        }
        this.mQuantizedColors = quantizePixels(i);
    }

    public List<Palette.Swatch> getQuantizedColors() {
        return this.mQuantizedColors;
    }

    public final List<Palette.Swatch> quantizePixels(int i) {
        PriorityQueue priorityQueue = new PriorityQueue(i, VBOX_COMPARATOR_VOLUME);
        priorityQueue.offer(new Vbox(0, this.mColors.length - 1));
        splitBoxes(priorityQueue, i);
        return generateAverageColors(priorityQueue);
    }

    public final void splitBoxes(PriorityQueue<Vbox> priorityQueue, int i) {
        Vbox poll;
        while (priorityQueue.size() < i && (poll = priorityQueue.poll()) != null && poll.canSplit()) {
            priorityQueue.offer(poll.splitBox());
            priorityQueue.offer(poll);
        }
    }

    public final List<Palette.Swatch> generateAverageColors(Collection<Vbox> collection) {
        ArrayList arrayList = new ArrayList(collection.size());
        for (Vbox averageColor : collection) {
            Palette.Swatch averageColor2 = averageColor.getAverageColor();
            if (!shouldIgnoreColor(averageColor2)) {
                arrayList.add(averageColor2);
            }
        }
        return arrayList;
    }

    public class Vbox {
        public int mLowerIndex;
        public int mMaxBlue;
        public int mMaxGreen;
        public int mMaxRed;
        public int mMinBlue;
        public int mMinGreen;
        public int mMinRed;
        public int mPopulation;
        public int mUpperIndex;

        public Vbox(int i, int i2) {
            this.mLowerIndex = i;
            this.mUpperIndex = i2;
            fitBox();
        }

        public final int getVolume() {
            return ((this.mMaxRed - this.mMinRed) + 1) * ((this.mMaxGreen - this.mMinGreen) + 1) * ((this.mMaxBlue - this.mMinBlue) + 1);
        }

        public final boolean canSplit() {
            return getColorCount() > 1;
        }

        public final int getColorCount() {
            return (this.mUpperIndex + 1) - this.mLowerIndex;
        }

        public final void fitBox() {
            ColorCutQuantizer colorCutQuantizer = ColorCutQuantizer.this;
            int[] iArr = colorCutQuantizer.mColors;
            int[] iArr2 = colorCutQuantizer.mHistogram;
            int i = Integer.MAX_VALUE;
            int i2 = Integer.MIN_VALUE;
            int i3 = Integer.MIN_VALUE;
            int i4 = Integer.MIN_VALUE;
            int i5 = 0;
            int i6 = Integer.MAX_VALUE;
            int i7 = Integer.MAX_VALUE;
            for (int i8 = this.mLowerIndex; i8 <= this.mUpperIndex; i8++) {
                int i9 = iArr[i8];
                i5 += iArr2[i9];
                int r11 = ColorCutQuantizer.quantizedRed(i9);
                int r12 = ColorCutQuantizer.quantizedGreen(i9);
                int r10 = ColorCutQuantizer.quantizedBlue(i9);
                if (r11 > i2) {
                    i2 = r11;
                }
                if (r11 < i) {
                    i = r11;
                }
                if (r12 > i3) {
                    i3 = r12;
                }
                if (r12 < i6) {
                    i6 = r12;
                }
                if (r10 > i4) {
                    i4 = r10;
                }
                if (r10 < i7) {
                    i7 = r10;
                }
            }
            this.mMinRed = i;
            this.mMaxRed = i2;
            this.mMinGreen = i6;
            this.mMaxGreen = i3;
            this.mMinBlue = i7;
            this.mMaxBlue = i4;
            this.mPopulation = i5;
        }

        public final Vbox splitBox() {
            if (canSplit()) {
                int findSplitPoint = findSplitPoint();
                Vbox vbox = new Vbox(findSplitPoint + 1, this.mUpperIndex);
                this.mUpperIndex = findSplitPoint;
                fitBox();
                return vbox;
            }
            throw new IllegalStateException("Can not split a box with only 1 color");
        }

        public final int getLongestColorDimension() {
            int i = this.mMaxRed - this.mMinRed;
            int i2 = this.mMaxGreen - this.mMinGreen;
            int i3 = this.mMaxBlue - this.mMinBlue;
            if (i < i2 || i < i3) {
                return (i2 < i || i2 < i3) ? -1 : -2;
            }
            return -3;
        }

        public final int findSplitPoint() {
            int longestColorDimension = getLongestColorDimension();
            ColorCutQuantizer colorCutQuantizer = ColorCutQuantizer.this;
            int[] iArr = colorCutQuantizer.mColors;
            int[] iArr2 = colorCutQuantizer.mHistogram;
            ColorCutQuantizer.modifySignificantOctet(iArr, longestColorDimension, this.mLowerIndex, this.mUpperIndex);
            Arrays.sort(iArr, this.mLowerIndex, this.mUpperIndex + 1);
            ColorCutQuantizer.modifySignificantOctet(iArr, longestColorDimension, this.mLowerIndex, this.mUpperIndex);
            int i = this.mPopulation / 2;
            int i2 = 0;
            for (int i3 = this.mLowerIndex; i3 <= this.mUpperIndex; i3++) {
                i2 += iArr2[iArr[i3]];
                if (i2 >= i) {
                    return i3;
                }
            }
            return this.mLowerIndex;
        }

        public final Palette.Swatch getAverageColor() {
            ColorCutQuantizer colorCutQuantizer = ColorCutQuantizer.this;
            int[] iArr = colorCutQuantizer.mColors;
            int[] iArr2 = colorCutQuantizer.mHistogram;
            int i = 0;
            int i2 = 0;
            int i3 = 0;
            int i4 = 0;
            for (int i5 = this.mLowerIndex; i5 <= this.mUpperIndex; i5++) {
                int i6 = iArr[i5];
                int i7 = iArr2[i6];
                i2 += i7;
                i += ColorCutQuantizer.quantizedRed(i6) * i7;
                i3 += ColorCutQuantizer.quantizedGreen(i6) * i7;
                i4 += i7 * ColorCutQuantizer.quantizedBlue(i6);
            }
            float f = (float) i2;
            return new Palette.Swatch(ColorCutQuantizer.approximateToRgb888(Math.round(((float) i) / f), Math.round(((float) i3) / f), Math.round(((float) i4) / f)), i2);
        }
    }

    public static void modifySignificantOctet(int[] iArr, int i, int i2, int i3) {
        if (i == -2) {
            while (i2 <= i3) {
                int i4 = iArr[i2];
                iArr[i2] = quantizedBlue(i4) | (quantizedGreen(i4) << 10) | (quantizedRed(i4) << 5);
                i2++;
            }
        } else if (i == -1) {
            while (i2 <= i3) {
                int i5 = iArr[i2];
                iArr[i2] = quantizedRed(i5) | (quantizedBlue(i5) << 10) | (quantizedGreen(i5) << 5);
                i2++;
            }
        }
    }

    public final boolean shouldIgnoreColor(int i) {
        int approximateToRgb888 = approximateToRgb888(i);
        ColorUtils.colorToHSL(approximateToRgb888, this.mTempHsl);
        return shouldIgnoreColor(approximateToRgb888, this.mTempHsl);
    }

    public final boolean shouldIgnoreColor(Palette.Swatch swatch) {
        return shouldIgnoreColor(swatch.getRgb(), swatch.getHsl());
    }

    public final boolean shouldIgnoreColor(int i, float[] fArr) {
        Palette.Filter[] filterArr = this.mFilters;
        if (filterArr != null && filterArr.length > 0) {
            int length = filterArr.length;
            for (int i2 = 0; i2 < length; i2++) {
                if (!this.mFilters[i2].isAllowed(i, fArr)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int quantizeFromRgb888(int i) {
        return modifyWordWidth(Color.blue(i), 8, 5) | (modifyWordWidth(Color.red(i), 8, 5) << 10) | (modifyWordWidth(Color.green(i), 8, 5) << 5);
    }

    public static int approximateToRgb888(int i, int i2, int i3) {
        return Color.rgb(modifyWordWidth(i, 5, 8), modifyWordWidth(i2, 5, 8), modifyWordWidth(i3, 5, 8));
    }

    public static int approximateToRgb888(int i) {
        return approximateToRgb888(quantizedRed(i), quantizedGreen(i), quantizedBlue(i));
    }
}
