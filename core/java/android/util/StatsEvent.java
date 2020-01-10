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

package android.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.SystemClock;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

/**
 * StatsEvent builds and stores the buffer sent over the statsd socket.
 * This class defines and encapsulates the socket protocol.
 *
 * <p>Usage:</p>
 * <pre>
 *      StatsEvent statsEvent = StatsEvent.newBuilder()
 *          .setAtomId(atomId)
 *          .writeBoolean(false)
 *          .writeString("annotated String field")
 *          .addBooleanAnnotation(annotationId, true)
 *          .build();
 *
 *      StatsLog.write(statsEvent);
 * </pre>
 * @hide
 **/
public final class StatsEvent {
    // Type Ids.
    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_INT = 0x00;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_LONG = 0x01;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_STRING = 0x02;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_LIST = 0x03;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_FLOAT = 0x04;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_BOOLEAN = 0x05;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_BYTE_ARRAY = 0x06;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_OBJECT = 0x07;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_KEY_VALUE_PAIRS = 0x08;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_ATTRIBUTION_CHAIN = 0x09;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final byte TYPE_ERRORS = 0x0F;

    // Error flags.
    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_NO_TIMESTAMP = 0x1;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_NO_ATOM_ID = 0x2;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_OVERFLOW = 0x4;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_ATTRIBUTION_CHAIN_TOO_LONG = 0x8;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_TOO_MANY_KEY_VALUE_PAIRS = 0x10;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_ANNOTATION_DOES_NOT_FOLLOW_FIELD = 0x20;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_INVALID_ANNOTATION_ID = 0x40;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_ANNOTATION_ID_TOO_LARGE = 0x80;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_TOO_MANY_ANNOTATIONS = 0x100;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_TOO_MANY_FIELDS = 0x200;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int ERROR_ATTRIBUTION_UIDS_TAGS_SIZES_NOT_EQUAL = 0x1000;

    // Size limits.

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int MAX_ANNOTATION_COUNT = 15;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int MAX_ATTRIBUTION_NODES = 127;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int MAX_NUM_ELEMENTS = 127;

    /**
     * @hide
     **/
    @VisibleForTesting
    public static final int MAX_KEY_VALUE_PAIRS = 127;

    private static final int LOGGER_ENTRY_MAX_PAYLOAD = 4068;

    // Max payload size is 4 bytes less as 4 bytes are reserved for statsEventTag.
    // See android_util_StatsLog.cpp.
    private static final int MAX_PAYLOAD_SIZE = LOGGER_ENTRY_MAX_PAYLOAD - 4;

    private final int mAtomId;
    private final Buffer mBuffer;
    private final int mNumBytes;

    private StatsEvent(final int atomId, @NonNull final Buffer buffer, final int numBytes) {
        mAtomId = atomId;
        mBuffer = buffer;
        mNumBytes = numBytes;
    }

    /**
     * Returns a new StatsEvent.Builder for building StatsEvent object.
     **/
    @NonNull
    public static StatsEvent.Builder newBuilder() {
        return new StatsEvent.Builder(Buffer.obtain());
    }

    /**
     * Get the atom Id of the atom encoded in this StatsEvent object.
     *
     * @hide
     **/
    public int getAtomId() {
        return mAtomId;
    }

    /**
     * Get the byte array that contains the encoded payload that can be sent to statsd.
     *
     * @hide
     **/
    @NonNull
    public byte[] getBytes() {
        return mBuffer.getBytes();
    }

    /**
     * Get the number of bytes used to encode the StatsEvent payload.
     *
     * @hide
     **/
    public int getNumBytes() {
        return mNumBytes;
    }

    /**
     * Recycle this StatsEvent object.
     **/
    public void release() {
        mBuffer.release();
    }

