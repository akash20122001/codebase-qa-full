# Code Chunking Improvement Plan

## Current Situation

### Problem
All indexed chunks show `chunk_type=BLOCK` and `chunk_name=NULL` because the regex-based chunkers are too simplistic and fail to match most real-world code patterns.

### What We Tried

1. **Apache Tika** ❌
   - Only does language detection (English vs Spanish), not code parsing
   - Cannot extract functions/classes from source code
   - Not designed for programming languages

2. **Tree-sitter (Official Java Bindings)** ❌
   - `io.github.tree-sitter:jtreesitter` requires JDK 23+ (we're on JDK 21)
   - Alternative `bonede/tree-sitter-ng` only has Java grammar, missing TypeScript/Python
   - `dev.kreuzberg:tree-sitter-language-pack` has 306 languages but:
     - Latest Maven Central version is 1.3.3 (docs show 1.9.0)
     - Package structure unclear (`org.treesitter` doesn't exist in 1.3.3)
     - Would require significant integration effort

3. **JavaParser** ✅ (Partially)
   - Already in pom.xml
   - Works great for Java
   - But doesn't help with TypeScript/JavaScript/Python

## Recommended Solution: Improved Regex Chunkers

### Why This Approach?

1. **Works Now** - No new dependencies, no JDK upgrades
2. **Incremental** - Improve one language at a time
3. **Proven** - Many tools use regex for code parsing (GitHub linguist, etc.)
4. **Good Enough** - 80% accuracy is fine for semantic search

### Implementation Plan

#### Phase 1: Improve TypeScript/JavaScript Chunker (Highest Priority)
Current regex only matches:
```typescript
function hello() {  // ✅ Matches
```

Needs to also match:
```typescript
const hello = () => {              // ❌ Currently misses
export const hello = async () => { // ❌ Currently misses
class UserService {                // ❌ Currently misses
export default class User {        // ❌ Currently misses
```

**Improved Patterns:**
- Arrow functions: `const|let|var \w+ = (async )?\([^)]*\) =>`
- Class declarations: `(export )?(default )?class \w+`
- Method declarations: `(async )?\w+\([^)]*\)\s*{`
- Better brace counting to handle multi-line declarations

#### Phase 2: Improve Python Chunker
Current regex works for basic cases but misses:
```python
@decorator
def function():  // ❌ Misses decorator

async def function():  // ✅ Already handles

class MyClass:
    def __init__(self):  // ❌ Doesn't extract methods separately
```

**Improvements:**
- Handle decorators
- Extract class methods as separate chunks
- Better indentation-based scope detection

#### Phase 3: Use JavaParser for Java (Best Quality)
Replace regex with AST parsing for Java files:
- Accurate class/interface/enum detection
- Method extraction with proper boundaries
- Constructor identification
- Nested class support

### Expected Results

After improvements:
- **Java**: 95%+ accuracy (using JavaParser AST)
- **TypeScript/JS**: 85%+ accuracy (improved regex)
- **Python**: 80%+ accuracy (improved regex)

This is sufficient for semantic search - users will find relevant code even if some edge cases are missed.

## Alternative: Tree-sitter (Future)

If we want 95%+ accuracy across all languages:

### Option A: Upgrade to JDK 23+
- Use official `io.github.tree-sitter:jtreesitter`
- Get all language grammars
- Best long-term solution

### Option B: Use External Service
- Run tree-sitter in a separate Node.js/Python service
- Call via HTTP API
- Adds deployment complexity

### Option C: Wait for Library Maturity
- `dev.kreuzberg:tree-sitter-language-pack` looks promising
- But current Maven version (1.3.3) has unclear API
- Revisit in 6 months

## Decision

**Proceed with improved regex chunkers** for now:
1. Immediate value - fixes the BLOCK/null problem
2. No infrastructure changes needed
3. Can always add Tree-sitter later if needed

The system already works end-to-end. This improvement makes the chunks more semantically meaningful, which improves search quality.

## Files to Modify

1. `backend/src/main/java/com/codebaseqa/service/chunking/TypeScriptChunker.java`
2. `backend/src/main/java/com/codebaseqa/service/chunking/PythonChunker.java`
3. `backend/src/main/java/com/codebaseqa/service/chunking/JavaChunker.java` (use JavaParser)

## Testing Strategy

1. Create test files with various code patterns
2. Run chunking service
3. Verify chunk_name and chunk_type are populated correctly
4. Re-index a repository and check database results
