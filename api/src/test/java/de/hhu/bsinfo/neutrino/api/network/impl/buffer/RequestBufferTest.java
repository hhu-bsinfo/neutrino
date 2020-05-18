package de.hhu.bsinfo.neutrino.api.network.impl.buffer;

import de.hhu.bsinfo.neutrino.api.network.impl.accessor.ScatterGatherAccessor;
import de.hhu.bsinfo.neutrino.api.network.impl.accessor.SendRequestAccessor;
import de.hhu.bsinfo.neutrino.util.MemoryAlignment;
import de.hhu.bsinfo.neutrino.verbs.SendWorkRequest;
import org.agrona.BitUtil;
import org.junit.jupiter.api.Test;

import static org.agrona.concurrent.ringbuffer.RecordDescriptor.ALIGNMENT;
import static org.agrona.concurrent.ringbuffer.RecordDescriptor.HEADER_LENGTH;
import static org.assertj.core.api.Assertions.assertThat;

class RequestBufferTest {

    @Test
    public void testReadWrite() {
        var buffer = new RequestBuffer(MemoryAlignment.PAGE.value());

        var index = buffer.tryClaim(1);
        var requestHandle = buffer.memoryAddress() + index;

        SendRequestAccessor.setId(requestHandle, 42);

        buffer.commitWrite(index);

        var request = new SendWorkRequest();
        var bytes = buffer.read((type, data, offset, length) -> {
            request.wrap(data.addressOffset() + offset);
            assertThat(request.getId()).isEqualTo(42);
        }, 1);

        var expectedSize = BitUtil.align(HEADER_LENGTH + SendRequestAccessor.ELEMENT_SIZE + ScatterGatherAccessor.ELEMENT_SIZE, ALIGNMENT);
        assertThat(buffer.size()).isEqualTo(expectedSize);

        buffer.commitRead(bytes);
        assertThat(buffer.size()).isEqualTo(0);
    }
}