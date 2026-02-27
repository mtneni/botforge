import { Brain } from 'lucide-react';

interface MemoryInsightProps {
    proposition: string;
    status: string;
}

export function MemoryInsight({ proposition, status }: MemoryInsightProps) {
    return (
        <div className="memory-insight">
            <div className="memory-insight-icon">
                <Brain size={12} className="animate-pulse" />
            </div>
            <div className="memory-insight-content">
                <span className="memory-insight-label">
                    {status === 'learned' ? 'Learned' : 'Updated'}
                </span>
                <span className="memory-insight-text">{proposition}</span>
            </div>
        </div>
    );
}
