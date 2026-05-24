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
 * Improved TypeScript/JavaScript chunker with support for:
 * - Traditional functions: function foo() {}
 * - Arrow functions: const foo = () => {}, const foo = async () => {}
 * - Class declarations: class Foo {}, export class Foo {}, export default class Foo {}
 * - Class methods: methodName() {}, async methodName() {}
 * - Multi-line declarations
 */
@Component
@Order(1) // High priority - check before FallbackChunker
@Slf4j
public class TypeScriptChunker implements LanguageChunker {

    // Traditional function: function foo() { or async function foo() {
    private static final Pattern FUNC_PATTERN = Pattern.compile(
        "^\\s*(export\\s+)?(async\\s+)?function\\s+(\\w+)\\s*\\("
    );
    
    // Arrow function: const foo = () => { or const foo = async () => {
    private static final Pattern ARROW_PATTERN = Pattern.compile(
        "^\\s*(export\\s+)?(const|let|var)\\s+(\\w+)\\s*=\\s*(async\\s+)?\\([^)]*\\)\\s*=>"
    );
    
    // Class declaration: class Foo { or export class Foo { or export default class Foo {
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^\\s*(export\\s+)?(default\\s+)?class\\s+(\\w+)"
    );
    
    // Class method: methodName() { or async methodName() { (inside a class)
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(async\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*\\{"
    );

    @Override
    public boolean supports(String language) {
        return "typescript".equals(language) || "javascript".equals(language);
    }

    @Override
    public List<CodeChunkResult> chunk(String content) {
        List<CodeChunkResult> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        int blockStart = -1;
        String blockName = null;
        String blockType = null;
        int braceCount = 0;
        boolean inBlock = false;
        String currentClassName = null;
        int classStartLine = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("//") || trimmedLine.startsWith("/*")) {
                if (inBlock) {
                    braceCount += countChar(line, '{') - countChar(line, '}');
                }
                continue;
            }

            if (!inBlock) {
                // Try to match class declaration
                Matcher classMatcher = CLASS_PATTERN.matcher(line);
                if (classMatcher.find()) {
                    blockStart = i;
                    blockName = classMatcher.group(3);
                    blockType = "CLASS";
                    currentClassName = blockName;
                    classStartLine = i;
                    braceCount = countChar(line, '{') - countChar(line, '}');
                    inBlock = braceCount > 0;
                    continue;
                }

                // Try to match traditional function
                Matcher funcMatcher = FUNC_PATTERN.matcher(line);
                if (funcMatcher.find()) {
                    blockStart = i;
                    blockName = funcMatcher.group(3);
                    blockType = "FUNCTION";
                    braceCount = countChar(line, '{') - countChar(line, '}');
                    if (braceCount == 0 && i + 1 < lines.length) {
                        String nextLine = lines[i + 1].trim();
                        if (nextLine.startsWith("{")) {
                            braceCount = 1;
                        }
                    }
                    inBlock = braceCount > 0;
                    continue;
                }

                // Try to match arrow function
                Matcher arrowMatcher = ARROW_PATTERN.matcher(line);
                if (arrowMatcher.find()) {
                    blockStart = i;
                    blockName = arrowMatcher.group(3);
                    blockType = "FUNCTION";
                    braceCount = countChar(line, '{') - countChar(line, '}');
                    if (braceCount == 0 && i + 1 < lines.length) {
                        String nextLine = lines[i + 1].trim();
                        if (nextLine.startsWith("{")) {
                            braceCount = 1;
                        }
                    }
                    inBlock = braceCount > 0;
                    continue;
                }
            } else {
                // We're inside a block
                
                // If inside a class, look for methods before updating brace count
                if ("CLASS".equals(blockType) && currentClassName != null) {
                    Matcher methodMatcher = METHOD_PATTERN.matcher(line);
                    if (methodMatcher.find()) {
                        String methodName = methodMatcher.group(2);
                        if (!"constructor".equals(methodName)) {
                            // Extract method as separate chunk
                            int methodStart = i;
                            int methodBraceCount = countChar(line, '{') - countChar(line, '}');
                            
                            // Find end of method
                            int methodEnd = i;
                            for (int j = i + 1; j < lines.length && methodBraceCount > 0; j++) {
                                methodBraceCount += countChar(lines[j], '{') - countChar(lines[j], '}');
                                methodEnd = j;
                                if (methodBraceCount <= 0) break;
                            }
                            
                            chunks.add(new CodeChunkResult(
                                extractLines(lines, methodStart, methodEnd),
                                currentClassName + "." + methodName,
                                "METHOD",
                                methodStart + 1,
                                methodEnd + 1
                            ));
                        }
                    }
                }
                
                // Update brace count
                braceCount += countChar(line, '{') - countChar(line, '}');
                
                if (braceCount <= 0) {
                    // Block ended - save the class chunk
                    String chunkContent = extractLines(lines, blockStart, i);
                    if (!chunkContent.trim().isEmpty()) {
                        chunks.add(new CodeChunkResult(
                            chunkContent,
                            blockName,
                            blockType,
                            blockStart + 1,
                            i + 1
                        ));
                    }
                    inBlock = false;
                    currentClassName = null;
                }
            }
        }

        // Handle unclosed block at end of file
        if (inBlock && blockStart >= 0) {
            String chunkContent = extractLines(lines, blockStart, lines.length - 1);
            if (!chunkContent.trim().isEmpty()) {
                chunks.add(new CodeChunkResult(
                    chunkContent,
                    blockName,
                    blockType,
                    blockStart + 1,
                    lines.length
                ));
            }
        }

        log.debug("TypeScript chunker extracted {} chunks", chunks.size());
        return chunks;
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
