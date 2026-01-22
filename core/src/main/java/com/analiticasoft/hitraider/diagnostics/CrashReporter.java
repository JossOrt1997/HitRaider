package com.analiticasoft.hitraider.diagnostics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class CrashReporter {

    private final File outDir;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public CrashReporter(File outDir) {
        this.outDir = outDir;
        if (!outDir.exists()) outDir.mkdirs();
    }

    public File writeReport(Throwable t, CrashContext ctx) {
        String ts = fmt.format(new Date());
        File f = new File(outDir, "crash_report_" + ts + ".txt");

        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write("=== HIT-RAIDER CRASH REPORT ===\n");
            w.write("Timestamp: " + ts + "\n\n");

            w.write("--- System ---\n");
            w.write("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n");
            w.write("Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n");
            w.write("Arch: " + System.getProperty("os.arch") + "\n\n");

            w.write("--- Context ---\n");
            if (ctx != null) {
                for (var e : ctx.data().entrySet()) {
                    w.write(e.getKey() + ": " + e.getValue() + "\n");
                }
            } else {
                w.write("(no context)\n");
            }
            w.write("\n");

            w.write("--- Exception ---\n");
            w.write(t.toString() + "\n\n");
            w.write(stackTraceToString(t));
            w.write("\n");

            File hs = tryFindLatestHsErr();
            if (hs != null) {
                w.write("\n--- Native hs_err excerpt (" + hs.getName() + ") ---\n");
                w.write(readHead(hs, 200));
            }
        } catch (IOException ignored) {}

        return f;
    }

    private static String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static File tryFindLatestHsErr() {
        List<File> candidates = new ArrayList<>();
        String userDir = System.getProperty("user.dir");

        candidates.addAll(find(userDir));
        candidates.addAll(find(userDir + File.separator + "assets"));

        File best = null;
        long bestTime = -1;
        for (File f : candidates) {
            if (f.lastModified() > bestTime) {
                bestTime = f.lastModified();
                best = f;
            }
        }
        return best;
    }

    private static List<File> find(String dir) {
        File d = new File(dir);
        if (!d.exists() || !d.isDirectory()) return List.of();
        File[] files = d.listFiles((dd, name) -> name.startsWith("hs_err_pid") && name.endsWith(".log"));
        if (files == null) return List.of();
        return Arrays.asList(files);
    }

    private static String readHead(File f, int maxLines) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            int n = 0;
            while ((line = br.readLine()) != null && n++ < maxLines) {
                sb.append(line).append("\n");
            }
        } catch (IOException ignored) {}
        return sb.toString();
    }
}

