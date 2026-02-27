import { FileText, ExternalLink } from 'lucide-react';
import { Citation } from '../../api/sse';
import '../../styles/chat.css';

interface CitationListProps {
    citations: Citation[];
}

export function CitationList({ citations }: CitationListProps) {
    if (!citations || citations.length === 0) return null;

    return (
        <div className="citation-list">
            <div className="citation-header">
                <FileText size={12} />
                <span>Sources</span>
            </div>
            <div className="citation-items">
                {citations.map((citation, idx) => (
                    <div key={idx} className="citation-badge" title={citation.snippet}>
                        <span className="citation-title">{citation.title || citation.uri.split('/').pop()}</span>
                        {citation.uri.startsWith('http') && (
                            <a href={citation.uri} target="_blank" rel="noopener noreferrer" className="citation-link">
                                <ExternalLink size={10} />
                            </a>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}
