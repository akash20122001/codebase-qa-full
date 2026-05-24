# Embedding Model Fix

## Problem
The application is configured to use `text-embedding-004` but this model doesn't exist in the Gemini API.

## Solution
Change the embedding model in `backend/src/main/resources/application.yml` from:

```yaml
embedding-model: text-embedding-004
```

To:

```yaml
embedding-model: text-embedding-004
```

## Available Gemini API Embedding Models
- `text-embedding-004` - For Gemini API (recommended)
- `gemini-embedding-001` - Alternative for Gemini API
- `gemini-embedding-2` - Latest multimodal model

## Manual Fix Required
Please manually edit `backend/src/main/resources/application.yml` line 52 and change:
- FROM: `embedding-model: text-embedding-004`
- TO: `embedding-model: text-embedding-004`

Then rebuild and restart the backend.
