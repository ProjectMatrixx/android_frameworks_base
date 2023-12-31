/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "TransformCanvas.h"

#include "FunctorDrawable.h"
#include "HolePunch.h"
#include "SkData.h"
#include "SkDrawable.h"
#include "SkMatrix.h"
#include "SkPaint.h"
#include "SkRect.h"
#include "SkRRect.h"

using namespace android::uirenderer::skiapipeline;

void TransformCanvas::onDrawAnnotation(const SkRect& rect, const char* key, SkData* value) {
    if (HOLE_PUNCH_ANNOTATION == key) {
        auto* rectParams = reinterpret_cast<const float*>(value->data());
        const float radiusX = rectParams[0];
        const float radiusY = rectParams[1];
        const float alpha = rectParams[2];
        SkRRect roundRect = SkRRect::MakeRectXY(rect, radiusX, radiusY);

        SkPaint paint;
        paint.setColor(SkColors::kBlack);
        paint.setBlendMode(mHolePunchBlendMode);
        paint.setAlphaf(alpha);
        mWrappedCanvas->drawRRect(roundRect, paint);
    }
}

void TransformCanvas::onDrawDrawable(SkDrawable* drawable, const SkMatrix* matrix) {
    // TransformCanvas filters all drawing commands while maintaining the current
    // clip stack and transformation. We need to draw most SkDrawables, since their
    // draw calls may call methods that affect the clip stack and transformation. (Any
    // actual draw commands will then be filtered out.) But FunctorDrawables are used
    // as leaf nodes which issue self-contained OpenGL/Vulkan commands. These won't
    // affect the clip stack + transformation, and in some cases cause problems (e.g. if
    // the surface only has an alpha channel). See b/203960959
    const auto* drawableName = drawable->getTypeName();
    if (drawableName == nullptr || strcmp(drawableName, FunctorDrawable::TYPE_NAME) != 0) {
        drawable->draw(this, matrix);
    }
}

bool TransformCanvas::onFilter(SkPaint& paint) const {
    return false;
}