    /**
     * Builder for constructing a StatsEvent object.
     *
     * <p>This class defines and encapsulates the socket encoding for the buffer.
     * The write methods must be called in the same order as the order of fields in the
     * atom definition.</p>
     *
     * <p>setAtomId() can be called anytime before build().</p>
     *
     * <p>Example:</p>
     * <pre>
     *     // Atom definition.
     *     message MyAtom {
     *         optional int32 field1 = 1;
     *         optional int64 field2 = 2;
     *         optional string field3 = 3 [(annotation1) = true];
     *     }
     *
     *     // StatsEvent construction.
     *     StatsEvent.newBuilder()
     *     StatsEvent statsEvent = StatsEvent.newBuilder()
     *         .setAtomId(atomId)
     *         .writeInt(3) // field1
     *         .writeLong(8L) // field2
     *         .writeString("foo") // field 3
     *         .addBooleanAnnotation(annotation1Id, true)
     *         .build();
     * </pre>
     **/
    public static final class Builder {
        // Fixed positions.
        private static final int POS_NUM_ELEMENTS = 1;
        private static final int POS_TIMESTAMP_NS = POS_NUM_ELEMENTS + Byte.BYTES;
        private static final int POS_ATOM_ID = POS_TIMESTAMP_NS + Byte.BYTES + Long.BYTES;

        private final Buffer mBuffer;
        private long mTimestampNs;
        private int mAtomId;
        private byte mCurrentAnnotationCount;
        private int mPos;
        private int mPosLastField;
        private byte mLastType;
        private int mNumElements;
        private int mErrorMask;

        private Builder(final Buffer buffer) {
            mBuffer = buffer;
            mCurrentAnnotationCount = 0;
            mAtomId = 0;
            mTimestampNs = SystemClock.elapsedRealtimeNanos();
            mNumElements = 0;

            // Set mPos to 0 for writing TYPE_OBJECT at 0th position.
            mPos = 0;
            writeTypeId(TYPE_OBJECT);

            // Set mPos to after atom id's location in the buffer.
            // First 2 elements in the buffer are event timestamp followed by the atom id.
            mPos = POS_ATOM_ID + Byte.BYTES + Integer.BYTES;
            mPosLastField = 0;
            mLastType = 0;
        }

        /**
         * Sets the atom id for this StatsEvent.
         **/
        @NonNull
        public Builder setAtomId(final int atomId) {
            mAtomId = atomId;
            return this;
        }

        /**
         * Sets the timestamp in nanos for this StatsEvent.
         **/
        @VisibleForTesting
        @NonNull
        public Builder setTimestampNs(final long timestampNs) {
            mTimestampNs = timestampNs;
            return this;
        }

        /**
         * Write a boolean field to this StatsEvent.
         **/
        @NonNull
        public Builder writeBoolean(final boolean value) {
            // Write boolean typeId byte followed by boolean byte representation.
            writeTypeId(TYPE_BOOLEAN);
            mPos += mBuffer.putBoolean(mPos, value);
            mNumElements++;
            return this;
        }

        /**
         * Write an integer field to this StatsEvent.
         **/
        @NonNull
        public Builder writeInt(final int value) {
            // Write integer typeId byte followed by 4-byte representation of value.
            writeTypeId(TYPE_INT);
            mPos += mBuffer.putInt(mPos, value);
            mNumElements++;
            return this;
        }

        /**
         * Write a long field to this StatsEvent.
         **/
        @NonNull
        public Builder writeLong(final long value) {
            // Write long typeId byte followed by 8-byte representation of value.
            writeTypeId(TYPE_LONG);
            mPos += mBuffer.putLong(mPos, value);
            mNumElements++;
            return this;
        }

        /**
         * Write a float field to this StatsEvent.
         **/
        @NonNull
        public Builder writeFloat(final float value) {
            // Write float typeId byte followed by 4-byte representation of value.
            writeTypeId(TYPE_FLOAT);
            mPos += mBuffer.putFloat(mPos, value);
            mNumElements++;
            return this;
        }

        /**
         * Write a String field to this StatsEvent.
         **/
        @NonNull
        public Builder writeString(@NonNull final String value) {
            // Write String typeId byte, followed by 4-byte representation of number of bytes
            // in the UTF-8 encoding, followed by the actual UTF-8 byte encoding of value.
            final byte[] valueBytes = stringToBytes(value);
            writeByteArray(valueBytes, TYPE_STRING);
            return this;
        }

        /**
         * Write a byte array field to this StatsEvent.
         **/
        @NonNull
        public Builder writeByteArray(@NonNull final byte[] value) {
            // Write byte array typeId byte, followed by 4-byte representation of number of bytes
            // in value, followed by the actual byte array.
            writeByteArray(value, TYPE_BYTE_ARRAY);
            return this;
        }

