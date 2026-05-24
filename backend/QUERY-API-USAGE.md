# Query API Usage Guide

## Quick Start

### 1. Basic Query (cURL)

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "repoId": "660e8400-e29b-41d4-a716-446655440001",
    "question": "How does the authentication middleware work?"
  }'
```

### 2. Follow-up Question

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "repoId": "660e8400-e29b-41d4-a716-446655440001",
    "conversationId": "880e8400-e29b-41d4-a716-446655440010",
    "question": "Can you show me an example of how to use it?"
  }'
```

---

## Response Format (SSE)

### Event 1: Citations
Sent first, contains relevant code chunks found by vector search.

```
event: citations
data: [
  {
    "filePath": "src/middleware/JwtAuthenticationFilter.java",
    "startLine": 25,
    "endLine": 58,
    "chunkName": "doFilterInternal",
    "snippet": "protected void doFilterInternal(HttpServletRequest request, ...) {\n    String token = extractToken(request);\n    ..."
  },
  {
    "filePath": "src/service/JwtService.java",
    "startLine": 15,
    "endLine": 42,
    "chunkName": "validateToken",
    "snippet": "public boolean validateToken(String token) {\n    try {\n        Jwts.parser()..."
  }
]
```

### Event 2-N: Tokens
Streamed as the LLM generates the response.

```
event: token
data: {"content":"The"}

event: token
data: {"content":" authentication"}

event: token
data: {"content":" middleware"}

event: token
data: {"content":" in"}

event: token
data: {"content":" `JwtAuthenticationFilter.java`"}

event: token
data: {"content":" works"}

event: token
data: {"content":" by"}
...
```

### Final Event: Done
Sent when the response is complete.

```
event: done
data: {
  "messageId": "990e8400-e29b-41d4-a716-446655440020",
  "conversationId": "880e8400-e29b-41d4-a716-446655440010",
  "tokenCount": 342
}
```

### Error Event
Sent if something goes wrong.

```
event: error
data: {
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "You have exceeded the rate limit. Please wait before asking another question.",
  "retryAfter": 1823
}
```

---

## JavaScript/TypeScript Client

### Using Fetch API with ReadableStream

```typescript
async function askQuestion(repoId: string, question: string, conversationId?: string) {
  const response = await fetch('http://localhost:8080/api/query', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
    },
    body: JSON.stringify({
      repoId,
      conversationId,
      question,
    }),
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  const reader = response.body!.getReader();
  const decoder = new TextDecoder();

  let citations: Citation[] = [];
  let fullAnswer = '';
  let metadata: any = null;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');

    for (const line of lines) {
      if (line.startsWith('event: ')) {
        const eventType = line.substring(7).trim();
        continue;
      }

      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.substring(6));

        if (eventType === 'citations') {
          citations = data;
          onCitations(citations);
        } else if (eventType === 'token') {
          fullAnswer += data.content;
          onToken(data.content);
        } else if (eventType === 'done') {
          metadata = data;
          onDone(metadata);
        } else if (eventType === 'error') {
          onError(data);
        }
      }
    }
  }

  return { citations, fullAnswer, metadata };
}

// Callbacks
function onCitations(citations: Citation[]) {
  console.log('Received citations:', citations);
  // Update UI with code snippets
}

function onToken(token: string) {
  console.log('Token:', token);
  // Append to UI (streaming effect)
}

function onDone(metadata: any) {
  console.log('Done:', metadata);
  // Save conversationId for follow-ups
}

function onError(error: any) {
  console.error('Error:', error);
  // Show error message to user
}
```

### React Hook Example

```typescript
import { useState, useCallback } from 'react';

interface Citation {
  filePath: string;
  startLine: number;
  endLine: number;
  chunkName: string;
  snippet: string;
}

interface QueryMetadata {
  messageId: string;
  conversationId: string;
  tokenCount: number;
}

export function useQuery() {
  const [isLoading, setIsLoading] = useState(false);
  const [citations, setCitations] = useState<Citation[]>([]);
  const [answer, setAnswer] = useState('');
  const [error, setError] = useState<string | null>(null);

  const askQuestion = useCallback(async (
    repoId: string,
    question: string,
    conversationId?: string,
    onToken?: (token: string) => void
  ): Promise<QueryMetadata | null> => {
    setIsLoading(true);
    setError(null);
    setCitations([]);
    setAnswer('');

    try {
      const response = await fetch('/api/query', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify({ repoId, conversationId, question }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let currentEvent = '';
      let metadata: QueryMetadata | null = null;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            currentEvent = line.substring(7).trim();
          } else if (line.startsWith('data: ')) {
            const data = JSON.parse(line.substring(6));

            if (currentEvent === 'citations') {
              setCitations(data);
            } else if (currentEvent === 'token') {
              const token = data.content;
              setAnswer(prev => prev + token);
              onToken?.(token);
            } else if (currentEvent === 'done') {
              metadata = data;
            } else if (currentEvent === 'error') {
              setError(data.message);
              return null;
            }
          }
        }
      }

      return metadata;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { askQuestion, isLoading, citations, answer, error };
}
```

---

## Python Client

