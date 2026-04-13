'use client';

import React, { useState, useRef, useEffect } from 'react';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { 
  Bot, 
  Send, 
  User, 
  Sparkles, 
  Terminal, 
  Trash2, 
  Loader2, 
  ChevronRight,
  Info
} from 'lucide-react';
import { aiAnalyticsService } from '@/services/aiAnalytics.service';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

const SUGGESTIONS = [
  "How many students are at high risk?",
  "Analyze today's attendance trends",
  "Report on any GPS anomalies",
  "Who are the top performers today?"
];

export const AIAssistantChat: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content: 'Hello Admin! I am the Smart Attendance AI Assistant. I can answer questions about students, sessions, and system-wide analytics. How can I help you today?',
      timestamp: new Date()
    }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const handleSendMessage = async (text?: string) => {
    const question = text || input.trim();
    if (!question || isLoading) return;

    // Add user message
    const userMessage: Message = { role: 'user', content: question, timestamp: new Date() };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      // Call AI Service
      const response = await aiAnalyticsService.askAI(question);
      
      // Add assistant message
      const assistantMessage: Message = { 
        role: 'assistant', 
        content: response.answer || "I'm sorry, I couldn't find an answer for that.",
        timestamp: new Date() 
      };
      setMessages(prev => [...prev, assistantMessage]);
    } catch (error: any) {
      const errorMessage: Message = { 
        role: 'assistant', 
        content: `Error: ${error.message || 'The AI assistant is temporarily unavailable. Check your connection or API configuration.'}`,
        timestamp: new Date() 
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const clearChat = () => {
    setMessages([{
      role: 'assistant',
      content: 'Chat history cleared. How can I assist you further?',
      timestamp: new Date()
    }]);
  };

  return (
    <Card glass className="h-[600px] flex flex-col border-indigo-500/20 overflow-hidden chat-dashboard-card shadow-2xl relative">
      <CardHeader className="flex flex-row items-center justify-between border-b border-gray-700/50 bg-gray-900/40 p-4">
        <div className="flex items-center space-x-3">
          <div className="relative">
            <div className="p-2 bg-indigo-500/10 rounded-lg">
              <Bot className="h-6 w-6 text-indigo-400" />
            </div>
            <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-gray-900 rounded-full animate-pulse"></span>
          </div>
          <div>
            <h3 className="text-lg font-bold text-white flex items-center">
              AI Command Center
              <Sparkles className="h-4 w-4 ml-2 text-yellow-400 opacity-60" />
            </h3>
            <span className="text-xs text-gray-400 flex items-center">
              <Terminal className="h-3 w-3 mr-1 text-indigo-400/70" />
              Direct access to system intelligence
            </span>
          </div>
        </div>
        
        <Button 
          variant="glass" 
          size="sm" 
          onClick={clearChat} 
          title="Clear History"
          className="hover:text-red-400 transition-colors"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </CardHeader>

      <CardContent className="flex-1 flex flex-col p-0 bg-black/20">
        {/* Messages Layout */}
        <div className="flex-1 overflow-y-auto p-4 space-y-6 scrollbar-thin scrollbar-thumb-indigo-500/20">
          {messages.map((msg, idx) => (
            <div 
              key={idx} 
              className={`flex items-start ${msg.role === 'user' ? 'justify-end' : 'justify-start'} animate-in fade-in slide-in-from-bottom-2 duration-300`}
            >
              {msg.role === 'assistant' && (
                <div className="w-8 h-8 rounded-full bg-indigo-500/20 flex items-center justify-center mr-3 flex-shrink-0 mt-1">
                  <Bot className="h-4 w-4 text-indigo-400" />
                </div>
              )}
              
              <div 
                className={`max-w-[85%] rounded-2xl p-4 shadow-sm border ${
                  msg.role === 'user' 
                    ? 'bg-indigo-600/20 border-indigo-500/30 text-white rounded-tr-none' 
                    : 'bg-gray-800/40 border-gray-700/50 text-gray-200 rounded-tl-none ring-1 ring-white/5'
                }`}
              >
                <div className="flex flex-col gap-1">
                  <p className="text-sm leading-relaxed whitespace-pre-wrap">
                    {msg.content}
                  </p>
                  <span className="text-[10px] text-gray-500 mt-2 self-end">
                    {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              </div>

              {msg.role === 'user' && (
                <div className="w-8 h-8 rounded-full bg-gray-700 flex items-center justify-center ml-3 flex-shrink-0 mt-1 border border-gray-600">
                  <User className="h-4 w-4 text-gray-300" />
                </div>
              )}
            </div>
          ))}
          
          {isLoading && (
            <div className="flex items-start animate-pulse">
              <div className="w-8 h-8 rounded-full bg-indigo-500/20 flex items-center justify-center mr-3">
                <Bot className="h-4 w-4 text-indigo-400" />
              </div>
              <div className="bg-gray-800/40 border border-gray-700/50 rounded-2xl rounded-tl-none p-4 flex items-center">
                <div className="flex space-x-1.5">
                  <div className="w-2 h-2 bg-indigo-400/60 rounded-full animate-bounce delay-0"></div>
                  <div className="w-2 h-2 bg-indigo-400/60 rounded-full animate-bounce delay-150"></div>
                  <div className="w-2 h-2 bg-indigo-400/60 rounded-full animate-bounce delay-300"></div>
                </div>
              </div>
            </div>
          )}
          <div ref={scrollRef} />
        </div>

        {/* Action Panel */}
        <div className="p-4 border-t border-gray-700/50 bg-gray-900/60">
          {/* Quick Suggestions (only if few messages) */}
          {messages.length < 5 && !isLoading && (
            <div className="flex flex-wrap gap-2 mb-4 animate-in fade-in duration-700">
              {SUGGESTIONS.map((s, i) => (
                <button
                  key={i}
                  onClick={() => handleSendMessage(s)}
                  className="text-xs px-3 py-1.5 rounded-full border border-gray-700 hover:border-indigo-400 hover:bg-indigo-500/10 text-gray-400 hover:text-indigo-300 transition-all flex items-center bg-gray-800/50"
                >
                  <ChevronRight className="h-3 w-3 mr-1 opacity-50" />
                  {s}
                </button>
              ))}
            </div>
          )}

          {/* Input Interface */}
          <div className="relative flex items-end gap-2 group">
            <div className="flex-1 relative">
              <Input
                placeholder="Ask the AI about your system..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                className="pr-12 py-6 bg-gray-800/80 border-gray-700/50 focus:border-indigo-500/50 hover:border-gray-600 transition-all"
              />
              <div className="absolute right-4 top-1/2 -translate-y-1/2 flex items-center text-xs text-gray-500 group-focus-within:opacity-0 transition-opacity">
                <span className="px-1.5 py-0.5 border border-gray-700 rounded mr-1">Enter</span>
                <Send className="h-3 w-3 ml-1" />
              </div>
            </div>
            
            <Button 
              size="md" 
              className="h-12 w-12 rounded-xl bg-indigo-600 hover:bg-indigo-500 shadow-lg shadow-indigo-500/20 active:scale-95 transition-all flex-shrink-0"
              onClick={() => handleSendMessage()}
              disabled={isLoading || !input.trim()}
            >
              <Send className="h-5 w-5" />
            </Button>
          </div>
          
          <p className="text-[10px] text-gray-500 mt-3 flex items-center justify-center">
            <Info className="h-3 w-3 mr-1 opacity-50" />
            AI can make mistakes. Please verify critical data in the main records.
          </p>
        </div>
      </CardContent>
    </Card>
  );
};

export default AIAssistantChat;
