package de.hhu.bsinfo.neutrino.example.command.arrow;

import de.hhu.bsinfo.neutrino.example.util.PhoneBook;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.RootAllocator;
import picocli.CommandLine;

import java.io.File;

@Slf4j
@CommandLine.Command(
        name = "datagen",
        description = "Generates sample data for arrow benchmarks.%n",
        showDefaultValues = true,
        separator = " ")
public class DataGenerator implements Runnable {

    @CommandLine.Option(
            names = {"-r", "--rows"},
            description = "The number of rows to generate.",
            required = true)
    private int rows;

    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "The output file.",
            required = true)
    private File output;

    @Override
    public void run() {
//        var allocator = new RootAllocator(Integer.MAX_VALUE);
//        var phoneBook = PhoneBook.create(allocator, rows);
//        phoneBook.writeTo(output);
    }
}
