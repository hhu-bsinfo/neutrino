package de.hhu.bsinfo.neutrino.verbs;

import de.hhu.bsinfo.neutrino.struct.field.NativeIntegerBitMask;
import de.hhu.bsinfo.neutrino.struct.field.NativeInteger;
import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.struct.Struct;
import de.hhu.bsinfo.neutrino.util.SystemUtil;
import de.hhu.bsinfo.neutrino.util.flag.IntegerFlag;
import de.hhu.bsinfo.neutrino.struct.LinkNative;
import de.hhu.bsinfo.neutrino.util.NativeObjectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@LinkNative("ibv_xrcd")
public class ExtendedConnectionDomain extends Struct implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedConnectionDomain.class);

    private final Context context = referenceField("context");

    public ExtendedConnectionDomain(final long handle) {
        super(handle);
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void close() throws IOException {
        var result = Result.localInstance();
        Verbs.closeExtendedConnectionDomain(getHandle(), result.getHandle());
        if (result.isError()) {
            throw new IOException(SystemUtil.getErrorMessage());
        }

        NativeObjectRegistry.deregisterObject(this);
    }

    public enum AttributeFlag implements IntegerFlag {
        FD(1), OFLAGS(1 << 1), RESERVED(1 << 2);

        private final int value;

        AttributeFlag(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return value;
        }
    }

    public enum OperationFlag implements IntegerFlag {
        O_CREAT(Verbs.getOperationFlagCreate()),
        O_EXCL(Verbs.getOperationFlagExclusive());

        private final int value;

        OperationFlag(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return value;
        }
    }

    @LinkNative("ibv_xrcd_init_attr")
    public static final class InitialAttributes extends Struct {

        private final NativeIntegerBitMask<AttributeFlag> attributesMask = integerBitField("comp_mask");
        private final NativeInteger fileDescriptor = integerField("fd");
        private final NativeIntegerBitMask<OperationFlag> operationFlags = integerBitField("oflags");

        InitialAttributes() {}

        public int getAttributesMask() {
            return attributesMask.get();
        }

        public int getFileDescriptor() {
            return fileDescriptor.get();
        }

        public int getOperationFlags() {
            return operationFlags.get();
        }

        public void setAttributeMask(final AttributeFlag... flags) {
            attributesMask.set(flags);
        }

        public void setFileDescriptor(final int value) {
            fileDescriptor.set(value);
        }

        public void setOperationFlags(final OperationFlag... flags) {
            operationFlags.set(flags);
        }

        @Override
        public String toString() {
            return "InitialAttributes {" +
                "\n\tattributesMask=" + attributesMask +
                ",\n\tfileDescriptor=" + fileDescriptor +
                ",\n\toperationFlags=" + operationFlags +
                "\n}";
        }

        public static final class Builder {

            private final Set<AttributeFlag> attributeFlags = new HashSet<>();

            private int fileDescriptor;
            private OperationFlag[] operationFlags;

            public Builder(OperationFlag... flags) {
                this.operationFlags = flags;
                this.attributeFlags.add(AttributeFlag.OFLAGS);
                this.attributeFlags.add(AttributeFlag.FD);
            }

            public Builder withFileDescriptor(int fileDescriptor) {
                this.fileDescriptor = fileDescriptor;
                return this;
            }

            public InitialAttributes build() {
                var ret = new InitialAttributes();

                ret.setAttributeMask(attributeFlags.toArray(new AttributeFlag[0]));
                ret.setFileDescriptor(fileDescriptor);

                if(operationFlags != null) {
                    ret.setOperationFlags(operationFlags);
                }

                return ret;
            }
        }
    }
}
