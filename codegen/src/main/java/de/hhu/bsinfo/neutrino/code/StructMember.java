package de.hhu.bsinfo.neutrino.code;

public class StructMember {

    private final String name;
    private final String type;

    public StructMember(String type, String name) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "StructMember {" +
            "\n\tname='" + name + '\'' +
            ",\n\ttype='" + type + '\'' +
            "\n}";
    }

}
