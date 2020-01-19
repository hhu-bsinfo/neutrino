package de.hhu.bsinfo.neutrino.example;

import de.hhu.bsinfo.neutrino.example.command.Root;
import de.hhu.bsinfo.neutrino.example.util.InetSocketAddressConverter;
import java.net.InetSocketAddress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import picocli.CommandLine;

@SpringBootApplication(scanBasePackages = "de.hhu.bsinfo.neutrino")
@ComponentScan(basePackages = "de.hhu.bsinfo.neutrino",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "de\\.hhu\\.bsinfo\\.neutrino\\.example\\.service\\..*"))
public class App {

    @SuppressWarnings("CallToSystemExit")
    public static void main(String... args) {
        System.exit(SpringApplication.exit(SpringApplication.run(App.class, args)));
    }
}
