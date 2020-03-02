package de.hhu.bsinfo.neutrino.verbs;

import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.data.*;
import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.struct.Struct;
import de.hhu.bsinfo.neutrino.util.BitMask;
import de.hhu.bsinfo.neutrino.util.flag.LongFlag;
import de.hhu.bsinfo.neutrino.util.LinkNative;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.hhu.bsinfo.neutrino.util.NativeObjectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LinkNative(value = "ibv_srq")
public class SharedReceiveQueue extends Struct implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedReceiveQueue.class);

    private final Context context = referenceField("context");
    private final NativeLong userContext = longField("srq_context");
    private final ProtectionDomain protectionDomain = referenceField("pd");
    private final NativeInteger eventsCompleted = integerField("events_completed");

    SharedReceiveQueue(long handle) {
        super(handle);
    }

    public Context getContext() {
        return context;
    }

    public NativeLong getUserContext() {
        return userContext;
    }

    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    public NativeInteger getEventsCompleted() {
        return eventsCompleted;
    }

    public boolean modify(Attributes attributes, AttributeFlag... flags) {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.modifySharedReceiveQueue(getHandle(), attributes.getHandle(), BitMask.intOf(flags), result.getHandle());
        boolean isError = result.isError();
        if (isError) {
            LOGGER.error("Modifying shared receive queue failed with error [{}]: {}", result.getStatus(), result.getStatusMessage());
        }

        result.releaseInstance();

        return !isError;
    }

    public boolean modify(Attributes.Builder builder) {
        return modify(builder.build(), builder.getAttributeFlags());
    }

    public Attributes queryAttributes() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);
        var attributes = new Attributes();

        Verbs.querySharedReceiveQueue(getHandle(), attributes.getHandle(), result.getHandle());
        boolean isError = result.isError();
        if (isError) {
            LOGGER.error("Querying shared receive queue failed with error [{}]: {}", result.getStatus(), result.getStatusMessage());
            attributes = null;
        }

        result.releaseInstance();

        return attributes;
    }

    public boolean postReceive(final ReceiveWorkRequest receiveWorkRequest) {
        return postReceive(receiveWorkRequest.getHandle());
    }

    public boolean postReceive(final NativeLinkedList<ReceiveWorkRequest> receiveWorkRequests) {
        return postReceive(receiveWorkRequests.getHandle());
    }

    private boolean postReceive(final long receiveWorkRequestsHandle) {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.postReceiveSharedReceiveQueue(getHandle(), receiveWorkRequestsHandle, result.getHandle());
        boolean isError = result.isError();
        if (isError) {
            LOGGER.error("Posting receive work requests to shared receive queue failed with error [{}]: {}", result.getStatus(), result.getStatusMessage());
        }

        result.releaseInstance();

        return !isError;
    }

    @Override
    public void close() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.destroySharedReceiveQueue(getHandle(), result.getHandle());
        if (result.isError()) {
            LOGGER.error("Destroying shared receive failed with error [{}]: {}", result.getStatus(), result.getStatusMessage());
        } else {
            NativeObjectRegistry.deregisterObject(this);
        }

        result.releaseInstance();
    }

    @Override
    public String toString() {
        return "SharedReceiveQueue {" +
                "\n\tuserContext=" + userContext +
                ",\n\teventsCompleted=" + eventsCompleted +
                "\n}";
    }

    public enum AttributeFlag implements LongFlag {
        MAX_WR(1), LIMIT(1 << 1);

        private final int value;

        AttributeFlag(int value) {
            this.value = value;
        }

        @Override
        public long getValue() {
            return value;
        }
    }

    public enum ExtendedAttributeFlag implements LongFlag {
        TYPE(1), PD(1 << 1), XRCD(1 << 2),
        CQ(1 << 3), TM(1 << 4), RESERVED(1 << 5);

        private final int value;

        ExtendedAttributeFlag(int value) {
            this.value = value;
        }

        @Override
        public long getValue() {
            return value;
        }
    }

    public enum Type {
        BASIC(1), XRC(2), TM(3);

        private static final Type[] VALUES;

        static {
            int arrayLength = Arrays.stream(values()).mapToInt(element -> element.value).max().orElseThrow() + 1;

            VALUES = new Type[arrayLength];

            for (Type element : Type.values()) {
                VALUES[element.value] = element;
            }
        }

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static final EnumConverter<Type> CONVERTER = new EnumConverter<>() {

            @Override
            public int toInt(Type enumeration) {
                return enumeration.value;
            }

            @Override
            public Type toEnum(int integer) {
                if (integer < BASIC.value || integer > TM.value) {
                    throw new IllegalArgumentException(String.format("Unknown operation code provided %d", integer));
                }

                return VALUES[integer];
            }
        };
    }

    @LinkNative(value = "ibv_srq_attr")
    public static final class Attributes extends Struct {

        private final NativeInteger maxWorkRequests = integerField("max_wr");
        private final NativeInteger maxScatterGatherElements = integerField("max_sge");
        private final NativeInteger limit = integerField("srq_limit");

        Attributes() {}

        Attributes(LocalBuffer buffer, long offset) {
            super(buffer, offset);
        }

        public int getMaxWorkRequests() {
            return maxWorkRequests.get();
        }

        public int getMaxScatterGatherElements() {
            return maxScatterGatherElements.get();
        }

        public int getLimit() {
            return limit.get();
        }

        void setMaxWorkRequests(int maxWorkRequests) {
            this.maxWorkRequests.set(maxWorkRequests);
        }

        void setMaxScatterGatherElements(int maxScatterGatherElements) {
            this.maxScatterGatherElements.set(maxScatterGatherElements);
        }

        void setLimit(int limit) {
            this.limit.set(limit);
        }

        @Override
        public String toString() {
            return "Attributes {" +
                "\n\tmaxWorkRequest=" + maxWorkRequests +
                ",\n\tmaxScatterGatherElements=" + maxScatterGatherElements +
                ",\n\tlimit=" + limit +
                "\n}";
        }

        public static final class Builder {

            private int maxWorkRequests;
            private int limit;

            private final Set<AttributeFlag> attributeFlags = new HashSet<>();

            public Builder() {}

            public Builder withMaxWorkRequests(final int maxWorkRequests) {
                this.maxWorkRequests = maxWorkRequests;
                attributeFlags.add(AttributeFlag.MAX_WR);
                return this;
            }

            public Builder withLimit(final int limit) {
                this.limit = limit;
                attributeFlags.add(AttributeFlag.LIMIT);
                return this;
            }

            public Attributes build() {
                var ret = new Attributes();

                ret.setMaxWorkRequests(maxWorkRequests);
                ret.setLimit(limit);

                return ret;
            }

            public AttributeFlag[] getAttributeFlags() {
                return attributeFlags.toArray(new AttributeFlag[0]);
            }
        }
    }

    @LinkNative(value = "ibv_srq_init_attr")
    public static final class InitialAttributes extends Struct {

        private final NativeLong userContext = longField("srq_context");

        public final Attributes attributes = valueField("attr", Attributes::new);

        InitialAttributes() {}

        public long getUserContext() {
            return userContext.get();
        }

        void setUserContext(long value) {
            this.userContext.set(value);
        }

        @Override
        public String toString() {
            return "InitialAttributes {" +
                "\n\tuserContext=" + userContext +
                ",\n\tattributes=" + attributes +
                "\n}";
        }

        public static final class Builder {

            private long userContext;

            // Attributes
            private int maxWorkRequests;
            private int maxScatterGatherElements;
            private int limit;

            public Builder(final int maxWorkRequests, final int maxScatterGatherElements) {
                this.maxWorkRequests = maxWorkRequests;
                this.maxScatterGatherElements = maxScatterGatherElements;
            }

            public Builder withLimit(final int limit) {
                this.limit = limit;
                return this;
            }

            public InitialAttributes build() {
                var ret = new InitialAttributes();

                ret.setUserContext(userContext);
                ret.attributes.setMaxWorkRequests(maxWorkRequests);
                ret.attributes.setMaxScatterGatherElements(maxScatterGatherElements);
                ret.attributes.setLimit(limit);

                return ret;
            }
        }
    }

    @LinkNative("ibv_srq_init_attr_ex")
    public static final class ExtendedInitialAttributes extends Struct {

        private final NativeLong userContext = longField("srq_context");
        private final NativeIntegerBitMask<ExtendedAttributeFlag> attributesMask = integerBitField("comp_mask");
        private final NativeEnum<Type> type = enumField("srq_type", Type.CONVERTER);
        private final NativeLong protectionDomain = longField("pd");
        private final NativeLong extendedConnectionDomain = longField("xrcd");
        private final NativeLong completionQueue = longField("cq");

        public final Attributes attributes = valueField("attr", Attributes::new);
        public final TagMatchingCapabilities tagMatchingCapabilities = valueField("tm_cap", TagMatchingCapabilities::new);

        ExtendedInitialAttributes() {}

        public long getUserContext() {
            return userContext.get();
        }

        public int getCompatibilityMask() {
            return attributesMask.get();
        }

        public Type getType() {
            return type.get();
        }

        public ProtectionDomain getProtectionDomain() {
            return NativeObjectRegistry.getObject(protectionDomain.get());
        }

        public ExtendedConnectionDomain getExtendedConnectionDomain() {
            return NativeObjectRegistry.getObject(extendedConnectionDomain.get());
        }

        public CompletionQueue getCompletionQueue() {
            return NativeObjectRegistry.getObject(completionQueue.get());
        }

        void setUserContext(final long value) {
            userContext.set(value);
        }

        void setAttributesMask(final ExtendedAttributeFlag... flags) {
            attributesMask.set(flags);
        }

        void setType(final Type value) {
            type.set(value);
        }

        void setProtectionDomain(final ProtectionDomain protectionDomain) {
            this.protectionDomain.set(protectionDomain.getHandle());
        }

        void setExtendedConnectionDomain(final ExtendedConnectionDomain extendedConnectionDomain) {
            this.extendedConnectionDomain.set(extendedConnectionDomain.getHandle());
        }

        void setCompletionQueue(final CompletionQueue completionQueue) {
            this.completionQueue.set(completionQueue.getHandle());
        }

        @Override
        public String toString() {
            return "ExtendedInitialAttributes {" +
                "\n\tuserContext=" + userContext +
                ",\n\tattributesMask=" + attributesMask +
                ",\n\ttype=" + type +
                ",\n\tprotectionDomain=" + protectionDomain +
                ",\n\textendedConnectionDomain=" + extendedConnectionDomain +
                ",\n\tcompletionQueue=" + completionQueue +
                ",\n\tattributes=" + attributes +
                ",\n\ttagMatchingCapabilities=" + tagMatchingCapabilities +
                "\n}";
        }

        public static final class Builder {

            private long userContext;
            private Set<ExtendedAttributeFlag> attributeFlags = new HashSet<>();
            private Type type;
            private ProtectionDomain protectionDomain;
            private ExtendedConnectionDomain extendedConnectionDomain;
            private CompletionQueue completionQueue;

            // Traditional Attributes
            private int maxWorkRequests;
            private int maxScatterGatherElements;
            private int limit;

            // Tag Matching Capabilities
            private int maxTags;
            private int maxOperations;

            public Builder(final ProtectionDomain protectionDomain, final int maxWorkRequests, final int maxScatterGatherElements) {
                this.protectionDomain = protectionDomain;
                this.maxWorkRequests = maxWorkRequests;
                this.maxScatterGatherElements = maxScatterGatherElements;

                attributeFlags.add(ExtendedAttributeFlag.PD);
            }

            public Builder withUserContext(final long userContext) {
                this.userContext = userContext;
                return this;
            }

            public Builder withType(final Type type) {
                this.type = type;
                attributeFlags.add(ExtendedAttributeFlag.TYPE);
                return this;
            }

            public Builder withExtendedConnectionDomain(final ExtendedConnectionDomain extendedConnectionDomain) {
                this.extendedConnectionDomain = extendedConnectionDomain;
                attributeFlags.add(ExtendedAttributeFlag.XRCD);
                return this;
            }

            public Builder withCompletionQueue(final CompletionQueue completionQueue) {
                this.completionQueue = completionQueue;
                attributeFlags.add(ExtendedAttributeFlag.CQ);
                return this;
            }

            public Builder withLimit(final int limit) {
                this.limit = limit;
                return this;
            }

            public Builder withMaxTags(final int maxTags) {
                this.maxTags = maxTags;
                attributeFlags.add(ExtendedAttributeFlag.TM);
                return this;
            }

            public Builder withMaxOperations(final int maxOperations) {
                this.maxOperations = maxOperations;
                attributeFlags.add(ExtendedAttributeFlag.TM);
                return this;
            }

            public ExtendedInitialAttributes build() {
                var ret = new ExtendedInitialAttributes();

                ret.setUserContext(userContext);
                ret.setAttributesMask(attributeFlags.toArray(new ExtendedAttributeFlag[0]));

                if(type != null) ret.setType(type);
                if(protectionDomain != null) ret.setProtectionDomain(protectionDomain);
                if(extendedConnectionDomain != null) ret.setExtendedConnectionDomain(extendedConnectionDomain);
                if(completionQueue != null) ret.setCompletionQueue(completionQueue);

                ret.attributes.setMaxWorkRequests(maxWorkRequests);
                ret.attributes.setMaxScatterGatherElements(maxScatterGatherElements);
                ret.attributes.setLimit(limit);

                ret.tagMatchingCapabilities.setMaxOperations(maxOperations);
                ret.tagMatchingCapabilities.setMaxTags(maxTags);

                return ret;
            }
        }
    }

    @LinkNative("ibv_tm_cap")
    public static final class TagMatchingCapabilities extends Struct {

        private final NativeInteger maxTags = integerField("max_num_tags");
        private final NativeInteger maxOperations = integerField("max_ops");

        TagMatchingCapabilities(LocalBuffer buffer, long offset) {
            super(buffer, offset);
        }

        public int getMaxTags() {
            return maxTags.get();
        }

        public int getMaxOperations() {
            return maxOperations.get();
        }

        void setMaxTags(final int value) {
            maxTags.set(value);
        }

        void setMaxOperations(final int value) {
            maxOperations.set(value);
        }

        @Override
        public String toString() {
            return "TagMatchingCapabilities {" +
                "\n\tmaxTags=" + maxTags +
                ",\n\tmaxOperations=" + maxOperations +
                "\n}";
        }
    }
}
