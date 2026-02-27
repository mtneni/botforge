import { useEffect, useRef, useState } from 'react'
import hljs from 'highlight.js/lib/core'
// Register common languages
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import sql from 'highlight.js/lib/languages/sql'
import bash from 'highlight.js/lib/languages/bash'
import markdown from 'highlight.js/lib/languages/markdown'
import yaml from 'highlight.js/lib/languages/yaml'
import { Copy, Check } from 'lucide-react'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('json', json)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('yml', yaml)

interface CodeBlockProps {
    className?: string
    children: React.ReactNode
}

export function CodeBlock({ className, children }: CodeBlockProps) {
    const codeRef = useRef<HTMLElement>(null)
    const [copied, setCopied] = useState(false)

    // Extract language from className (format: "language-xxx")
    const lang = className?.replace(/language-/, '') || ''
    const code = String(children).replace(/\n$/, '')

    useEffect(() => {
        if (codeRef.current && lang) {
            // Only highlight if we have a registered language
            try {
                const result = hljs.highlight(code, { language: lang, ignoreIllegals: true })
                codeRef.current.innerHTML = result.value
            } catch {
                // Fall back to auto-detection or plain text
                try {
                    const result = hljs.highlightAuto(code)
                    codeRef.current.innerHTML = result.value
                } catch {
                    // Leave as plain text
                }
            }
        } else if (codeRef.current && !lang) {
            try {
                const result = hljs.highlightAuto(code)
                if (result.language) {
                    codeRef.current.innerHTML = result.value
                }
            } catch {
                // Leave as plain text
            }
        }
    }, [code, lang])

    const handleCopy = () => {
        navigator.clipboard.writeText(code)
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
    }

    return (
        <div className="code-block-wrapper">
            <div className="code-block-header">
                <span className="code-block-lang">{lang || 'code'}</span>
                <button
                    className={`code-copy-btn ${copied ? 'copied' : ''}`}
                    onClick={handleCopy}
                >
                    {copied ? <><Check size={12} /> Copied</> : <><Copy size={12} /> Copy</>}
                </button>
            </div>
            <pre>
                <code ref={codeRef} className={className}>
                    {code}
                </code>
            </pre>
        </div>
    )
}
