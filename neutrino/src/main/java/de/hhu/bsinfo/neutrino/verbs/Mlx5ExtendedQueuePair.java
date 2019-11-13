package de.hhu.bsinfo.neutrino.verbs;

import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.data.*;
import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.struct.Struct;
import de.hhu.bsinfo.neutrino.util.Flag;
import de.hhu.bsinfo.neutrino.util.LinkNative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Mlx5ExtendedQueuePair extends Struct {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mlx5ExtendedQueuePair.class);

    private final NativeLongBitMask<AttributeFlag> attributeMask = longBitField("comp_mask");

    Mlx5ExtendedQueuePair(final long handle) {
        super(handle);
    }

    Mlx5ExtendedQueuePair(final LocalBuffer buffer, final long offset) {
        super(buffer, offset);
    }

    public static Mlx5ExtendedQueuePair fromExtendedQueuePair(ExtendedQueuePair extendedQueuePair) {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.mlx5ExtendedQueuePairFromExtendedQueuePair(extendedQueuePair.getHandle(), result.getHandle());
        if(result.isError()) {
            LOGGER.error("Converting queue pair to extended queue pair failed with error [{}]: {}", result.getStatus(), result.getStatusMessage());
        }

        return result.getAndRelease(Mlx5ExtendedQueuePair::new);
    }

    public long getAttributeMask() {
        return attributeMask.get();
    }

    void setAttributeMask(final AttributeFlag... flags) {
        attributeMask.set(flags);
    }

    @LinkNative("mlx5dv_qp_init_attr")
    public static final class InitialAttributes extends Struct {

        private final NativeLongBitMask<AttributeFlag> attributesMask = longBitField("comp_mask");
        private final NativeIntegerBitMask<CreationFlag> creationFlags = integerBitField("create_flags");
        private final NativeLongBitMask<SendOperationFlag> sendOperationFlags = longBitField("send_ops_flags");

        public final DcInitialAttributes dcInitialAttributes = valueField("dc_init_attr", DcInitialAttributes::new);

        InitialAttributes() {}

        public long getAttributesMask() {
            return attributesMask.get();
        }

        public int getCreationFlags() {
            return creationFlags.get();
        }

        public long getSendOperationFlags() {
            return sendOperationFlags.get();
        }

        void setAttributesMask(final AttributeFlag... flags) {
            this.attributesMask.set(flags);
        }

        void setCreationFlags(final CreationFlag... creationFlags) {
            this.creationFlags.set(creationFlags);
        }

        void setSendOperationFlags(SendOperationFlag... sendOperationFlags) {
            this.sendOperationFlags.set(sendOperationFlags);
        }

        public static final class Builder {

            // Initial Attributes
            private final Set<AttributeFlag> attributeMask = new HashSet<>();
            private CreationFlag[] creationFlags;
            private SendOperationFlag[] sendOperationFlags;

            // DC Initial Attributes
            private DcType dcType;
            private long accessKey;

            public Builder withCreationFlags(final CreationFlag... flags) {
                creationFlags = flags;
                attributeMask.add(AttributeFlag.QP_CREATE_FLAGS);
                return this;
            }

            public Builder withSendOperationFlags(final SendOperationFlag... flags) {
                sendOperationFlags = flags;
                attributeMask.add(AttributeFlag.SEND_OPS_FLAGS);
                return this;
            }

            public Builder withDcType(final DcType dcType) {
                this.dcType = dcType;
                attributeMask.add(AttributeFlag.DC);
                return this;
            }

            public Builder withDcAccessKey(final long accessKey) {
                this.accessKey = accessKey;
                attributeMask.add(AttributeFlag.DC);
                return this;
            }

            public InitialAttributes build() {
                var ret = new InitialAttributes();

                ret.setAttributesMask(attributeMask.toArray(new AttributeFlag[0]));

                if(creationFlags != null) ret.setCreationFlags(creationFlags);
                if(sendOperationFlags != null) ret.setSendOperationFlags(sendOperationFlags);

                ret.dcInitialAttributes.setAccessKey(accessKey);

                if(dcType != null) ret.dcInitialAttributes.setDcType(dcType);

                return ret;
            }
        }
    }

    @LinkNative("mlx5dv_dc_init_attr")
    public static final class DcInitialAttributes extends Struct {

        private final NativeEnum<DcType> dcType = enumField("dc_type", DcType.CONVERTER);
        private final NativeLong accessKey = longField("dct_access_key");

        DcInitialAttributes(final long handle) {
            super(handle);
        }

        DcInitialAttributes(final LocalBuffer buffer, final long offset) {
            super(buffer, offset);
        }

        public DcType getDcType() {
            return dcType.get();
        }

        public long getAccessKey() {
            return accessKey.get();
        }

        void setDcType(DcType dcType) {
            this.dcType.set(dcType);
        }

        void setAccessKey(long accessKey) {
            this.accessKey.set(accessKey);
        }
    }

    public enum AttributeFlag implements Flag {
        QP_CREATE_FLAGS(1), DC(1 << 1), SEND_OPS_FLAGS(1 << 2);

        private final long value;

        AttributeFlag(long value) {
            this.value = value;
        }

        @Override
        public long getValue() {
            return value;
        }
    }

    public enum CreationFlag implements Flag {
        CREATE_TUNNEL_OFFLOADS(1), CREATE_TIR_ALLOW_SELF_LOOPBACK_UC(1 << 1),
        CREATE_TIR_ALLOW_SELF_LOOPBACK_MC(1 << 2), CREATE_DISABLE_SCATTER_TO_CQE(1 << 3),
        CREATE_ALLOW_SCATTER_TO_CQE(1 << 4), CREATE_PACKET_BASED_CREDIT_MODE(1 << 5);

        private final long value;

        CreationFlag(long value) {
            this.value = value;
        }

        @Override
        public long getValue() {
            return value;
        }
    }

    public enum SendOperationFlag implements Flag {
        WITH_MR_INTERLEAVED(1), WITH_MR_LIST(1 << 1);

        private final long value;

        SendOperationFlag(long value) {
            this.value = value;
        }

        @Override
        public long getValue() {
            return value;
        }
    }

    public enum DcType {
        DCT(1), DCI(1 << 1);

        private static final DcType[] VALUES;

        static {
            int arrayLength = Arrays.stream(values()).mapToInt(element -> element.value).max().orElseThrow() + 1;

            VALUES = new DcType[arrayLength];

            for (var element : values()) {
                VALUES[element.value] = element;
            }
        }

        private final int value;

        DcType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static final EnumConverter<DcType> CONVERTER = new EnumConverter<>() {

            @Override
            public int toInt(DcType enumeration) {
                return enumeration.value;
            }

            @Override
            public DcType toEnum(int integer) {
                if (integer < DCT.value || integer > DCI.value) {
                    throw new IllegalArgumentException(String.format("Unknown state provided %d", integer));
                }

                return VALUES[integer];
            }
        };
    }
}