        private void writeByteArray(@NonNull final byte[] value, final byte typeId) {
            writeTypeId(typeId);
            final int numBytes = value.length;
            mPos += mBuffer.putInt(mPos, numBytes);
            mPos += mBuffer.putByteArray(mPos, value);
            mNumElements++;
        }

        /**
         * Write an attribution chain field to this StatsEvent.
         *
         * The sizes of uids and tags must be equal. The AttributionNode at position i is
         * made up of uids[i] and tags[i].
         *
         * @param uids array of uids in the attribution nodes.
         * @param tags array of tags in the attribution nodes.
         **/
        @NonNull
        public Builder writeAttributionChain(
                @NonNull final int[] uids, @NonNull final String[] tags) {
            final byte numUids = (byte) uids.length;
            final byte numTags = (byte) tags.length;

            if (numUids != numTags) {
                mErrorMask |= ERROR_ATTRIBUTION_UIDS_TAGS_SIZES_NOT_EQUAL;
            } else if (numUids > MAX_ATTRIBUTION_NODES) {
                mErrorMask |= ERROR_ATTRIBUTION_CHAIN_TOO_LONG;
            } else {
                // Write attribution chain typeId byte, followed by 1-byte representation of
                // number of attribution nodes, followed by encoding of each attribution node.
                writeTypeId(TYPE_ATTRIBUTION_CHAIN);
                mPos += mBuffer.putByte(mPos, numUids);
                for (int i = 0; i < numUids; i++) {
                    // Each uid is encoded as 4-byte representation of its int value.
                    mPos += mBuffer.putInt(mPos, uids[i]);

                    // Each tag is encoded as 4-byte representation of number of bytes in its
                    // UTF-8 encoding, followed by the actual UTF-8 bytes.
                    final byte[] tagBytes = stringToBytes(tags[i]);
                    mPos += mBuffer.putInt(mPos, tagBytes.length);
                    mPos += mBuffer.putByteArray(mPos, tagBytes);
                }
                mNumElements++;
            }
            return this;
        }

        /**
         * Write KeyValuePairsAtom entries to this StatsEvent.
         *
         * @param intMap Integer key-value pairs.
         * @param longMap Long key-value pairs.
         * @param stringMap String key-value pairs.
         * @param floatMap Float key-value pairs.
         **/
        @NonNull
        public Builder writeKeyValuePairs(
                @NonNull final SparseIntArray intMap,
                @NonNull final SparseLongArray longMap,
                @NonNull final SparseArray<String> stringMap,
                @NonNull final SparseArray<Float> floatMap) {
            final int intMapSize = intMap.size();
            final int longMapSize = longMap.size();
            final int stringMapSize = stringMap.size();
            final int floatMapSize = floatMap.size();
            final int totalCount = intMapSize + longMapSize + stringMapSize + floatMapSize;

            if (totalCount > MAX_KEY_VALUE_PAIRS) {
                mErrorMask |= ERROR_TOO_MANY_KEY_VALUE_PAIRS;
            } else {
                writeTypeId(TYPE_KEY_VALUE_PAIRS);
                mPos += mBuffer.putByte(mPos, (byte) totalCount);

                for (int i = 0; i < intMapSize; i++) {
                    final int key = intMap.keyAt(i);
                    final int value = intMap.valueAt(i);
                    mPos += mBuffer.putInt(mPos, key);
                    writeTypeId(TYPE_INT);
                    mPos += mBuffer.putInt(mPos, value);
                }

                for (int i = 0; i < longMapSize; i++) {
                    final int key = longMap.keyAt(i);
                    final long value = longMap.valueAt(i);
                    mPos += mBuffer.putInt(mPos, key);
                    writeTypeId(TYPE_LONG);
                    mPos += mBuffer.putLong(mPos, value);
                }

                for (int i = 0; i < stringMapSize; i++) {
                    final int key = stringMap.keyAt(i);
                    final String value = stringMap.valueAt(i);
                    mPos += mBuffer.putInt(mPos, key);
                    writeTypeId(TYPE_STRING);
                    final byte[] valueBytes = stringToBytes(value);
                    mPos += mBuffer.putInt(mPos, valueBytes.length);
                    mPos += mBuffer.putByteArray(mPos, valueBytes);
                }

                for (int i = 0; i < floatMapSize; i++) {
                    final int key = floatMap.keyAt(i);
                    final float value = floatMap.valueAt(i);
                    mPos += mBuffer.putInt(mPos, key);
                    writeTypeId(TYPE_FLOAT);
                    mPos += mBuffer.putFloat(mPos, value);
                }

                mNumElements++;
            }

            return this;
        }

