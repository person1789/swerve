package org.firstinspires.ftc.teamcode.Swerve.Sim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RouteFileService {
    private static final Pattern SIMPLE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
    private static final String PACKAGE_NAME = "org.firstinspires.ftc.teamcode.pedroPathing";
    private static final String SIM_START_START = "// @sim-start-start";
    private static final String SIM_START_END = "// @sim-start-end";
    private static final String PATH_START = "// @path-start";
    private static final String PATH_END = "// @path-end";
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private final Path pedroDir;
    private final Path staleGeneratedDir;

    public RouteFileService() {
        this.pedroDir = resolvePedroDir();
        this.staleGeneratedDir = pedroDir.resolve("stale").resolve("generated").normalize();
    }

    private static Path resolvePedroDir() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path teamCodeRoot = cwd.getFileName() != null && "TeamCode".equals(cwd.getFileName().toString())
                ? cwd
                : cwd.resolve("TeamCode");

        return teamCodeRoot
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("org")
                .resolve("firstinspires")
                .resolve("ftc")
                .resolve("teamcode")
                .resolve("pedroPathing")
                .normalize();
    }

    public List<String> listAutoFiles() throws IOException {
        return listAutoMetadata().stream()
                .map(metadata -> metadata.fileName)
                .collect(Collectors.toList());
    }

    public List<AutoMetadata> listAutoMetadata() throws IOException {
        List<AutoMetadata> autos = new ArrayList<>();
        if (!Files.isDirectory(pedroDir)) {
            return autos;
        }

        try (java.util.stream.Stream<Path> stream = Files.list(pedroDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith("Auto.java"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            String source = new String(Files.readAllBytes(path), UTF8);
                            autos.add(new AutoMetadata(
                                    path.getFileName().toString(),
                                    path.toAbsolutePath().toString(),
                                    extractClassName(source, path.getFileName().toString()),
                                    extractOpModeName(source, path.getFileName().toString()),
                                    source.contains("// @sim"),
                                    Files.getLastModifiedTime(path).toMillis(),
                                    false));
                        } catch (IOException ignored) {
                        }
                    });
        }

        return autos;
    }

    public RouteWriteResponse createAuto(RouteWriteRequest request) throws IOException {
        String className = normalizeSimpleName(request.className, "PedroGeneratedAuto");
        String opModeName = safeText(request.opModeName, className);
        RouteCodeParts routeCode = splitRouteCode(requireRouteCode(request.routeCode));

        Files.createDirectories(pedroDir);
        Path file = resolveFile(className + ".java");
        String source = buildAutoTemplate(className, opModeName, routeCode);
        Files.write(file, source.getBytes(UTF8));
        return new RouteWriteResponse(
                true,
                "Created " + file.getFileName() + " at " + file.toAbsolutePath(),
                file.getFileName().toString(),
                file.toAbsolutePath().toString());
    }

    public RouteWriteResponse patchAuto(RouteWriteRequest request) throws IOException {
        String targetFileName = requireJavaFileName(request.targetFileName);
        RouteCodeParts routeCode = splitRouteCode(requireRouteCode(request.routeCode));
        Path file = resolveFile(targetFileName);
        String original = new String(Files.readAllBytes(file), UTF8);

        String updated;
        if (original.contains(SIM_START_START) && original.contains(SIM_START_END)
                && original.contains(PATH_START) && original.contains(PATH_END)) {
            updated = replaceGeneratedTaggedBlocks(original, routeCode);
        } else if (original.contains(PATH_START) && original.contains(PATH_END)) {
            updated = replaceTaggedBlock(original, routeCode);
        } else if (original.contains("@path")) {
            updated = replaceSingleMarkerLine(original, routeCode.pathBlock);
        } else {
            throw new IOException("No @path marker found in " + targetFileName);
        }

        Files.write(file, updated.getBytes(UTF8));
        return new RouteWriteResponse(
                true,
                "Updated " + targetFileName + " at " + file.toAbsolutePath(),
                targetFileName,
                file.toAbsolutePath().toString());
    }

    public RouteWriteResponse deleteAuto(String targetFileName) throws IOException {
        String normalizedFileName = requireJavaFileName(targetFileName);
        Path file = resolveFile(normalizedFileName);
        if (!Files.exists(file)) {
            throw new IOException("Auto file not found: " + normalizedFileName);
        }
        Files.delete(file);
        return new RouteWriteResponse(
                true,
                "Deleted " + normalizedFileName,
                normalizedFileName,
                file.toAbsolutePath().toString());
    }

    public RouteWriteResponse duplicateAuto(String targetFileName, String className, String opModeName) throws IOException {
        String normalizedFileName = requireJavaFileName(targetFileName);
        String newClassName = normalizeSimpleName(className, normalizedFileName.replace(".java", "Copy"));
        String newFileName = newClassName + ".java";
        Path sourceFile = resolveFile(normalizedFileName);
        Path targetFile = resolveFile(newFileName);
        String source = new String(Files.readAllBytes(sourceFile), UTF8);

        String updated = source.replaceFirst("\\b" + Pattern.quote(normalizedFileName.replace(".java", "")) + "\\b", newClassName);
        if (opModeName != null && !opModeName.trim().isEmpty()) {
            updated = updated.replaceFirst("@Autonomous\\(name = \"[^\"]*\"", "@Autonomous(name = \"" + escapeJava(opModeName.trim()) + "\"");
        } else {
            updated = updated.replaceFirst("@Autonomous\\(name = \"[^\"]*\"", "@Autonomous(name = \"" + escapeJava(newClassName) + "\"");
        }
        updated = updated.replaceFirst("SIM_OPMODE_NAME = \"[^\"]*\"", "SIM_OPMODE_NAME = \"" + escapeJava(newClassName) + "\"");

        Files.write(targetFile, updated.getBytes(UTF8));
        return new RouteWriteResponse(true, "Duplicated " + normalizedFileName + " to " + newFileName, newFileName, targetFile.toAbsolutePath().toString());
    }

    public RouteWriteResponse renameAuto(String targetFileName, String className, String opModeName) throws IOException {
        String normalizedFileName = requireJavaFileName(targetFileName);
        String newClassName = normalizeSimpleName(className, normalizedFileName.replace(".java", ""));
        String newFileName = newClassName + ".java";
        Path sourceFile = resolveFile(normalizedFileName);
        Path targetFile = resolveFile(newFileName);
        String source = new String(Files.readAllBytes(sourceFile), UTF8);

        String updated = source.replaceFirst("\\b" + Pattern.quote(normalizedFileName.replace(".java", "")) + "\\b", newClassName);
        if (opModeName != null && !opModeName.trim().isEmpty()) {
            updated = updated.replaceFirst("@Autonomous\\(name = \"[^\"]*\"", "@Autonomous(name = \"" + escapeJava(opModeName.trim()) + "\"");
        }
        updated = updated.replaceFirst("SIM_OPMODE_NAME = \"[^\"]*\"", "SIM_OPMODE_NAME = \"" + escapeJava(newClassName) + "\"");

        Files.write(targetFile, updated.getBytes(UTF8));
        if (!sourceFile.equals(targetFile)) {
            Files.delete(sourceFile);
        }
        return new RouteWriteResponse(true, "Renamed " + normalizedFileName + " to " + newFileName, newFileName, targetFile.toAbsolutePath().toString());
    }

    public RouteWriteResponse archiveAuto(String targetFileName) throws IOException {
        String normalizedFileName = requireJavaFileName(targetFileName);
        Path sourceFile = resolveFile(normalizedFileName);
        if (!Files.exists(sourceFile)) {
            throw new IOException("Auto file not found: " + normalizedFileName);
        }
        Files.createDirectories(staleGeneratedDir);
        Path targetFile = staleGeneratedDir.resolve(normalizedFileName).normalize();
        Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        return new RouteWriteResponse(true, "Archived " + normalizedFileName, normalizedFileName, targetFile.toAbsolutePath().toString());
    }

    private String buildAutoTemplate(String className, String opModeName, RouteCodeParts routeCode) {
        return ""
                + "package " + PACKAGE_NAME + ";\n\n"
                + "import com.acmerobotics.dashboard.config.Config;\n"
                + "import com.pedropathing.follower.Follower;\n"
                + "import com.pedropathing.geometry.Pose;\n"
                + "import com.pedropathing.paths.PathChain;\n"
                + "import com.qualcomm.robotcore.eventloop.opmode.Autonomous;\n"
                + "import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;\n\n"
                + "import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;\n\n"
                + "// @sim\n"
                + "@Config\n"
                + "@Autonomous(name = \"" + escapeJava(opModeName) + "\", group = \"Pedro\")\n"
                + "public class " + className + " extends LinearOpMode {\n"
                + "    public static String SIM_OPMODE_NAME = \"" + escapeJava(className) + "\";\n"
                + "    public static boolean useStoredPose = true;\n\n"
                + "    public static Pose buildSimStartPose() {\n"
                + "        " + SIM_START_START + "\n"
                + "        " + routeCode.startLine + "\n"
                + "        " + SIM_START_END + "\n"
                + "        return startPose;\n"
                + "    }\n\n"
                + "    public static PathChain buildSimPath(Follower follower, Pose startPose) {\n"
                + "        " + PATH_START + "\n"
                + indent(routeCode.pathBlock, 8) + "\n"
                + "        " + PATH_END + "\n"
                + "        return route;\n"
                + "    }\n\n"
                + "    @Override\n"
                + "    public void runOpMode() throws InterruptedException {\n"
                + "        PedroSwerveFactory.PedroRobot robot = PedroSwerveFactory.createRobot(hardwareMap, null);\n"
                + "        Follower follower = robot.follower;\n\n"
                + "        PedroSwerveFactory.applyLiveTuning(follower);\n\n"
                + "        Pose startPose = buildSimStartPose();\n"
                + "        org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose storedPose = PoseStorage.getCurrentPose();\n"
                + "        if (useStoredPose && storedPose != null) {\n"
                + "            startPose = new Pose(storedPose.x, storedPose.y, storedPose.heading);\n"
                + "        }\n\n"
                + "        PathChain route = buildSimPath(follower, startPose);\n"
                + "        robot.stack.resetForStart();\n"
                + "        follower.setStartingPose(startPose);\n"
                + "        follower.setPose(startPose);\n\n"
                + "        telemetry.addLine(\"Pedro auto ready\");\n"
                + "        telemetry.addData(\"Start\", startPose);\n"
                + "        telemetry.update();\n\n"
                + "        waitForStart();\n"
                + "        if (isStopRequested()) {\n"
                + "            return;\n"
                + "        }\n\n"
                + "        follower.followPath(route, true);\n\n"
                + "        while (opModeIsActive() && follower.isBusy()) {\n"
                + "            PedroSwerveFactory.applyLiveTuning(follower);\n"
                + "            follower.update();\n"
                + "            Pose pose = follower.getPose();\n"
                + "            telemetry.addData(\"X\", pose.getX());\n"
                + "            telemetry.addData(\"Y\", pose.getY());\n"
                + "            telemetry.addData(\"HeadingDeg\", Math.toDegrees(pose.getHeading()));\n"
                + "            telemetry.addData(\"DistanceRemaining\", follower.getDistanceRemaining());\n"
                + "            telemetry.update();\n"
                + "        }\n\n"
                + "        PoseStorage.setFromPedroPose(follower.getPose());\n"
                + "        robot.stack.stopPedroDrive();\n"
                + "    }\n"
                + "}\n";
    }

    private String replaceTaggedBlock(String original, RouteCodeParts routeCode) throws IOException {
        int startIndex = original.indexOf(PATH_START);
        int endIndex = original.indexOf(PATH_END);
        if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
            throw new IOException("Malformed @path block");
        }

        int insertStart = original.indexOf('\n', startIndex);
        if (insertStart < 0) {
            throw new IOException("Malformed @path block");
        }

        String before = original.substring(0, insertStart + 1);
        String after = original.substring(endIndex);
        return before + indent(routeCode.pathBlock, 8) + "\n" + after;
    }

    private String replaceGeneratedTaggedBlocks(String original, RouteCodeParts routeCode) throws IOException {
        String withStart = replaceBlockBetweenMarkers(
                original,
                SIM_START_START,
                SIM_START_END,
                "        " + routeCode.startLine + "\n");
        return replaceBlockBetweenMarkers(
                withStart,
                PATH_START,
                PATH_END,
                indent(routeCode.pathBlock, 8) + "\n");
    }

    private String replaceSingleMarkerLine(String original, String routeCode) {
        String[] lines = original.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("@path")) {
                String indent = leadingWhitespace(line);
                out.append(indent(routeCode.trim(), indent.length())).append('\n');
            } else {
                out.append(line);
                if (i < lines.length - 1) {
                    out.append('\n');
                }
            }
        }
        return out.toString();
    }

    private String replaceBlockBetweenMarkers(String original, String startMarker, String endMarker, String replacement) throws IOException {
        int startIndex = original.indexOf(startMarker);
        int endIndex = original.indexOf(endMarker);
        if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
            throw new IOException("Malformed marker block: " + startMarker);
        }
        int insertStart = original.indexOf('\n', startIndex);
        if (insertStart < 0) {
            throw new IOException("Malformed marker block: " + startMarker);
        }
        String before = original.substring(0, insertStart + 1);
        String after = original.substring(endIndex);
        return before + replacement + after;
    }

    private RouteCodeParts splitRouteCode(String routeCode) throws IOException {
        String normalized = routeCode.replace("\r\n", "\n").trim();
        int newlineIndex = normalized.indexOf('\n');
        if (newlineIndex < 0) {
            throw new IOException("Route code must include a start pose line and a path block");
        }
        String startLine = normalized.substring(0, newlineIndex).trim();
        String pathBlock = normalized.substring(newlineIndex + 1).trim();
        if (!startLine.startsWith("Pose startPose")) {
            throw new IOException("Route code must begin with a Pose startPose line");
        }
        if (!pathBlock.startsWith("PathChain route")) {
            throw new IOException("Route code must define a PathChain route");
        }
        return new RouteCodeParts(startLine, pathBlock);
    }

    private static String indent(String text, int spaces) {
        String indent = " ".repeat(Math.max(0, spaces));
        return text.replace("\n", "\n" + indent);
    }

    private static String leadingWhitespace(String text) {
        int index = 0;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return text.substring(0, index);
    }

    private Path resolveFile(String fileName) throws IOException {
        Path file = pedroDir.resolve(fileName).normalize();
        if (!file.startsWith(pedroDir.normalize())) {
            throw new IOException("Invalid target path");
        }
        return file;
    }

    private static String requireRouteCode(String routeCode) throws IOException {
        if (routeCode == null || routeCode.trim().isEmpty()) {
            throw new IOException("Route code is empty");
        }
        return routeCode;
    }

    private static String normalizeSimpleName(String value, String fallback) throws IOException {
        String candidate = value == null || value.trim().isEmpty() ? fallback : value.trim();
        if (!SIMPLE_NAME.matcher(candidate).matches()) {
            throw new IOException("Invalid Java class name: " + candidate);
        }
        return candidate;
    }

    private static String requireJavaFileName(String value) throws IOException {
        String candidate = value == null ? "" : value.trim();
        if (!candidate.endsWith(".java")) {
            throw new IOException("Target file must end with .java");
        }

        String simple = candidate.substring(0, candidate.length() - 5);
        if (!SIMPLE_NAME.matcher(simple).matches()) {
            throw new IOException("Invalid target file: " + candidate);
        }
        return candidate;
    }

    private static String safeText(String value, String fallback) {
        String text = value == null || value.trim().isEmpty() ? fallback : value.trim();
        return text.replace('\n', ' ').replace('\r', ' ');
    }

    private static String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractClassName(String source, String fallbackFileName) {
        java.util.regex.Matcher matcher = Pattern.compile("public class\\s+([A-Za-z][A-Za-z0-9_]*)").matcher(source);
        return matcher.find() ? matcher.group(1) : fallbackFileName.replace(".java", "");
    }

    private static String extractOpModeName(String source, String fallbackFileName) {
        java.util.regex.Matcher matcher = Pattern.compile("@Autonomous\\(name = \"([^\"]*)\"").matcher(source);
        return matcher.find() ? matcher.group(1) : fallbackFileName.replace(".java", "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RouteWriteRequest {
        public String className;
        public String opModeName;
        public String targetFileName;
        public String routeCode;
    }

    public static class AutoMetadata {
        public final String fileName;
        public final String filePath;
        public final String className;
        public final String opModeName;
        public final boolean simTagged;
        public final long modifiedTimeMs;
        public final boolean archived;

        public AutoMetadata(String fileName, String filePath, String className, String opModeName, boolean simTagged, long modifiedTimeMs, boolean archived) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.className = className;
            this.opModeName = opModeName;
            this.simTagged = simTagged;
            this.modifiedTimeMs = modifiedTimeMs;
            this.archived = archived;
        }
    }

    public static class RouteWriteResponse {
        public final boolean ok;
        public final String message;
        public final String fileName;
        public final String filePath;

        public RouteWriteResponse(boolean ok, String message, String fileName, String filePath) {
            this.ok = ok;
            this.message = message;
            this.fileName = fileName;
            this.filePath = filePath;
        }
    }

    private static class RouteCodeParts {
        final String startLine;
        final String pathBlock;

        RouteCodeParts(String startLine, String pathBlock) {
            this.startLine = startLine;
            this.pathBlock = pathBlock;
        }
    }
}
