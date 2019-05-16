#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import de.hhu.bsinfo.neutrino.struct.Struct;
import de.hhu.bsinfo.neutrino.struct.StructInformation;
import de.hhu.bsinfo.neutrino.util.StructUtil;
    
public class ${NAME} extends Struct {

    private static final StructInformation INFO = StructUtil.getInfo("${STRUCT_NAME}");
    public static final int SIZE = INFO.structSize.get();
    
    public ${NAME}() {
        super(SIZE);
    }
    
    public ${NAME}(long handle) {
        super(handle, SIZE);
    }
}