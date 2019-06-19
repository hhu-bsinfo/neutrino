package de.hhu.bsinfo.neutrino.example.util;

public class Result {

    private static final char[] metricTable = {
            ' ',
            'k',
            'm',
            'g',
            't',
            'p',
            'e'
    };

    private final long operationCount;
    private final long operationSize;
    private final double totalTime;

    private final long totalData;
    private final double operationThroughput;
    private final double dataThroughput;

    public Result(long operationCount, long operationSize, long timeNanos) {
        this.operationCount = operationCount;
        this.operationSize = operationSize;
        this.totalTime = timeNanos / 1000000000d;

        totalData = operationCount * operationSize;
        operationThroughput = (double) operationCount / totalTime;
        dataThroughput = (double) totalData / totalTime;
    }

    private String getFormattedValue(String name, double value, String unit) {
        double formattedValue = value;

        int counter = 0;
        while (formattedValue > 1000 && counter < metricTable.length) {
            formattedValue = formattedValue / 1000;
            counter++;
        }

        if(value == (long) value) {
            return String.format("%-20s %.3f %c%s (%d)", name + ":", formattedValue, metricTable[counter], unit, (long) value);
        }

        return String.format("%-20s %.3f %c%s (%f)", name + ":", formattedValue, metricTable[counter], unit, value);
    }

    private String getFormattedValue(String name, double value) {
        return getFormattedValue(name, value, "Units");
    }

    @Override
    public String toString() {
        return "Result {\n" +
                "\t" + getFormattedValue("operationCount", operationCount) +
                ",\n\t" + getFormattedValue("operationSize", operationSize, "Byte") +
                ",\n\t" + getFormattedValue("totalTime", totalTime, "s") +
                ",\n\t" + getFormattedValue("totalData", totalData, "Byte") +
                ",\n\t" + getFormattedValue("operationThroughput", operationThroughput, "Operations/s") +
                ",\n\t" + getFormattedValue("dataThroughput", dataThroughput, "Byte/s") +
                "\n}";
    }
}
