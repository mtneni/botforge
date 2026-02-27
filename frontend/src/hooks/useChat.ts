import React, { useState, useCallback, useEffect, useRef, createContext, useContext, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { connectSse, type SseEventHandler, type Citation } from '../api/sse';

export interface ChatMessage {
    role: 'user' | 'assistant' | 'error';
    content: string;
    citations?: Citation[];
    thoughts?: string[];
}

export interface MemoryInsightData {
    proposition: string;
    status: string;
    timestamp: number;
}

export interface ConversationData {
    id: string;
    title: string;
    persona: string;
    updatedAt: string;
}

interface ChatContextType {
    messages: ChatMessage[];
    setMessages: React.Dispatch<React.SetStateAction<ChatMessage[]>>;
    conversations: ConversationData[];
    activeConversationId: string | null;
    setActiveConversationId: (id: string | null) => void;
    sending: boolean;
    progress: string | null;
    connected: boolean;
    memoryInsights: MemoryInsightData[];
    connect: () => void;
    disconnect: () => void;
    sendMessage: (text: string) => Promise<void>;
    loadConversation: (id: string, skipMessages?: boolean) => Promise<void>;
    fetchConversations: () => Promise<void>;
    createNewChat: (title?: string, persona?: string) => Promise<string | undefined>;
    clearSession: () => Promise<void>;
    deleteConversation: (id: string) => Promise<void>;
    renameConversation: (id: string, newTitle: string) => Promise<void>;
}

const ChatContext = createContext<ChatContextType | null>(null);

export function ChatProvider({ children }: { children: ReactNode }) {
    const navigate = useNavigate();
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [conversations, setConversations] = useState<ConversationData[]>([]);
    const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
    const [sending, setSending] = useState(false);
    const [progress, setProgress] = useState<string | null>(null);
    const [connected, setConnected] = useState(false);
    const [memoryInsights, setMemoryInsights] = useState<MemoryInsightData[]>([]);
    const [streamingMessage, setStreamingMessage] = useState<ChatMessage | null>(null);
    const [thoughts, setThoughts] = useState<string[]>([]);
    const sseRef = useRef<EventSource | null>(null);
    const thoughtsRef = useRef<string[]>([]);

    const activeIdRef = useRef<string | null>(null);
    useEffect(() => { activeIdRef.current = activeConversationId; }, [activeConversationId]);

    const connect = useCallback(() => {
        if (sseRef.current) {
            sseRef.current.close();
        }

        const handlers: SseEventHandler = {
            onConnected: () => setConnected(true),
            onProgress: (data: any) => {
                const msg = typeof data === 'string' ? data : data.message;
                if (!data.conversationId || data.conversationId === activeIdRef.current) {
                    setProgress(msg);
                    thoughtsRef.current = [...thoughtsRef.current, msg];
                    setThoughts(thoughtsRef.current);
                }
            },
            onToken: (data: any) => {
                if (data.conversationId === activeIdRef.current) {
                    setSending(false);
                    setProgress(null);
                    setStreamingMessage({
                        role: 'assistant',
                        content: data.fullContent,
                        thoughts: thoughtsRef.current.length > 0 ? [...thoughtsRef.current] : undefined
                    });
                }
            },
            onMessage: (data: any) => {
                const { conversationId, role, content, citations } = data;
                if (conversationId === activeIdRef.current) {
                    setProgress(null);
                    setSending(false);
                    setStreamingMessage(null); // Clear streaming when final message arrives
                    setMessages((prev) => [...prev, { role, content, citations, thoughts: thoughtsRef.current.length > 0 ? [...thoughtsRef.current] : undefined }]);
                    thoughtsRef.current = [];
                    setThoughts([]);
                }
            },
            onMemory: (data: any) => {
                if (data.conversationId === activeIdRef.current) {
                    setMemoryInsights((prev) => [
                        ...prev,
                        { proposition: data.proposition, status: data.status, timestamp: Date.now() },
                    ]);
                    setTimeout(() => {
                        setMemoryInsights((prev) => prev.slice(1));
                    }, 8000);
                }
            },
            onError: (data: any) => {
                const conversationId = typeof data === 'string' ? null : data.conversationId;
                if (!conversationId || conversationId === activeIdRef.current) {
                    setProgress(null);
                    setSending(false);
                    setConnected(false);
                }
            },
        };

        sseRef.current = connectSse(handlers);
    }, []);

    const disconnect = useCallback(() => {
        sseRef.current?.close();
        sseRef.current = null;
        setConnected(false);
    }, []);

    const fetchConversations = useCallback(async () => {
        try {
            const data = await api.get<{ conversations: ConversationData[] }>('/api/chat/list');
            setConversations(data.conversations);
        } catch (e) {
            console.error('Failed to fetch conversations', e instanceof Error ? e.message : e);
        }
    }, []);

    const loadConversation = useCallback(async (id: string, skipMessages = false) => {
        if (!id) return;
        setActiveConversationId(id);
        activeIdRef.current = id;
        if (!skipMessages) {
            setMessages([]);
            try {
                const data = await api.get<{ messages: ChatMessage[] }>(`/api/chat/history/${id}`);
                setMessages(data.messages);
            } catch (e) {
                console.error('Failed to load conversation history', e instanceof Error ? e.message : e);
            }
        }
    }, [setMessages]);

    const createNewChat = useCallback(async (title = 'New Conversation', persona?: string) => {
        try {
            const chat = await api.post<ConversationData>('/api/chat/new', { title, persona });
            setConversations(prev => [chat, ...prev]);
            activeIdRef.current = chat.id;
            navigate(`/chat/${chat.id}`);
            return chat.id;
        } catch (e) {
            console.error('Failed to create new chat', e);
        }
    }, [navigate]);

    const sendMessage = useCallback(async (text: string) => {
        if (!text.trim()) return;

        let cid: string | null | undefined = activeIdRef.current;
        if (!cid) {
            cid = await createNewChat(text.slice(0, 30) + '...');
        }
        if (!cid) return;

        setMessages((prev) => [...prev, { role: 'user', content: text }]);
        setSending(true);
        setProgress(null);
        thoughtsRef.current = [];
        setThoughts([]);

        try {
            await api.post(`/api/chat/message/${cid}`, { message: text });
        } catch (e: any) {
            setSending(false);
            setMessages((prev) => [...prev, { role: 'error', content: e.message }]);
        }
    }, [createNewChat]);

    const clearSession = useCallback(async () => {
        if (activeConversationId) {
            await api.del(`/api/chat/session`);
        }
        setMessages([]);
        setActiveConversationId(null);
        activeIdRef.current = null;
        setProgress(null);
        navigate('/');
    }, [activeConversationId, navigate]);

    const deleteConversation = useCallback(async (id: string) => {
        try {
            await api.del(`/api/chat/${id}`);
            setConversations(prev => prev.filter(c => c.id !== id));
            if (activeConversationId === id) {
                clearSession();
            }
        } catch (e) {
            console.error('Failed to delete conversation', e);
        }
    }, [activeConversationId, clearSession]);

    const renameConversation = useCallback(async (id: string, newTitle: string) => {
        try {
            await api.put(`/api/chat/${id}/title`, { title: newTitle });
            setConversations(prev => prev.map(c => c.id === id ? { ...c, title: newTitle } : c));
        } catch (e) {
            console.error('Failed to rename conversation', e);
        }
    }, []);

    useEffect(() => {
        fetchConversations();
        return () => {
            sseRef.current?.close();
        };
    }, [fetchConversations]);

    const value: ChatContextType = {
        messages: [...messages, ...(streamingMessage ? [streamingMessage] : [])],
        setMessages,
        conversations,
        activeConversationId,
        setActiveConversationId,
        sending,
        progress,
        connected,
        memoryInsights,
        connect,
        disconnect,
        sendMessage,
        loadConversation,
        fetchConversations,
        createNewChat,
        clearSession,
        deleteConversation,
        renameConversation,
    };

    return React.createElement(ChatContext.Provider, { value }, children);
}

export function useChat() {
    const context = useContext(ChatContext);
    if (!context) {
        throw new Error('useChat must be used within a ChatProvider');
    }
    return context;
}
