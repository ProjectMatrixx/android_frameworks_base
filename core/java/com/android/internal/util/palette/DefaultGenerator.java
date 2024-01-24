package com.android.internal.util.palette;

import java.util.List;
import com.android.internal.util.palette.Palette;

public class DefaultGenerator extends Palette.Generator {
    public Palette.Swatch mDarkMutedSwatch;
    public Palette.Swatch mDarkVibrantSwatch;
    public int mHighestPopulation;
    public Palette.Swatch mLightMutedSwatch;
    public Palette.Swatch mLightVibrantSwatch;
    public Palette.Swatch mMutedSwatch;
    public List<Palette.Swatch> mSwatches;
    public Palette.Swatch mVibrantSwatch;

    public void generate(List<Palette.Swatch> list) {
        this.mSwatches = list;
        this.mHighestPopulation = findMaxPopulation();
        generateVariationColors();
        generateEmptySwatches();
    }

    public Palette.Swatch getLightVibrantSwatch() {
        return this.mLightVibrantSwatch;
    }

    public Palette.Swatch getDarkVibrantSwatch() {
        return this.mDarkVibrantSwatch;
    }

    public final void generateVariationColors() {
        this.mVibrantSwatch = findColorVariation(0.5f, 0.3f, 0.7f, 1.0f, 0.35f, 1.0f);
        this.mLightVibrantSwatch = findColorVariation(0.74f, 0.55f, 1.0f, 1.0f, 0.35f, 1.0f);
        this.mDarkVibrantSwatch = findColorVariation(0.26f, 0.0f, 0.45f, 1.0f, 0.35f, 1.0f);
        this.mMutedSwatch = findColorVariation(0.5f, 0.3f, 0.7f, 0.3f, 0.0f, 0.4f);
        this.mLightMutedSwatch = findColorVariation(0.74f, 0.55f, 1.0f, 0.3f, 0.0f, 0.4f);
        this.mDarkMutedSwatch = findColorVariation(0.26f, 0.0f, 0.45f, 0.3f, 0.0f, 0.4f);
    }

    public final void generateEmptySwatches() {
        Palette.Swatch swatch;
        Palette.Swatch swatch2;
        if (this.mVibrantSwatch == null && (swatch2 = this.mDarkVibrantSwatch) != null) {
            float[] copyHslValues = copyHslValues(swatch2);
            copyHslValues[2] = 0.5f;
            this.mVibrantSwatch = new Palette.Swatch(ColorUtils.HSLToColor(copyHslValues), 0);
        }
        if (this.mDarkVibrantSwatch == null && (swatch = this.mVibrantSwatch) != null) {
            float[] copyHslValues2 = copyHslValues(swatch);
            copyHslValues2[2] = 0.26f;
            this.mDarkVibrantSwatch = new Palette.Swatch(ColorUtils.HSLToColor(copyHslValues2), 0);
        }
    }

    public final int findMaxPopulation() {
        int i = 0;
        for (Palette.Swatch population : this.mSwatches) {
            i = Math.max(i, population.getPopulation());
        }
        return i;
    }

    public final Palette.Swatch findColorVariation(float f, float f2, float f3, float f4, float f5, float f6) {
        Palette.Swatch swatch = null;
        float f7 = 0.0f;
        for (Palette.Swatch next : this.mSwatches) {
            float f8 = next.getHsl()[1];
            float f9 = next.getHsl()[2];
            if (f8 >= f5 && f8 <= f6 && f9 >= f2 && f9 <= f3 && !isAlreadySelected(next)) {
                float createComparisonValue = createComparisonValue(f8, f4, f9, f, next.getPopulation(), this.mHighestPopulation);
                if (swatch == null || createComparisonValue > f7) {
                    swatch = next;
                    f7 = createComparisonValue;
                }
            }
        }
        return swatch;
    }

    public final boolean isAlreadySelected(Palette.Swatch swatch) {
        return this.mVibrantSwatch == swatch || this.mDarkVibrantSwatch == swatch || this.mLightVibrantSwatch == swatch || this.mMutedSwatch == swatch || this.mDarkMutedSwatch == swatch || this.mLightMutedSwatch == swatch;
    }

    public static float createComparisonValue(float f, float f2, float f3, float f4, int i, int i2) {
        return createComparisonValue(f, f2, 3.0f, f3, f4, 6.0f, i, i2, 1.0f);
    }

    public static float createComparisonValue(float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7) {
        return weightedMean(invertDiff(f, f2), f3, invertDiff(f4, f5), f6, ((float) i) / ((float) i2), f7);
    }

    public static float[] copyHslValues(Palette.Swatch swatch) {
        float[] fArr = new float[3];
        System.arraycopy(swatch.getHsl(), 0, fArr, 0, 3);
        return fArr;
    }

    public static float invertDiff(float f, float f2) {
        return 1.0f - Math.abs(f - f2);
    }

    public static float weightedMean(float... fArr) {
        float f = 0.0f;
        float f2 = 0.0f;
        for (int i = 0; i < fArr.length; i += 2) {
            float f3 = fArr[i];
            float f4 = fArr[i + 1];
            f += f3 * f4;
            f2 += f4;
        }
        return f / f2;
    }
}
