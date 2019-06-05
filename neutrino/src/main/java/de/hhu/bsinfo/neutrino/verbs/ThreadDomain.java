package de.hhu.bsinfo.neutrino.verbs;

import de.hhu.bsinfo.neutrino.data.NativeInteger;
import de.hhu.bsinfo.neutrino.struct.Result;
import de.hhu.bsinfo.neutrino.struct.Struct;
import de.hhu.bsinfo.neutrino.util.LinkNative;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LinkNative("ibv_td")
public class ThreadDomain extends Struct implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectionDomain.class);

    private final Context context = referenceField("context", Context::new);

    ThreadDomain(final long handle) {
        super(handle);
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void close() {
        var result = (Result) Verbs.getPoolableInstance(Result.class);

        Verbs.deallocateThreadDomain(getHandle(), result.getHandle());
        if (result.isError()) {
            LOGGER.error("Closing thread domain failed with error [{}]", result.getStatus());
        }

        result.releaseInstance();
    }

    @LinkNative("ibv_td_init_attr")
    public static final class InitialAttributes extends Struct {

        private final NativeInteger compatibilityMask = integerField("comp_mask");

        public InitialAttributes() {}

        public InitialAttributes(final Consumer<InitialAttributes> configurator) {
            configurator.accept(this);
        }

        public int getCompatibilityMask() {
            return compatibilityMask.get();
        }

        public void setCompatibilityMask(final int value) {
            compatibilityMask.set(value);
        }
    }
}