        /**
         * Write a boolean annotation for the last field written.
         **/
        @NonNull
        public Builder addBooleanAnnotation(
                final byte annotationId, final boolean value) {
            // Ensure there's a field written to annotate.
            if (0 == mPosLastField) {
                mErrorMask |= ERROR_ANNOTATION_DOES_NOT_FOLLOW_FIELD;
            } else if (mCurrentAnnotationCount >= MAX_ANNOTATION_COUNT) {
                mErrorMask |= ERROR_TOO_MANY_ANNOTATIONS;
            } else {
                mPos += mBuffer.putByte(mPos, annotationId);
                mPos += mBuffer.putByte(mPos, TYPE_BOOLEAN);
                mPos += mBuffer.putBoolean(mPos, value);
                mCurrentAnnotationCount++;
                writeAnnotationCount();
            }
            return this;
        }

        /**
         * Write an integer annotation for the last field written.
         **/
        @NonNull
        public Builder addIntAnnotation(final byte annotationId, final int value) {
            if (0 == mPosLastField) {
                mErrorMask |= ERROR_ANNOTATION_DOES_NOT_FOLLOW_FIELD;
            } else if (mCurrentAnnotationCount >= MAX_ANNOTATION_COUNT) {
                mErrorMask |= ERROR_TOO_MANY_ANNOTATIONS;
            } else {
                mPos += mBuffer.putByte(mPos, annotationId);
                mPos += mBuffer.putByte(mPos, TYPE_INT);
                mPos += mBuffer.putInt(mPos, value);
                mCurrentAnnotationCount++;
                writeAnnotationCount();
            }
            return this;
        }

        /**
         * Builds a StatsEvent object with values entered in this Builder.
         **/
        @NonNull
        public StatsEvent build() {
            if (0L == mTimestampNs) {
                mErrorMask |= ERROR_NO_TIMESTAMP;
            }
            if (0 == mAtomId) {
                mErrorMask |= ERROR_NO_ATOM_ID;
            }
            if (mBuffer.hasOverflowed()) {
                mErrorMask |= ERROR_OVERFLOW;
            }
            if (mNumElements > MAX_NUM_ELEMENTS) {
                mErrorMask |= ERROR_TOO_MANY_FIELDS;
            }

            int size = mPos;
            mPos = POS_TIMESTAMP_NS;
            writeLong(mTimestampNs);
            writeInt(mAtomId);
            if (0 == mErrorMask) {
                mBuffer.putByte(POS_NUM_ELEMENTS, (byte) mNumElements);
            } else {
                mPos += mBuffer.putByte(mPos, TYPE_ERRORS);
                mPos += mBuffer.putInt(mPos, mErrorMask);
                mBuffer.putByte(POS_NUM_ELEMENTS, (byte) 3);
                size = mPos;
            }

            return new StatsEvent(mAtomId, mBuffer, size);
        }

        private void writeTypeId(final byte typeId) {
            mPosLastField = mPos;
            mLastType = typeId;
            mCurrentAnnotationCount = 0;
            final byte encodedId = (byte) (typeId & 0x0F);
            mPos += mBuffer.putByte(mPos, encodedId);
        }

        private void writeAnnotationCount() {
            // Use first 4 bits for annotation count and last 4 bits for typeId.
            final byte encodedId = (byte) ((mCurrentAnnotationCount << 4) | (mLastType & 0x0F));
            mBuffer.putByte(mPosLastField, encodedId);
        }

        @NonNull
        private static byte[] stringToBytes(@Nullable final String value) {
            return (null == value ? "" : value).getBytes(UTF_8);
        }
    }

    private static final class Buffer {
        private static Object sLock = new Object();

