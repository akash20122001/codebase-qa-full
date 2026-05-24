package com.codebaseqa.service.chunking;

import com.codebaseqa.service.ChunkingService.CodeChunkResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Improved Java chunker with better pattern matching for:
 * - Classes: public class Foo, abstract class Foo
 * - Interfaces: public interface Foo
 * - Enums: public enum Status
 * - Methods: public void foo(), private static int bar()
 * - Constructors: public Foo()
 * - Multi-line declarations
 * - Nested classes
 */
@Component
@Order(3) // High priority - check before FallbackChunker
@Slf4j
public class JavaChunker implements LanguageChunker {

    // Matches: public class Foo, interface Bar, enum Status, etc.
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(public|private|protected)?\\s*(abstract|static|final)?\\s*(class|interface|enum)\\s+(\\w+)"
    );
    
    // Matches: public void foo(...), private static int bar(...), etc.
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(public|private|protected)?\\s*(static|final|abstract|synchronized)?\\s*(<[^>]+>)?\\s*([\\w<>\\[\\]]+)\\s+(\\w+)\\s*\\("
    );

    @Override
    public boolean supports(String language) {
        return "java".equals(language);
    }

    @Override
    public List<CodeChunkResult> chunk(String content) {
        List<CodeChunkResult> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        // First pass: find all class/interface/enum declarations
        List<BlockInfo> topLevelBlocks = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty() || 
                trimmedLine.startsWith("//") || 
                trimmedLine.startsWith("/*") || 
                trimmedLine.startsWith("*") ||
                trimmedLine.startsWith("@")) {
                continue;
            }

            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                String name = classMatcher.group(4);
                String typeKeyword = classMatcher.group(3);
                String type = typeKeyword.toUpperCase();
                
                // Find end of this class
                int braceCount = countChar(line, '{') - countChar(line, '}');
                int endLine = i;
                
                for (int j = i + 1; j < lines.length && braceCount > 0; j++) {
                    braceCount += countChar(lines[j], '{') - countChar(lines[j], '}');
                    endLine = j;
                    if (braceCount <= 0) break;
                }
                
                topLevelBlocks.add(new BlockInfo(i, endLine, name, type));
                
                // If it's a class, extract methods/constructors
                if ("CLASS".equals(type)) {
                    extractMethodsFromClass(lines, i, endLine, name, chunks);
                }
                
                // Add the class/interface/enum itself
                chunks.add(new CodeChunkResult(
                    extractLines(lines, i, endLine),
                    name,
                    type,
                    i + 1,
                    endLine + 1
                ));
            }
        }

        log.debug("Java chunker extracted {} chunks", chunks.size());
        return chunks;
    }

    private void extractMethodsFromClass(String[] lines, int classStart, int classEnd, String className, List<CodeChunkResult> chunks) {
        for (int i = classStart + 1; i < classEnd; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty() || 
                trimmedLine.startsWith("//") || 
                trimmedLine.startsWith("/*") || 
                trimmedLine.startsWith("*") ||
                trimmedLine.startsWith("@") ||
                trimmedLine.startsWith("private") && trimmedLine.contains(";")) { // Skip field declarations
                continue;
            }

            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            if (methodMatcher.find()) {
                String methodName = methodMatcher.group(5);
                
                // Find end of method
                int braceCount = countChar(line, '{') - countChar(line, '}');
                
                // Look ahead for opening brace if not on same line
                if (braceCount == 0) {
                    for (int j = i + 1; j < Math.min(i + 5, classEnd); j++) {
                        String lookAhead = lines[j].trim();
                        if (lookAhead.startsWith("{")) {
                            braceCount = 1;
                            break;
                        }
                        if (!lookAhead.isEmpty() && !lookAhead.startsWith("@")) {
                            break;
                        }
                    }
                }
                
                if (braceCount > 0) {
                    int methodEnd = i;
                    for (int j = i + 1; j < classEnd && braceCount > 0; j++) {
                        braceCount += countChar(lines[j], '{') - countChar(lines[j], '}');
                        methodEnd = j;
                        if (braceCount <= 0) break;
                    }
                    
                    // Check if it's a constructor
                    String chunkName;
                    String chunkType;
                    if (methodName.equals(className)) {
                        chunkName = className + ".<init>";
                        chunkType = "CONSTRUCTOR";
                    } else {
                        chunkName = className + "." + methodName;
                        chunkType = "METHOD";
                    }
                    
                    chunks.add(new CodeChunkResult(
                        extractLines(lines, i, methodEnd),
                        chunkName,
                        chunkType,
                        i + 1,
                        methodEnd + 1
                    ));
                }
            }
        }
    }

    private static class BlockInfo {
        int startLine;
        int endLine;
        String name;
        String type;

        BlockInfo(int startLine, int endLine, String name, String type) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.name = name;
            this.type = type;
        }
    }

    private int countChar(String s, char c) {
        return (int) s.chars().filter(ch -> ch == c).count();
    }

    private String extractLines(String[] lines, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= Math.min(end, lines.length - 1); i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
