# Backend OAuth Flow Requirements

## Issue
The current backend OAuth callback returns JSON directly, but it needs to redirect back to the frontend with the token.

## Current Behavior
1. User clicks "Sign in with GitHub" on frontend
2. Frontend redirects to: `http://localhost:8080/api/auth/github`
3. Backend redirects to GitHub OAuth
4. GitHub redirects back to: `http://localhost:8080/api/auth/github/callback?code=...`
5. **Backend returns JSON** ❌ (This is the problem)

## Required Behavior
1. User clicks "Sign in with GitHub" on frontend
2. Frontend redirects to: `http://localhost:8080/api/auth/github?redirect_uri=http://localhost:5173/auth/callback`
3. Backend redirects to GitHub OAuth
4. GitHub redirects back to: `http://localhost:8080/api/auth/github/callback?code=...`
5. **Backend processes OAuth, generates JWT, then redirects to frontend** ✅
   - Redirect to: `http://localhost:5173/auth/callback?token=<jwt_token>`
   - Or on error: `http://localhost:5173/auth/callback?error=auth_failed`

## Backend Changes Needed

### 1. Update AuthController.java

The `/api/auth/github/callback` endpoint should:

```java
@GetMapping("/github/callback")
public void handleGithubCallback(
    @RequestParam String code,
    @RequestParam(required = false) String state, // Contains redirect_uri
    HttpServletResponse response
) throws IOException {
    try {
        // Exchange code for access token
        String accessToken = githubClient.exchangeCodeForToken(code);
        
        // Get user info from GitHub
        GitHubUser githubUser = githubClient.getUserInfo(accessToken);
        
        // Create or update user in database
        User user = authService.createOrUpdateUser(githubUser);
        
        // Generate JWT
        String jwt = jwtService.generateToken(user);
        
        // Extract redirect_uri from state parameter (or use default)
        String redirectUri = extractRedirectUri(state);
        if (redirectUri == null) {
            redirectUri = "http://localhost:5173/auth/callback";
        }
        
        // Redirect to frontend with token
        response.sendRedirect(redirectUri + "?token=" + jwt);
        
    } catch (Exception e) {
        log.error("OAuth callback failed", e);
        response.sendRedirect("http://localhost:5173/auth/callback?error=auth_failed");
    }
}
```

### 2. Update Initial OAuth Redirect

The `/api/auth/github` endpoint should accept and preserve the `redirect_uri`:

```java
@GetMapping("/github")
public void redirectToGithub(
    @RequestParam(required = false) String redirect_uri,
    HttpServletResponse response
) throws IOException {
    String githubAuthUrl = String.format(
        "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=%s&state=%s",
        githubClientId,
        URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8),
        "read:user,repo",
        URLEncoder.encode(redirect_uri != null ? redirect_uri : "", StandardCharsets.UTF_8)
    );
    
    response.sendRedirect(githubAuthUrl);
}
```

### 3. Security Considerations

**Important:** Validate the `redirect_uri` to prevent open redirect vulnerabilities:

```java
private boolean isValidRedirectUri(String uri) {
    if (uri == null) return false;
    
    List<String> allowedOrigins = Arrays.asList(
        "http://localhost:5173",
        "http://localhost:3000",
        "https://your-production-domain.com"
    );
    
    return allowedOrigins.stream().anyMatch(uri::startsWith);
}
```

## Frontend Changes (Already Done ✅)

The frontend has been updated to:

1. **LoginPage**: Redirects to backend with `redirect_uri` parameter
2. **OAuthCallbackPage**: Expects `token` or `error` in URL params
3. **useAuth hook**: Fetches user info after receiving token
4. **Error handling**: Shows error message on login page if auth fails

## Testing the Flow

1. Start backend: `mvn spring-boot:run` (port 8080)
2. Start frontend: `npm run dev` (port 5173)
3. Navigate to: `http://localhost:5173/login`
4. Click "Sign in with GitHub"
5. Authorize on GitHub
6. Should redirect back to: `http://localhost:5173/auth/callback?token=<jwt>`
7. Frontend fetches user info and redirects to main app

## Environment Variables

Backend needs these configured:

```yaml
# application.yml
github:
  client-id: ${GITHUB_CLIENT_ID}
  client-secret: ${GITHUB_CLIENT_SECRET}
  callback-url: http://localhost:8080/api/auth/github/callback

frontend:
  url: http://localhost:5173
  allowed-origins:
    - http://localhost:5173
    - http://localhost:3000
```

## CORS Configuration

Ensure backend allows the frontend origin:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173", "http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowCredentials(true);
    }
}
```

## Summary

The key change is: **Backend should redirect to frontend with token, not return JSON**.

This is a standard OAuth flow pattern where:
- Backend handles OAuth with GitHub (server-to-server)
- Backend redirects user back to frontend with token (browser redirect)
- Frontend stores token and makes authenticated API calls

---

**Note for Backend Agent:** Please implement these changes in the AuthController and related services.
