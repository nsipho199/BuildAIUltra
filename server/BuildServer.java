import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class BuildServer {

    static final String API_KEY = System.getenv().getOrDefault("OPENROUTER_API_KEY", "");
    static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    static final String HOST = System.getenv().getOrDefault("HOST", "0.0.0.0");
    static final Path TEMPLATE_DIR = Paths.get(System.getenv().getOrDefault(
        "TEMPLATE_DIR", Paths.get(System.getProperty("user.dir"), "templates/base").toString()));
    static final Path BUILDS_DIR = Paths.get(System.getenv().getOrDefault(
        "BUILDS_DIR", Paths.get(System.getProperty("user.dir"), "builds").toString()));
    static final String BASE_URL = System.getenv().getOrDefault(
        "BASE_URL", "http://localhost:" + PORT);
    static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    static final String MODEL = System.getenv().getOrDefault("AI_MODEL", "openai/gpt-4o-mini");

    static final Map<String, BuildTask> tasks = new ConcurrentHashMap<>();
    static final ExecutorService buildPool = Executors.newFixedThreadPool(
        Integer.parseInt(System.getenv().getOrDefault("BUILD_THREADS", "2")));

    static HttpClient httpClient;

    public static void main(String[] args) throws Exception {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(120))
            .build();

        Files.createDirectories(BUILDS_DIR);

        if (API_KEY == null || API_KEY.isBlank()) {
            System.out.println("WARNING: No OPENROUTER_API_KEY environment variable set.");
            System.out.println("Get a free API key at https://openrouter.ai/keys (no credit card required).");
            System.out.println("Export it: export OPENROUTER_API_KEY=sk-or-v1-...\n");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
        server.createContext("/api/build", new BuildHandler());
        server.createContext("/api/build/", new BuildStatusHandler());
        server.createContext("/api/download/", new DownloadHandler());
        server.createContext("/api/health", h -> {
            String resp = "{\"status\":\"ok\",\"version\":\"1.0.0\"}";
            h.getResponseHeaders().add("Content-Type", "application/json");
            h.sendResponseHeaders(200, resp.getBytes().length);
            h.getResponseBody().write(resp.getBytes());
        });
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("BuildAI Ultra Server running on http://" + HOST + ":" + PORT);

        if (args.length > 0 && args[0].equals("--cli")) {
            if (args.length < 3) {
                System.out.println("Usage: --cli <idea> <output-dir>");
                return;
            }
            String idea = args[1];
            Path outDir = Paths.get(args[2]);
            cliBuild(idea, outDir);
        }
    }

    static void cliBuild(String idea, Path outDir) throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        BuildTask task = new BuildTask(id, idea);
        tasks.put(id, task);
        executeBuild(task);
        Path apk = findApk(task.buildDir);
        if (apk != null) {
            Files.createDirectories(outDir);
            Files.copy(apk, outDir.resolve("app-debug.apk"), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("APK generated: " + outDir.resolve("app-debug.apk"));
        } else {
            System.out.println("Build failed: " + task.error);
        }
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                String html = "<!DOCTYPE html><html><head>"
                    + "<meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<title>BuildAI Ultra</title>"
                    + "<style>body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;background:#f6f6fe;color:#1c1b1f}.container{max-width:500px;width:100%;padding:24px;text-align:center}h1{color:#6c63ff;font-size:28px;margin-bottom:24px}textarea{width:100%;height:250px;padding:16px;border:2px solid #6c63ff;border-radius:16px;font-size:16px;resize:vertical;box-sizing:border-box;outline:none}button{width:100%;padding:18px;background:#6c63ff;color:#fff;border:none;border-radius:16px;font-size:18px;font-weight:bold;cursor:pointer;margin-top:16px;display:flex;align-items:center;justify-content:center;gap:8px}button:hover{background:#5a52d5}button:disabled{opacity:0.6;cursor:not-allowed}#progress{display:none;margin-top:24px;text-align:left}#bar{width:100%;height:8px;background:#e8e0ff;border-radius:4px;overflow:hidden}#fill{height:100%;background:#6c63ff;border-radius:4px;transition:width 0.3s;width:0%}#status{margin-top:12px;font-size:14px;color:#666}.hidden{display:none !important}#result{margin-top:24px;text-align:center}#result h2{font-size:24px;color:#1c1b1f;margin-bottom:8px}#result a{display:block;margin-top:16px;padding:16px;background:#6c63ff;color:#fff;border-radius:16px;text-decoration:none;font-size:16px;font-weight:bold;text-align:center}</style>"
                    + "</head><body><div class='container'>"
                    + "<h1>BuildAI Ultra</h1>"
                    + "<textarea id='idea' placeholder='Describe the app you want to build...'></textarea>"
                    + "<button id='buildBtn' onclick='startBuild()'><svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2'><path d='M12 2l-2.09 2.09C9.33 5.17 9 5.8 9 6.47V9.5c0 1.1-.9 2-2 2H4.5c-.67 0-1.3.33-1.69.91L2.5 14.5l4.5-.5c.55 0 1 .45 1 1v4.5l2.09-2.09c.58-.58.91-1.21.91-1.88V14c0-1.1.9-2 2-2h2.5c.67 0 1.3-.33 1.69-.91L21.5 9.5l-4.5.5c-.55 0-1-.45-1-1V4.5l-2.09 2.09C13.67 7.17 13 6.84 13 6.17V2.5H12z'/></svg> BUILD THE APP</button>"
                    + "<div id='progress'><div id='bar'><div id='fill'></div></div><div id='status'>Starting...</div></div>"
                    + "<div id='result' class='hidden'><h2>App Ready</h2><p id='sizeInfo'></p><a id='downloadLink' href='#'>INSTALL APK</a></div>"
                    + "</div><script>let pollId;function startBuild(){const idea=document.getElementById('idea').value.trim();if(!idea)return alert('Please describe your app idea first.');const btn=document.getElementById('buildBtn');btn.disabled=true;document.getElementById('progress').style.display='block';document.getElementById('result').classList.add('hidden');fetch('/api/build',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({idea})}).then(r=>r.json()).then(data=>{pollId=data.build_id;poll()}).catch(e=>{alert('Error: '+e.message);btn.disabled=false})}function poll(){fetch('/api/build/'+pollId).then(r=>r.json()).then(data=>{document.getElementById('fill').style.width=data.progress+'%';document.getElementById('status').textContent=data.phase+' ('+data.progress+'%)';if(data.status==='COMPLETE'){const btn=document.getElementById('buildBtn');btn.disabled=false;document.getElementById('result').classList.remove('hidden');document.getElementById('downloadLink').href=data.download_url;if(data.apk_size){const s=data.apk_size;document.getElementById('sizeInfo').textContent='APK Size: '+(s<1024?s+' B':s<1048576?Math.floor(s/1024)+' KB':Math.floor(s/1048576)+' MB')}}else if(data.status==='FAILED'){document.getElementById('buildBtn').disabled=false;alert('Build failed: '+(data.error||'Unknown error'))}else{setTimeout(poll,1000)}}).catch(e=>{document.getElementById('buildBtn').disabled=false;alert('Error: '+e.message)})}</script>"
                    + "</body></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                ex.sendResponseHeaders(200, bytes.length);
                ex.getResponseBody().write(bytes);
            } else {
                String resp = "{\"error\":\"Not found\"}";
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(404, resp.getBytes().length);
                ex.getResponseBody().write(resp.getBytes());
            }
        }
    }

    static class BuildHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equals(ex.getRequestMethod())) {
                sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String idea = extractJsonField(body, "idea");
            if (idea == null || idea.isBlank()) {
                sendJson(ex, 400, "{\"error\":\"Idea is required\"}");
                return;
            }

            String id = UUID.randomUUID().toString().substring(0, 8);
            BuildTask task = new BuildTask(id, idea);
            tasks.put(id, task);

            buildPool.submit(() -> {
                try { executeBuild(task); }
                catch (Exception e) {
                    task.error = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    task.status = "FAILED";
                }
            });

            String resp = String.format(
                "{\"build_id\":\"%s\",\"status\":\"%s\",\"progress\":%d,\"phase\":\"%s\"}",
                id, task.status, task.progress, task.phase
            );
            sendJson(ex, 200, resp);
        }
    }

    static class BuildStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            String id = path.replace("/api/build/", "");
            if (id.contains("/")) id = id.substring(0, id.indexOf('/'));
            BuildTask task = tasks.get(id);
            if (task == null) {
                sendJson(ex, 404, "{\"error\":\"Build not found\"}");
                return;
            }
            sendJson(ex, 200, task.toJson());
        }
    }

    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            String id = path.replace("/api/download/", "");
            if (id.contains("/")) id = id.substring(0, id.indexOf('/'));
            BuildTask task = tasks.get(id);
            if (task == null) {
                sendJson(ex, 404, "{\"error\":\"Build not found\"}");
                return;
            }
            Path apk = findApk(task.buildDir);
            if (apk == null || !Files.exists(apk)) {
                sendJson(ex, 404, "{\"error\":\"APK not found\"}");
                return;
            }
            ex.getResponseHeaders().add("Content-Type", "application/vnd.android.package-archive");
            ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + id + ".apk\"");
            ex.sendResponseHeaders(200, Files.size(apk));
            Files.copy(apk, ex.getResponseBody());
        }
    }

    static void executeBuild(BuildTask task) throws Exception {
        task.status = "GENERATING";
        task.progress = 1;
        task.phase = "Analyzing your idea";
        Thread.sleep(500);

        task.progress = 3;
        task.phase = "Planning application architecture";
        Thread.sleep(300);

        task.progress = 5;
        String code = callOpenRouter(task.idea);
        task.progress = 40;
        task.phase = "Generating user interface";
        Thread.sleep(300);

        task.progress = 45;
        task.phase = "Generating app code";
        createProject(task, code);
        task.progress = 70;
        task.phase = "Creating database";
        Thread.sleep(200);

        task.progress = 75;
        task.phase = "Creating APIs";
        Thread.sleep(200);

        task.progress = 80;
        task.phase = "Creating navigation";
        Thread.sleep(200);

        task.progress = 85;
        task.phase = "Creating settings";
        Thread.sleep(200);

        task.progress = 90;
        task.phase = "Generating assets";
        Thread.sleep(200);

        task.progress = 93;
        task.phase = "Compiling APK";
        compileProject(task);

        task.progress = 100;
        task.status = "COMPLETE";
        task.phase = "App ready";
    }

    static String callOpenRouter(String idea) throws Exception {
        String systemPrompt = "You are an Android app generator. Generate a COMPLETE, COMPILABLE Android app in Kotlin. "
            + "CRITICAL: Do NOT use markdown, backticks, or code fences. Output ONLY the file markers and raw file content. "
            + "RULES: "
            + "1. File format: //=== FILE: relative/path/from/app/src/main === then file content. "
            + "2. Package: com.generated.app. "
            + "3. Use Material Design 3, ViewBinding, AndroidX, Kotlin Coroutines. "
            + "4. Include Light and Dark theme (themes.xml and values-night/themes.xml). "
            + "5. IMPLEMENT REAL FUNCTIONALITY - real working code with proper UI and logic. "
            + "6. Include a theme toggle button in the app UI to switch between light and dark mode. "
            + "7. For lists use RecyclerView. "
            + "8. Generate: Kotlin in java/com/generated/app/, layouts in res/layout/, resources in res/values/ and res/values-night/. "
            + "9. Do NOT generate: build.gradle, settings.gradle, gradle files, proguard, AndroidManifest. "
            + "10. In colors.xml define: primary, primary_dark, accent, background, surface, on_primary, on_background, on_surface, text_primary, text_secondary. "
            + "11. In themes.xml use ONLY: <item name=\"colorPrimary\">@color/primary</item> <item name=\"colorPrimaryDark\">@color/primary_dark</item> <item name=\"colorAccent\">@color/accent</item> <item name=\"android:colorBackground\">@color/background</item>. "
            + "12. Theme name: Theme.GeneratedApp. Parent: Theme.Material3.DayNight.NoActionBar. "
            + "13. Every XML must have <?xml version='1.0' encoding='utf-8'?> header. "
            + "14. values-night/colors.xml: background, surface, on_background, on_surface, text_primary, text_secondary. "
            + "Generate app for: " + idea;

        String userMsg = "Generate the complete Android app code for: " + idea;

        String requestBody = "{\"model\":\"" + MODEL + "\",\"messages\":["
            + "{\"role\":\"system\",\"content\":" + jsonString(systemPrompt) + "},"
            + "{\"role\":\"user\",\"content\":" + jsonString(userMsg) + "}"
            + "],\"temperature\":0.3}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OPENROUTER_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + API_KEY)
            .header("HTTP-Referer", "https://buildai.ultra")
            .header("X-Title", "BuildAI Ultra")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenRouter API error " + response.statusCode() + ": " + response.body());
        }

        String json = response.body();
        String content = extractContentFromResponse(json);
        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI returned empty response");
        }
        return content;
    }

    static String jsonString(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    static String extractContentFromResponse(String json) {
        try {
            int choicesIdx = json.indexOf("\"choices\"");
            if (choicesIdx < 0) return null;
            int msgIdx = json.indexOf("\"message\"", choicesIdx);
            if (msgIdx < 0) return null;
            int contIdx = json.indexOf("\"content\"", msgIdx);
            if (contIdx < 0) return null;
            contIdx = json.indexOf('"', contIdx + 10);
            if (contIdx < 0) return null;
            contIdx += 1;
            if (contIdx >= json.length()) return null;
            
            StringBuilder content = new StringBuilder();
            boolean escaped = false;
            for (int i = contIdx; i < json.length(); i++) {
                char c = json.charAt(i);
                if (escaped) {
                    if (c == 'n') content.append('\n');
                    else if (c == 't') content.append('\t');
                    else if (c == 'r') content.append('\r');
                    else if (c == '"') content.append('"');
                    else if (c == '\\') content.append('\\');
                    else { content.append('\\'); content.append(c); }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                } else {
                    content.append(c);
                }
            }
            return content.toString();
        } catch (Exception e) {
            return null;
        }
    }

    static void createProject(BuildTask task, String generatedCode) throws Exception {
        Path buildDir = BUILDS_DIR.resolve(task.id);
        task.buildDir = buildDir;
        copyTemplate(buildDir);
        Path appSrc = buildDir.resolve("app/src/main");

        String[] files = generatedCode.split("//=== FILE: ");
        for (String fileBlock : files) {
            if (fileBlock.isBlank()) continue;
            int newlineIdx = fileBlock.indexOf('\n');
            if (newlineIdx < 0) continue;
            String relPath = fileBlock.substring(0, newlineIdx).trim();
            if (relPath.endsWith("===")) {
                relPath = relPath.substring(0, relPath.length() - 3).trim();
            }
            String content = fileBlock.substring(newlineIdx + 1).trim();
            int endMarker = content.lastIndexOf("//===");
            if (endMarker >= 0) {
                content = content.substring(0, endMarker).trim();
            }
            content = content.replaceAll("(?m)^```[a-zA-Z0-9_-]*$", "").trim();

            Path targetFile = appSrc.resolve(relPath);
            Files.createDirectories(targetFile.getParent());
            Files.writeString(targetFile, content);
        }

        // Ensure Kotlin files have common needed imports
        Path javaDir = appSrc.resolve("java");
        if (Files.isDirectory(javaDir)) {
            try (Stream<Path> kfs = Files.walk(javaDir)) {
                kfs.filter(p -> p.toString().endsWith(".kt")).forEach(p -> {
                    try {
                        String kc = Files.readString(p);
                        String[][] neededImports = {
                            {"repeatOnLifecycle", "import androidx.lifecycle.repeatOnLifecycle"},
                            {"Lifecycle.State", "import androidx.lifecycle.Lifecycle"},
                        };
                        for (String[] imp : neededImports) {
                            if (kc.contains(imp[0]) && !kc.contains(imp[1])) {
                                int nl = kc.indexOf('\n');
                                if (nl > 0) kc = kc.substring(0, nl + 1) + "\n" + imp[1] + kc.substring(nl + 1);
                            }
                        }
                        Files.writeString(p, kc);
                    } catch (Exception e) {}
                });
            }
        }

        // Clean layout XML files - fix '?attr/color' references to '@color/color'
        Path layoutDir = appSrc.resolve("res/layout");
        if (Files.isDirectory(layoutDir)) {
            String[] colorNames = {"primary","primary_dark","accent","background","surface","on_primary","on_background","on_surface","text_primary","text_secondary"};
            try (Stream<Path> lfs = Files.list(layoutDir)) {
                lfs.filter(p -> p.toString().endsWith(".xml")).forEach(p -> {
                    try {
                        String xc = Files.readString(p);
                        for (String cn : colorNames) {
                            xc = xc.replace("?attr/" + cn, "@color/" + cn);
                        }
                        Files.writeString(p, xc);
                    } catch (Exception e) {}
                });
            }
        }

        // Clean generated themes.xml - remove invalid android: attributes
        for (String tp : new String[]{"res/values/themes.xml", "res/values-night/themes.xml"}) {
            Path tpPath = appSrc.resolve(tp);
            if (Files.exists(tpPath)) {
                String tc = Files.readString(tpPath);
                tc = tc.replaceAll("(?m)^[\\s]*<item name=\"android:(surface|onBackground|onSurface|onPrimary)\">[^<]*</item>[\\s]*$", "");
                Files.writeString(tpPath, tc);
            }
        }

        // Fix theme name in manifest to match generated theme
        Path themesPath = appSrc.resolve("res/values/themes.xml");
        if (Files.exists(themesPath)) {
            String themes = Files.readString(themesPath);
            int q1 = themes.indexOf("style name=\"");
            if (q1 >= 0) {
                q1 = themes.indexOf('"', q1 + 11) + 1;
                int q2 = themes.indexOf('"', q1);
                if (q2 > q1) {
                    String themeName = themes.substring(q1, q2);
                    Path mp = appSrc.resolve("AndroidManifest.xml");
                    if (Files.exists(mp)) {
                        String m = Files.readString(mp);
                        m = m.replaceAll("android:theme=\"@style/[^\"]+\"", "android:theme=\"@style/" + themeName + "\"");
                        Files.writeString(mp, m);
                    }
                }
            }
        }

        // Ensure AndroidManifest exists
        Path manifestPath = appSrc.resolve("AndroidManifest.xml");
        if (!Files.exists(manifestPath)) {
            Files.writeString(manifestPath, String.join("\n",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"",
                "    package=\"com.generated.app\">",
                "    <uses-permission android:name=\"android.permission.INTERNET\"/>",
                "    <application",
                "        android:allowBackup=\"true\"",
                "        android:label=\"@string/app_name\"",
                "        android:supportsRtl=\"true\"",
                "        android:theme=\"@style/Theme.GeneratedApp\">",
                "        <activity",
                "            android:name=\".MainActivity\"",
                "            android:exported=\"true\">",
                "            <intent-filter>",
                "                <action android:name=\"android.intent.action.MAIN\"/>",
                "                <category android:name=\"android.intent.category.LAUNCHER\"/>",
                "            </intent-filter>",
                "        </activity>",
                "    </application>",
                "</manifest>"));
        }
    }

    static void compileProject(BuildTask task) throws Exception {
        Path buildDir = task.buildDir;
        String gradlewPath = buildDir.resolve("gradlew").toAbsolutePath().toString();

        String javaHome = System.getenv().getOrDefault("JAVA_HOME", "/usr/lib/jvm/java-17-openjdk-arm64");
        String androidHome = System.getenv().getOrDefault("ANDROID_HOME", "/opt/android_sdk");

        ProcessBuilder pb = new ProcessBuilder(
            gradlewPath, "assembleDebug",
            "--no-daemon", "--no-build-cache"
        );
        pb.directory(buildDir.toFile());
        pb.environment().put("JAVA_HOME", javaHome);
        pb.environment().put("ANDROID_HOME", androidHome);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("BUILD SUCCESSFUL")) {
                    task.progress = 98;
                }
                if (line.contains("BUILD FAILED") || line.contains("ERROR")) {
                    task.error = task.error == null ? line : task.error + "\n" + line;
                    // Limit error length
                    if (task.error.length() > 2000) {
                        task.error = task.error.substring(0, 2000) + "...";
                    }
                }
            }
        }
        int exitCode = p.waitFor(10, TimeUnit.MINUTES) ? p.exitValue() : -1;
        if (exitCode != 0) {
            if (task.error == null) task.error = "Gradle build failed with exit code " + exitCode;
            task.status = "FAILED";
        }
    }

    static void copyTemplate(Path dest) throws Exception {
        if (!Files.exists(TEMPLATE_DIR)) {
            throw new RuntimeException("Template directory not found: " + TEMPLATE_DIR);
        }
        try (Stream<Path> stream = Files.walk(TEMPLATE_DIR)) {
            stream.forEach(src -> {
                try {
                    Path rel = TEMPLATE_DIR.relativize(src);
                    Path dst = dest.resolve(rel);
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dst);
                    } else {
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
    }

    static Path findApk(Path buildDir) {
        if (buildDir == null) return null;
        Path apk = buildDir.resolve("app/build/outputs/apk/debug/app-debug.apk");
        if (Files.exists(apk)) return apk;
        try (Stream<Path> stream = Files.walk(buildDir.resolve("app/build/outputs"))) {
            return stream.filter(p -> p.toString().endsWith(".apk"))
                .findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    static void sendJson(HttpExchange ex, int code, String body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
    }

    static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end)
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\\", "\\");
    }

    static class BuildTask {
        final String id;
        final String idea;
        volatile String status = "CREATED";
        volatile int progress = 0;
        volatile String phase = "Starting";
        volatile String error;
        volatile Path buildDir;
        final long createdAt = System.currentTimeMillis();

        BuildTask(String id, String idea) {
            this.id = id;
            this.idea = idea;
        }

        String toJson() {
            Path apk = findApk(buildDir);
            String downloadUrl;
            long apkSize = 0;
            try {
                if (apk != null && Files.exists(apk)) {
                    apkSize = Files.size(apk);
                    downloadUrl = String.format("\"download_url\":\"%s/api/download/%s\",\"apk_size\":%d,",
                        BASE_URL, id, apkSize);
                } else {
                    downloadUrl = "\"download_url\":null,\"apk_size\":0,";
                }
            } catch (IOException e) {
                downloadUrl = "\"download_url\":null,\"apk_size\":0,";
            }
            String err = error != null ? "\"error\":\"" + error.replace("\"", "'").replace("\n", "\\n") + "\"," : "";
            return String.format(
                "{\"build_id\":\"%s\",\"status\":\"%s\",\"progress\":%d,\"phase\":\"%s\",%s%s\"created_at\":%d}",
                id, status, progress, phase, downloadUrl, err, createdAt
            );
        }
    }
}