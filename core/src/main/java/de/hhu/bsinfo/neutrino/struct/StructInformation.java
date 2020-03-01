package de.hhu.bsinfo.neutrino.struct;

import de.hhu.bsinfo.neutrino.buffer.LocalBuffer;
import de.hhu.bsinfo.neutrino.data.NativeInteger;
import de.hhu.bsinfo.neutrino.data.NativeLong;
import de.hhu.bsinfo.neutrino.data.NativeObject;
import de.hhu.bsinfo.neutrino.util.CustomStruct;
import de.hhu.bsinfo.neutrino.util.MemoryUtil;
import de.hhu.bsinfo.neutrino.util.StructUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.stream.Collectors;

public class StructInformation implements NativeObject {

    public static final int SIZE = 16;

    private final LocalBuffer buffer;
    public final NativeInteger size;
    public final NativeInteger memberCount;
    public final NativeLong memberInfoHandle;
    private final Map<String, Integer> offsetInfoMap;

    public StructInformation(CustomStruct annotation) {
        buffer = LocalBuffer.allocate(SIZE);
        size = new NativeInteger(buffer, 0);
        memberCount = new NativeInteger(buffer, 4);
        memberInfoHandle = new NativeLong(buffer, 8);
        offsetInfoMap = null;

        size.set(annotation.value());
    }

    public StructInformation(long handle) {
        buffer = LocalBuffer.wrap(handle, SIZE);
        size = new NativeInteger(buffer, 0);
        memberCount = new NativeInteger(buffer, 4);
        memberInfoHandle = new NativeLong(buffer, 8);
        offsetInfoMap = StructUtil.wrap(
                            MemberInformation::new,
                            memberInfoHandle.get(),
                            MemberInformation.SIZE,
                            memberCount.get())
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(
                            MemberInformation::getName,
                            MemberInformation::getOffset));
    }

    public int getOffset(String memberName) {
        if (offsetInfoMap.containsKey(memberName)) {
            return offsetInfoMap.get(memberName);
        }

        throw new IllegalArgumentException(String.format("No member found with name %s", memberName));
    }

    public int getSize() {
        return size.get();
    }

    @Override
    public long getHandle() {
        return 0;
    }

    @Override
    public long getNativeSize() {
        return SIZE;
    }
}
