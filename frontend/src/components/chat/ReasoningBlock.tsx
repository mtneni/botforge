import { useState, useRef, useEffect } from 'react';
import { ChevronDown, ChevronRight, BrainCircuit } from 'lucide-react';
import '../../styles/chat.css';

interface ReasoningBlockProps {
    thoughts: string[];
}

export function ReasoningBlock({ thoughts }: ReasoningBlockProps) {
    const [isExpanded, setIsExpanded] = useState(false);
    const scrollRef = useRef<HTMLDivElement>(null);

    // Auto-scroll when new thoughts arrive if expanded
    useEffect(() => {
        if (isExpanded && scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [thoughts, isExpanded]);

    if (!thoughts || thoughts.length === 0) {
        return null; // hide if nothing to show
    }

    return (
        <div className="reasoning-block">
            <button
                className="reasoning-header"
                onClick={() => setIsExpanded(!isExpanded)}
                aria-expanded={isExpanded}
            >
                <BrainCircuit size={16} className="reasoning-icon" />
                <span className="reasoning-title">
                    Analyzed {thoughts.length} step{thoughts.length !== 1 ? 's' : ''}
                </span>
                {isExpanded ? <ChevronDown size={14} className="reasoning-chevron" /> : <ChevronRight size={14} className="reasoning-chevron" />}
            </button>
            {isExpanded && (
                <div className="reasoning-body" ref={scrollRef}>
                    <ul className="reasoning-list">
                        {thoughts.map((thought, idx) => (
                            <li key={idx} className="reasoning-item">
                                <span className="reasoning-bullet">•</span>
                                <span className="reasoning-text">{thought}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
}
