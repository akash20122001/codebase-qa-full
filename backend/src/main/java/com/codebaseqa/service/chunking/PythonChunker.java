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
 * Improved Python chunker with support for:
 * - Functions: def foo():, async def foo():
 * - Classes: class Foo:, class Foo(Base):
 * - Decorators: @decorator before functions/classes
 * - Class methods: extracted as ClassName.method_name
 * - Indentation-based scope detection
 */
@Component
@Order(2) // High priority - check before FallbackChunker
@Slf4j
public class PythonChunker implements LanguageChunker {

    // Matches: def foo(...): or async def foo(...):
    private static final Pattern DEF_PATTERN = Pattern.compile(
        "^(\\s*)(async\\s+)?def\\s+(\\w+)\\s*\\("
    );
    
    // Matches: class Foo: or class Foo(Base):
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "^(\\s*)class\\s+(\\w+)"
    );
    
    // Matches decorators: @decorator or @decorator(args)
    private static final Pattern DECORATOR_PATTERN = Pattern.compile(
        "^\\s*@\\w+"
    );

    @Override
    public boolean supports(String language) {
        return "python".equals(language);
    }

    @Override
    public List<CodeChunkResult> chunk(String content) {
        List<CodeChunkResult> chunks = new ArrayList<>();
        String[] lines = content.split("\n");

        List<BlockInfo> blocks = new ArrayList<>();
        boolean hasDecorator = false;
        int decoratorStart = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            int currentIndent = getIndentLevel(line);

            // Check for decorator
            if (DECORATOR_PATTERN.matcher(line).find()) {
                if (!hasDecorator) {
                    decoratorStart = i;
                    hasDecorator = true;
                }
                continue;
            }

            // Try to match class declaration
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                int actualStart = hasDecorator ? decoratorStart : i;
                blocks.add(new BlockInfo(
                    actualStart,
                    classMatcher.group(2),
                    "CLASS",
                    currentIndent
                ));
                hasDecorator = false;
                continue;
            }

            // Try to match function/method declaration
            Matcher defMatcher = DEF_PATTERN.matcher(line);
            if (defMatcher.find()) {
                int actualStart = hasDecorator ? decoratorStart : i;
                String funcName = defMatcher.group(3);
                
                // Find the containing class (if any)
                String className = null;
                for (int j = blocks.size() - 1; j >= 0; j--) {
                    BlockInfo block = blocks.get(j);
                    if ("CLASS".equals(block.type) && currentIndent > block.indent) {
                        className = block.name;
                        break;
                    }
                }
                
                String blockName = className != null ? className + "." + funcName : funcName;
                String blockType = className != null ? "METHOD" : "FUNCTION";
                
                blocks.add(new BlockInfo(
                    actualStart,
                    blockName,
                    blockType,
                    currentIndent
                ));
                hasDecorator = false;
                continue;
            }
        }

        // Convert blocks to chunks by finding their end lines
        for (int i = 0; i < blocks.size(); i++) {
            BlockInfo block = blocks.get(i);
            int endLine = lines.length - 1;
            
            // Find where this block ends (next block at same or lower indentation)
            for (int j = block.startLine + 1; j < lines.length; j++) {
                String line = lines[j].trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                int lineIndent = getIndentLevel(lines[j]);
                if (lineIndent <= block.indent) {
                    endLine = j - 1;
                    break;
                }
            }
            
            String chunkContent = extractLines(lines, block.startLine, endLine);
            if (!chunkContent.trim().isEmpty()) {
                chunks.add(new CodeChunkResult(
                    chunkContent,
                    block.name,
                    block.type,
                    block.startLine + 1,
                    endLine + 1
                ));
            }
        }

        log.debug("Python chunker extracted {} chunks", chunks.size());
        return chunks;
    }

    private static class BlockInfo {
        int startLine;
        String name;
        String type;
        int indent;

        BlockInfo(int startLine, String name, String type, int indent) {
            this.startLine = startLine;
            this.name = name;
            this.type = type;
            this.indent = indent;
        }
    }

    private int getIndentLevel(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                indent++;
            } else if (c == '\t') {
                indent += 4; // Treat tab as 4 spaces
            } else {
                break;
            }
        }
        return indent;
    }

    private String extractLines(String[] lines, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= Math.min(end, lines.length - 1); i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
