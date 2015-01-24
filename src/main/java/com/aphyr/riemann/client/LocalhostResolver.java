package com.aphyr.riemann.client;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A "Smarter" localhost resolver
 * see issue: https://github.com/aphyr/riemann-java-client/issues/44
 * Trying to avoid a lot of calls to java.net.InetAddress.getLocalHost()
 * which under AWS trigger DNS resolving and have relatively high latency *per event*
 * usually, the hostname doesn't change so often to warrant a real query.
 *
 * A real call to java.net.InetAddress.getLocalHost().getHostName()
 * is made only if:
 * 1) the refresh interval has passed (=result is stale)
 * AND
 * 2) no env vars that identify the hostname are found
 */
public class LocalhostResolver {

    // default hostname env var names on Win/Nix
    public static final String COMPUTERNAME = "COMPUTERNAME"; // Windows
    public static final String HOSTNAME = "HOSTNAME"; // Nix

    // how often should we refresh the cached hostname
    private static volatile long refreshIntervalMillis = 60 * 1000;
    public static void setRefreshIntervalMillis(long intervalMS) {
        refreshIntervalMillis = intervalMS;
        startUpdateTimer();
    }


    // the cached resolved hostname
    // our timer thread is the only writer of this variable
    private static volatile String hostname;

    // periodically updates the hostname
    // will run as daemon thread
    private static volatile Timer timer;

    // this is mostly for testing/monitoring
    private static volatile long lastUpdate = 0;
    public static long getLastUpdateTime() { return lastUpdate; }
    public static void setLastUpdateTime(long time) { lastUpdate = time; }

    static {
        resolveByEnv();
        if(hostname == null) {
            resolveNow(); // first without delay
            startUpdateTimer();
        }
    }

    /**
     * starts the timer thread to resolve hostname every interval
     */
    private static void startUpdateTimer() {
        if(timer != null) {
            System.out.println("startUpdateTimer: timer.cancel()");
            timer.cancel();
        }
        System.out.println("startUpdateTimer: interval="+refreshIntervalMillis);
        System.out.println("startUpdateTimer: now="+System.currentTimeMillis());

        // will run as daemon thread
        timer = new Timer("LocalhostResolver", true);
        // not fixed-rate, delay interval from last refresh
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                refreshResolve();
                System.out.println(String.format("resolveTimer[%d]:%s", lastUpdate, hostname));
            }
        }, 0, refreshIntervalMillis);
    }

    /**
     * get resolved hostname.
     *
     * @return the hostname
     */
    public static String getResolvedHostname() {
        return hostname;
    }

    public static void resolveNow() {
        System.out.println("resolveNow");
        if(timer != null) {
            System.out.println("resolveNow: timer.schedule()");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    refreshResolve();
                    System.out.println(String.format("resolveNow[%d]:%s", lastUpdate, hostname));
                }
            }, 0);
        }
    }

    /**
     * forces a new resolve
     */
    private static void refreshResolve() {
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
            if (hostname == null) {
                hostname = "localhost";
            }
            lastUpdate = System.currentTimeMillis();
        } catch (UnknownHostException e) {
            // fallthrough
        }
    }

    /**
     * try to resolve the hostname by env vars
     *
     */
    public static void resolveByEnv() {
        String var;
        if(System.getProperty("os.name").startsWith("Windows")) {
            var =  System.getenv(COMPUTERNAME);
            if(var == null) {
                var  = "localhost";
            }
        }
        else {
            var = System.getenv(HOSTNAME);
        }

        hostname = var;
    }
}
