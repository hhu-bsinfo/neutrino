package de.hhu.bsinfo.neutrino.api.network.impl.event;

import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.Test;

class EventLoopGroupTest {

    @Test
    public void testSimpleGroup() throws InterruptedException {
        var group = new EventLoopGroup("test", () -> BusySpinIdleStrategy.INSTANCE);
        group.waitOnStart();

        for (int i = 0; i < group.size(); i++) {
            group.next().add(new NullAgent());
        }

        group.close();
        group.join();
    }

    @Slf4j
    private static final class NullAgent implements Agent {

        private boolean logged;

        @Override
        public int doWork() {
            if (!logged) {
                log.debug("Started doing work");
                logged = true;
            }

            return 0;
        }

        @Override
        public String roleName() {
            return "null";
        }
    }

}