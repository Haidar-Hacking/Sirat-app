package com.haydar.raqeeb;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * التلاوات المحفوظة للاستماع بدون إنترنت.
 * ينزّل ملف الصوت مرة واحدة إلى ذاكرة التطبيق، وبعدها كل طلب لنفس الرابط يُخدَم من الملف المحفوظ.
 */
class OfflineAudio {
    interface Listener { void progress(String url, int percent, boolean done, String error); }

    private static final Set<String> RUNNING = new HashSet<>();

    static boolean isAudioHost(String host) {
        if (host == null) return false;
        return host.endsWith("mp3quran.net") || host.endsWith("archive.org") || host.endsWith("everyayah.com");
    }

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "audio");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    static String key(String url) {
        String u = Uri.decode(url);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest(u.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(u.hashCode());
        }
    }

    static File file(Context c, String url) {
        return new File(dir(c), key(url) + ".mp3");
    }

    static boolean has(Context c, String url) {
        File f = file(c, url);
        return f.exists() && f.length() > 0;
    }

    static long totalSize(Context c) {
        long n = 0;
        File[] fs = dir(c).listFiles();
        if (fs != null) for (File f : fs) n += f.length();
        return n;
    }

    static int count(Context c) {
        int n = 0;
        File[] fs = dir(c).listFiles();
        if (fs != null) for (File f : fs) if (f.getName().endsWith(".mp3")) n++;
        return n;
    }

    static void clear(Context c) {
        File[] fs = dir(c).listFiles();
        if (fs != null) for (File f : fs) f.delete();
    }

    static void delete(Context c, String url) {
        file(c, url).delete();
    }

    static void download(final Context c, final String url, final Listener l) {
        synchronized (RUNNING) {
            if (RUNNING.contains(url)) return;
            RUNNING.add(url);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                File out = file(c, url);
                File part = new File(out.getPath() + ".part");
                HttpURLConnection con = null;
                try {
                    URL u = new URL(url);
                    int hops = 0;
                    while (true) {
                        con = (HttpURLConnection) u.openConnection();
                        con.setInstanceFollowRedirects(false);
                        con.setConnectTimeout(20000);
                        con.setReadTimeout(30000);
                        con.setRequestProperty("User-Agent", "SiratAlMuminin/Android");
                        int code = con.getResponseCode();
                        if (code >= 300 && code < 400 && hops < 6) {
                            String loc = con.getHeaderField("Location");
                            con.disconnect();
                            if (loc == null) throw new IOException("redirect");
                            u = new URL(u, loc);
                            hops++;
                            continue;
                        }
                        if (code != 200) throw new IOException("HTTP " + code);
                        break;
                    }
                    long len = con.getContentLength();
                    InputStream in = con.getInputStream();
                    FileOutputStream fo = new FileOutputStream(part);
                    byte[] buf = new byte[65536];
                    long got = 0;
                    int n, last = -1;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        got += n;
                        if (len > 0) {
                            int p = (int) (got * 100 / len);
                            if (p != last && p % 2 == 0) { last = p; l.progress(url, p, false, null); }
                        }
                    }
                    fo.close();
                    in.close();
                    if (got < 1024) throw new IOException("empty");
                    if (out.exists()) out.delete();
                    if (!part.renameTo(out)) throw new IOException("rename");
                    l.progress(url, 100, true, null);
                } catch (Exception e) {
                    part.delete();
                    l.progress(url, 0, false, String.valueOf(e.getMessage()));
                } finally {
                    if (con != null) try { con.disconnect(); } catch (Exception ignored) {}
                    synchronized (RUNNING) { RUNNING.remove(url); }
                }
            }
        }).start();
    }

    private static final Pattern RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");

    /** يخدم الملف المحفوظ، مع دعم طلب جزء من الملف (للتقديم والتأخير في التلاوة). */
    static WebResourceResponse serve(Context c, String url, Map<String, String> reqHeaders) {
        File f = file(c, url);
        if (!f.exists() || f.length() == 0) return null;
        try {
            long size = f.length();
            String range = null;
            if (reqHeaders != null) {
                for (Map.Entry<String, String> h : reqHeaders.entrySet()) {
                    if ("range".equalsIgnoreCase(h.getKey())) range = h.getValue();
                }
            }
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept-Ranges", "bytes");
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Cache-Control", "no-cache");
            if (range != null) {
                Matcher m = RANGE.matcher(range);
                if (m.find()) {
                    long start = m.group(1).isEmpty() ? 0 : Long.parseLong(m.group(1));
                    long end = m.group(2).isEmpty() ? size - 1 : Math.min(Long.parseLong(m.group(2)), size - 1);
                    if (m.group(1).isEmpty() && !m.group(2).isEmpty()) {
                        start = Math.max(0, size - Long.parseLong(m.group(2)));
                        end = size - 1;
                    }
                    if (start <= end && start < size) {
                        FileInputStream in = new FileInputStream(f);
                        long skipped = 0;
                        while (skipped < start) {
                            long s = in.skip(start - skipped);
                            if (s <= 0) break;
                            skipped += s;
                        }
                        long n = end - start + 1;
                        headers.put("Content-Range", "bytes " + start + "-" + end + "/" + size);
                        headers.put("Content-Length", String.valueOf(n));
                        return new WebResourceResponse("audio/mpeg", null, 206, "Partial Content", headers, new Limited(in, n));
                    }
                }
            }
            headers.put("Content-Length", String.valueOf(size));
            return new WebResourceResponse("audio/mpeg", null, 200, "OK", headers, new FileInputStream(f));
        } catch (Exception e) {
            return null;
        }
    }

    private static class Limited extends FilterInputStream {
        private long left;
        Limited(InputStream in, long n) { super(in); left = n; }
        @Override public int read() throws IOException {
            if (left <= 0) return -1;
            int b = super.read();
            if (b >= 0) left--;
            return b;
        }
        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (left <= 0) return -1;
            int n = super.read(b, off, (int) Math.min(len, left));
            if (n > 0) left -= n;
            return n;
        }
    }
}