```python
import requests
import json

def ask_question(repo_id: str, question: str, token: str, conversation_id: str = None):
    url = "http://localhost:8080/api/query"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "text/event-stream",
    }
    payload = {
        "repoId": repo_id,
        "question": question,
    }
    if conversation_id:
        payload["conversationId"] = conversation_id

    response = requests.post(url, headers=headers, json=payload, stream=True)
    response.raise_for_status()

    citations = []
    full_answer = ""
    metadata = None
    current_event = None

    for line in response.iter_lines(decode_unicode=True):
        if not line:
            continue

        if line.startswith("event: "):
            current_event = line[7:].strip()
        elif line.startswith("data: "):
            data = json.loads(line[6:])

            if current_event == "citations":
                citations = data
                print(f"Received {len(citations)} citations")
            elif current_event == "token":
                token_text = data["content"]
                full_answer += token_text
                print(token_text, end="", flush=True)
            elif current_event == "done":
                metadata = data
                print(f"\n\nDone! Conversation ID: {metadata['conversationId']}")
            elif current_event == "error":
                print(f"\nError: {data['message']}")
                return None

    return {
        "citations": citations,
        "answer": full_answer,
        "metadata": metadata,
    }

# Usage
if __name__ == "__main__":
    result = ask_question(
        repo_id="660e8400-e29b-41d4-a716-446655440001",
        question="How does the authentication work?",
        token="your-jwt-token"
    )

    if result:
        print(f"\n\nFull answer:\n{result['answer']}")
```

---

## Error Codes

| Code | HTTP Status | Description | Retry Strategy |
|------|-------------|-------------|----------------|
| `RATE_LIMIT_EXCEEDED` | 429 | User exceeded 20 queries/hour | Wait `retryAfter` seconds |
| `REPO_NOT_INDEXED` | 400 | Repository not ready yet | Poll repo status, retry when READY |
| `FORBIDDEN` | 403 | User doesn't own repository | Don't retry |
| `INTERNAL_ERROR` | 500 | Unexpected server error | Retry with exponential backoff |
| `LLM_UNAVAILABLE` | 503 | Circuit breaker open | Wait 30 seconds, retry |

---

## Rate Limiting

- **Limit:** 20 queries per hour per user
- **Window:** Rolling 1-hour window
- **Headers:** Not currently exposed (future enhancement)
- **Reset:** Automatic after 1 hour from first query

### Check Remaining Quota (Future Enhancement)
```bash
# This endpoint doesn't exist yet, but could be added
curl -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/query/quota
```

---

## Best Practices

### 1. Handle SSE Properly
- Always check for `error` events
- Implement reconnection logic for network failures
- Set appropriate timeouts (2 minutes recommended)

### 2. Manage Conversations
- Store `conversationId` from `done` event
- Use it for follow-up questions
- Don't create new conversations for every question

### 3. Display Citations
- Show citations before the answer
- Make them clickable to view full code
- Highlight line numbers

### 4. Rate Limiting
- Show remaining quota to users
- Display countdown timer when limited
- Cache responses client-side when possible

### 5. Error Handling
- Show user-friendly error messages
- Implement retry logic with exponential backoff
- Log errors for debugging

### 6. Performance
- Use streaming to show progress
- Don't wait for full response before displaying
- Cache identical queries client-side

---

## Troubleshooting

### SSE Connection Fails
- Check CORS configuration
- Verify JWT token is valid
- Ensure `Accept: text/event-stream` header is set

### No Citations Returned
- Repository may not be indexed
- Question may be too vague
- Check if code chunks exist in database

### Slow Responses
- First query is always slower (no cache)
- Check Gemini API latency
- Verify Redis is running

### Rate Limit Hit Immediately
- Check if Redis has stale data
- Verify rate limit configuration
- Clear Redis cache for testing

---

## Advanced Usage

### Custom System Prompt (Future Enhancement)
Currently, the system prompt is hardcoded. To customize:
1. Modify `SYSTEM_PROMPT` in `QueryService.java`
2. Restart the application

### Adjust Top-K Chunks
To change the number of code chunks retrieved:
1. Modify `TOP_K_CHUNKS` in `QueryService.java`
2. Restart the application

### Change Rate Limits
Update `application.yml`:
```yaml
app:
  rate-limit:
    queries-per-hour: 50  # Increase limit
```

---

## API Reference

### Endpoint
```
POST /api/query
```

### Headers
- `Authorization: Bearer <jwt_token>` (required)
- `Content-Type: application/json` (required)
- `Accept: text/event-stream` (required)

### Request Body
```typescript
{
  repoId: string;           // UUID, required
  conversationId?: string;  // UUID, optional
  question: string;         // Max 1000 chars, required
}
```

### Response
Server-Sent Events stream with events:
- `citations` - Array of code chunks
- `token` - Individual response tokens
- `done` - Completion metadata
- `error` - Error information

### Status Codes
- `200` - Success (SSE stream)
- `400` - Invalid request
- `401` - Unauthorized
- `403` - Forbidden
- `429` - Rate limit exceeded
- `500` - Internal server error
- `503` - Service unavailable
