package de.hhu.bsinfo.neutrino.api.network;

public interface NetworkConfiguration {

    /**
     * The maximum transmission unit used for new connections.
     */
    int getMtu();

    /**
     * The shared receive queue's size.
     */
    int getSharedReceiveQueueSize();

    /**
     * The maximum number of scatter-gather elements per work request.
     */
    int getMaxScatterGatherElements();

    /**
     * The maximum number of completion events within a completion queue.
     */
    int getCompletionQueueSize();

    /**
     * The maximum number of work requests within a queue pair.
     */
    int getQueuePairSize();

    /**
     * The minimal delay after a message could not be received due to missing
     * work requests on the receive queue.
     *
     * 0  - 655.36 milliseconds
     * 1  -   0.01 milliseconds
     * 2  -   0.02 milliseconds
     * 3  -   0.03 milliseconds
     * 4  -   0.04 milliseconds
     * 5  -   0.06 milliseconds
     * 6  -   0.08 milliseconds
     * 7  -   0.12 milliseconds
     * 8  -   0.16 milliseconds
     * 9  -   0.24 milliseconds
     * 10 -   0.32 milliseconds
     * 11 -   0.48 milliseconds
     * 12 -   0.64 milliseconds
     * 13 -   0.96 milliseconds
     * 14 -   1.28 milliseconds
     * 15 -   1.92 milliseconds
     * 16 -   2.56 milliseconds
     * 17 -   3.84 milliseconds
     * 18 -   5.12 milliseconds
     * 19 -   7.68 milliseconds
     * 20 -   10.24 milliseconds
     * 21 -   15.36 milliseconds
     * 22 -   20.48 milliseconds
     * 23 -   30.72 milliseconds
     * 24 -   40.96 milliseconds
     * 25 -   61.44 milliseconds
     * 26 -   81.92 milliseconds
     * 27 -  122.88 milliseconds
     * 28 -  163.84 milliseconds
     * 29 -  245.76 milliseconds
     * 30 -  327.68 milliseconds
     * 31 -  491.52 milliseconds
     */
    byte getRnrTimer();

    /**
     * The total number of times a queue pair will try to resend a message due
     * to the remote side not being able to handle the message.
     *
     * 3 bit value
     */
    byte getRnrRetryCount();

    /**
     * The service level.
     */
    byte getServiceLevel();

    /**
     * The minmal delay a queue pair waits for an ACK/NACK before
     * resending the message.
     *
     * 0  -   infinite
     * 1  -      8.192 usec (0.000008 sec)
     * 2  -     16.384 usec (0.000016 sec)
     * 3  -     32.768 usec (0.000032 sec)
     * 4  -     65.536 usec (0.000065 sec)
     * 5  -    131.072 usec (0.000131 sec)
     * 6  -    262.144 usec (0.000262 sec)
     * 7  -    524.288 usec (0.000524 sec)
     * 8  -   1048.576 usec (0.00104 sec)
     * 9  -   2097.152 usec (0.00209 sec)
     * 10 -   4194.304 usec (0.00419 sec)
     * 11 -   8388.608 usec (0.00838 sec)
     * 12 -   16777.22 usec (0.01677 sec)
     * 13 -   33554.43 usec (0.0335 sec)
     * 14 -   67108.86 usec (0.0671 sec)
     * 15 -   134217.7 usec (0.134 sec)
     * 16 -   268435.5 usec (0.268 sec)
     * 17 -   536870.9 usec (0.536 sec)
     * 18 -    1073742 usec (1.07 sec)
     * 19 -    2147484 usec (2.14 sec)
     * 20 -    4294967 usec (4.29 sec)
     * 21 -    8589935 usec (8.58 sec)
     * 22 -   17179869 usec (17.1 sec)
     * 23 -   34359738 usec (34.3 sec)
     * 24 -   68719477 usec (68.7 sec)
     * 25 -  137000000 usec (137 sec)
     * 26 -  275000000 usec (275 sec)
     * 27 -  550000000 usec (550 sec)
     * 28 - 1100000000 usec (1100 sec)
     * 29 - 2200000000 usec (2200 sec)
     * 30 - 4400000000 usec (4400 sec)
     * 31 - 8800000000 usec (8800 sec)
     */
    byte getRetryCount();

    /**
     * The total number of times a queue pair will try to resend a message due
     * to the remote side not answering.
     *
     * 3 bit value
     */
    byte getTimeout();

    /**
     * The number of buffers pooled for network operations.
     */
    int getPoolSize();
}
