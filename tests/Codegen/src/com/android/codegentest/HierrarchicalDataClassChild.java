/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.codegentest;

import android.annotation.NonNull;
import android.os.Parcelable;

import com.android.internal.util.DataClass;

/**
 * An example of data classes that extend one another.
 *
 * Note that some features like constructor generation might not work well due to lack of
 * information about the superclass when generating code for subclass.
 *
 * It is recommended to avoid inheritance in favor of composition for new data classes,
 * particularly parcelable ones.
 *
 * However for legacy classes or where inheritance is desired for allocation efficiency,
 * you can either use a technique from this example, opting for mutability/setters, or just write
 * constructors by hand.
 *
 * @see HierrarchicalDataClassBase
 */
@DataClass(
        genParcelable = true,
        genConstructor = false,
        genSetters = true)
public class HierrarchicalDataClassChild extends HierrarchicalDataClassBase {

    private @NonNull String mChildData;



    // Code below generated by codegen v1.0.20.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/tests/Codegen/src/com/android/codegentest/HierrarchicalDataClassChild.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    public @NonNull String getChildData() {
        return mChildData;
    }

    @DataClass.Generated.Member
    public @NonNull HierrarchicalDataClassChild setChildData(@NonNull String value) {
        mChildData = value;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mChildData);
        return this;
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@NonNull android.os.Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        super.writeToParcel(dest, flags);

        dest.writeString(mChildData);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    protected HierrarchicalDataClassChild(@NonNull android.os.Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        super(in);

        String childData = in.readString();

        this.mChildData = childData;
        com.android.internal.util.AnnotationValidations.validate(
                NonNull.class, null, mChildData);

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @NonNull Parcelable.Creator<HierrarchicalDataClassChild> CREATOR
            = new Parcelable.Creator<HierrarchicalDataClassChild>() {
        @Override
        public HierrarchicalDataClassChild[] newArray(int size) {
            return new HierrarchicalDataClassChild[size];
        }

        @Override
        public HierrarchicalDataClassChild createFromParcel(@NonNull android.os.Parcel in) {
            return new HierrarchicalDataClassChild(in);
        }
    };

    @DataClass.Generated(
            time = 1604522376059L,
            codegenVersion = "1.0.20",
            sourceFile = "frameworks/base/tests/Codegen/src/com/android/codegentest/HierrarchicalDataClassChild.java",
            inputSignatures = "private @android.annotation.NonNull java.lang.String mChildData\nclass HierrarchicalDataClassChild extends com.android.codegentest.HierrarchicalDataClassBase implements []\n@com.android.internal.util.DataClass(genParcelable=true, genConstructor=false, genSetters=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
