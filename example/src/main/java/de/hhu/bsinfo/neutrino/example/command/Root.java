package de.hhu.bsinfo.neutrino.example.command;


import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@CommandLine.Command(
    name = "neutrino",
    description = "",
    subcommands = { Start.class, MessagingTest.class }
)
public class Root implements Runnable {

    public static void printBanner() {
        InputStream inputStream = Root.class.getClassLoader().getResourceAsStream("banner.txt");

        if (inputStream == null) {
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String banner = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        System.out.println(banner);
    }

    @Override
    public void run() {}
}