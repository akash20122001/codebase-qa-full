import { useState } from 'react';
import { Send, Sparkles } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';

/**
 * Home / Dashboard Page
 * Welcome state with quick ask interface and recent conversations
 */
export function HomePage() {
  const user = useAuthStore((s) => s.user);
  const [question, setQuestion] = useState('');
  const [selectedRepo, setSelectedRepo] = useState('');

  const suggestions = [
    'How does auth work?',
    'Explain the DB schema',
    'What patterns are used?',
    'Show API endpoints',
  ];

  const handleAsk = () => {
    if (!question.trim() || !selectedRepo) return;
    // TODO: Navigate to chat with question
    console.log('Ask:', question, 'Repo:', selectedRepo);
  };

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-site mx-auto px-chat-padding py-8">
        {/* Welcome Header */}
        <div className="mb-12">
          <h1 className="text-display-xs font-semibold text-neutral-900 mb-2">
            Welcome back, {user?.username || 'there'} 👋
          </h1>
          <p className="text-body-md text-neutral-600">
            Ask anything about your connected repositories
          </p>
        </div>

        {/* Ask Card */}
        <div className="bg-white border-2 border-neutral-200 border-t-brand-500 rounded-xl p-6 shadow-xs mb-12">
          <div className="flex items-center gap-2 mb-4">
            <Sparkles className="w-5 h-5 text-brand-600" />
            <h2 className="text-headline-sm font-semibold text-neutral-900">
              Ask Anything About Your Codebase
            </h2>
          </div>

          {/* Repository Selector */}
          <div className="mb-4">
            <label className="block text-body-sm font-medium text-neutral-700 mb-2">
              Repository
            </label>
            <select
              value={selectedRepo}
              onChange={(e) => setSelectedRepo(e.target.value)}
              className="w-full px-4 py-3 border border-neutral-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 transition-all"
            >
              <option value="">Select a repository...</option>
              <option value="repo1">owner/repository-name</option>
              <option value="repo2">owner/another-repo</option>
            </select>
          </div>

          {/* Question Input */}
          <div className="mb-4">
            <label className="block text-body-sm font-medium text-neutral-700 mb-2">
              Your Question
            </label>
            <textarea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Ask a question about the codebase..."
              rows={4}
              className="w-full px-4 py-3 border border-neutral-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500 transition-all resize-none"
            />
          </div>

          {/* Suggestions */}
          <div className="mb-6">
            <p className="text-body-sm text-neutral-600 mb-3">Suggestions:</p>
            <div className="flex flex-wrap gap-2">
              {suggestions.map((suggestion) => (
                <button
                  key={suggestion}
                  onClick={() => setQuestion(suggestion)}
                  className="px-3 py-1.5 bg-brand-50 text-brand-700 text-body-sm rounded-md hover:bg-brand-100 transition-colors"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>

          {/* Ask Button */}
          <div className="flex justify-end">
            <button
              onClick={handleAsk}
              disabled={!question.trim() || !selectedRepo}
              className="flex items-center gap-2 bg-brand-600 text-white font-semibold px-6 py-3 rounded-lg hover:bg-brand-700 disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98] transition-all shadow-sm"
            >
              Ask
              <Send className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Recent Conversations */}
        <div>
          <h2 className="text-headline-sm font-semibold text-neutral-900 mb-4">
            Recent Conversations
          </h2>
          
          {/* Empty State */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="col-span-full flex flex-col items-center justify-center py-16 text-center">
              <div className="w-16 h-16 rounded-full bg-neutral-100 flex items-center justify-center mb-4">
                <Sparkles className="w-8 h-8 text-neutral-400" />
              </div>
              <p className="text-body-md text-neutral-600 mb-2">No conversations yet</p>
              <p className="text-body-sm text-neutral-500">
                Start by asking a question about your codebase above
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