        @GuardedBy("sLock")
        private static Buffer sPool;

        private final byte[] mBytes = new byte[MAX_PAYLOAD_SIZE];
        private boolean mOverflow = false;

        @NonNull
        private static Buffer obtain() {
            final Buffer buffer;
            synchronized (sLock) {
                buffer = null == sPool ? new Buffer() : sPool;
                sPool = null;
            }
            buffer.reset();
            return buffer;
        }

        private Buffer() {
        }

        @NonNull
        private byte[] getBytes() {
            return mBytes;
        }

        private void release() {
            synchronized (sLock) {
                if (null == sPool) {
                    sPool = this;
                }
            }
        }

        private void reset() {
            mOverflow = false;
        }

        private boolean hasOverflowed() {
            return mOverflow;
        }

        /**
         * Checks for available space in the byte array.
         *
         * @param index starting position in the buffer to start the check.
         * @param numBytes number of bytes to check from index.
         * @return true if space is available, false otherwise.
         **/
        private boolean hasEnoughSpace(final int index, final int numBytes) {
            final boolean result = index + numBytes < MAX_PAYLOAD_SIZE;
            if (!result) {
                mOverflow = true;
            }
            return result;
        }

        /**
         * Writes a byte into the buffer.
         *
         * @param index position in the buffer where the byte is written.
         * @param value the byte to write.
         * @return number of bytes written to buffer from this write operation.
         **/
        private int putByte(final int index, final byte value) {
            if (hasEnoughSpace(index, Byte.BYTES)) {
                mBytes[index] = (byte) (value);
                return Byte.BYTES;
            }
            return 0;
        }

        /**
         * Writes a boolean into the buffer.
         *
         * @param index position in the buffer where the boolean is written.
         * @param value the boolean to write.
         * @return number of bytes written to buffer from this write operation.
         **/
        private int putBoolean(final int index, final boolean value) {
            return putByte(index, (byte) (value ? 1 : 0));
        }

        /**
         * Writes an integer into the buffer.
         *
         * @param index position in the buffer where the integer is written.
         * @param value the integer to write.
         * @return number of bytes written to buffer from this write operation.
         **/
        private int putInt(final int index, final int value) {
            if (hasEnoughSpace(index, Integer.BYTES)) {
                // Use little endian byte order.
                mBytes[index] = (byte) (value);
                mBytes[index + 1] = (byte) (value >> 8);
                mBytes[index + 2] = (byte) (value >> 16);
                mBytes[index + 3] = (byte) (value >> 24);
                return Integer.BYTES;
            }
            return 0;
        }

        /**
         * Writes a long into the buffer.
         *
         * @param index position in the buffer where the long is written.
         * @param value the long to write.
         * @return number of bytes written to buffer from this write operation.
         **/
        private int putLong(final int index, final long value) {
            if (hasEnoughSpace(index, Long.BYTES)) {
                // Use little endian byte order.
                mBytes[index] = (byte) (value);
                mBytes[index + 1] = (byte) (value >> 8);
                mBytes[index + 2] = (byte) (value >> 16);
                mBytes[index + 3] = (byte) (value >> 24);
                mBytes[index + 4] = (byte) (value >> 32);
                mBytes[index + 5] = (byte) (value >> 40);
                mBytes[index + 6] = (byte) (value >> 48);
                mBytes[index + 7] = (byte) (value >> 56);
                return Long.BYTES;
            }
            return 0;
        }

        /**
         * Writes a float into the buffer.
         *
         * @param index position in the buffer where the float is written.
         * @param value the float to write.
         * @return number of bytes written to buffer from this write operation.
         **/
        private int putFloat(final int index, final float value) {
            return putInt(index, Float.floatToIntBits(value));
        }

        /**
         * Copies a byte array into the buffer.
         *
         * @param index position in the buffer where the byte array is copied.
         * @param value the byte array to copy.
         * @return number of bytes written to buffer from this write operation.
         **/
        private int putByteArray(final int index, @NonNull final byte[] value) {
            final int numBytes = value.length;
            if (hasEnoughSpace(index, numBytes)) {
                System.arraycopy(value, 0, mBytes, index, numBytes);
                return numBytes;
            }
            return 0;
        }
    }
}
