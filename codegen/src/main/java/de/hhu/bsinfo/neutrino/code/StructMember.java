package de.hhu.bsinfo.neutrino.code;

public class StructMember {

    private final String name;
    private final String type;
    private final String pointer;
    private final String size;
    private final String special;

    public StructMember(String name, String type, String pointer, String size, String special) {
        this.name = name;
        this.type = type;
        this.pointer = pointer;
        this.size = size;
        this.special = special;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isPointer() {
        return !pointer.isBlank();
    }

    public boolean isArray() {
        return !size.isBlank();
    }

    public int getArraySize() {
        String result = size.replace("[", "").replace("]", "");
        if (result.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(result);
    }

    public boolean isEnum() {
        return "enum".equals(special);
    }

    public boolean isStruct() {
        return "struct".equals(special);
    }

    @Override
    public String toString() {
        return "StructMember {" +
            "\n\tname='" + name + '\'' +
            ",\n\ttype='" + type + '\'' +
            ",\n\tpointer='" + pointer + '\'' +
            ",\n\tsize='" + size + '\'' +
            ",\n\tspecial='" + special + '\'' +
            "\n}";
    }
}
