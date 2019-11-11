package de.hhu.bsinfo.neutrino.code;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class StructGenerator {

    private static final Pattern STRUCT_PATTERN = Pattern.compile("struct\\s+(\\w+)\\s+\\{\\s+(.*?)\\s+^};", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern STRUCT_MEMBER_PATTERN = Pattern.compile("(?<special>enum|struct)?\\s*(?<type>\\w+)\\s+(?<pointer>\\**)(?<name>\\w+)(?<size>\\[\\d*])?;", Pattern.MULTILINE);

    public static void main(String... args) throws Exception {

        String content = Files.readString(Paths.get("/usr/include/infiniband/verbs.h"))
                + Files.readString(Paths.get("/usr/include/infiniband/mlx5dv.h"));

        var structs = getStructs(content);

        structs.forEach((key, value) -> {
            try {
                System.out.println(generateClass(key, value));
                System.out.println(NativeMapGenerator.generate(key, value));
            } catch (Exception e) {
                System.err.println("Unable to generate code for '" + key + "'");
            }
        });
    }

    private static Map<String, List<StructMember>> getStructs(final String fileContent) {
        var matcher = STRUCT_PATTERN.matcher(fileContent);
        var map = new HashMap<String, List<StructMember>>();

        while (matcher.find()) {
            var memberMatcher = STRUCT_MEMBER_PATTERN.matcher(matcher.group(2));
            var memberList = new ArrayList<StructMember>();
            while (memberMatcher.find()) {
                memberList.add(new StructMember(
                    memberMatcher.group("name"),
                    memberMatcher.group("type"),
                    memberMatcher.group("pointer"),
                    memberMatcher.group("size"),
                    memberMatcher.group("special")));
            }
            map.put(matcher.group(1), memberList);
        }

        return map;
    }

    private static String generateClass(String structName, List<StructMember> members) {

        TypeSpec typeSpec = StructDefinition.generate(structName, members);

        JavaFile javaFile = JavaFile.builder("de.hhu.bsinfo.neutrino.generated", typeSpec)
            .build();

        return javaFile.toString();
    }



}
