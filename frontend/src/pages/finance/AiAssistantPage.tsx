import React, { useEffect, useRef, useState } from 'react';
import { Bot, Send, Sparkles, LoaderCircle, Database } from 'lucide-react';
import { PageHeader } from '../../components/admin/shared/PageHeader';
import { aiService } from '../../services/ai/aiService';
import type { ChatTurn } from '../../services/ai/aiService';

interface ChatMessage extends ChatTurn {
  toolsUsed?: string[];
  isError?: boolean;
}

const SUGGESTIONS = [
  'Give me a summary of how the business is doing this month.',
  'Which customers are most likely to cancel, and why?',
  'Which plan brings in the most revenue?',
  'How much revenue did we lose to churn, and what are the main reasons?',
  'Are failed payments a problem right now?',
];

const TOOL_LABELS: Record<string, string> = {
  get_finance_dashboard: 'Dashboard KPIs',
  get_mrr_report: 'MRR report',
  get_arr_report: 'ARR report',
  get_churn_report: 'Churn report',
  get_arpu_ltv_report: 'ARPU & LTV',
  get_churn_risk: 'Churn risk scores',
  get_recent_invoices: 'Invoices',
  get_recent_payments: 'Payments',
  get_invoice_detail: 'Invoice detail',
  get_refunds: 'Refunds',
};

// Minimal markdown: **bold**, *italic*, `code`, bullet lists and pipe tables, which covers what the models produce
const renderInline = (text: string) =>
  text.split(/(\*\*[^*]+\*\*|\*[^*\s][^*]*\*|`[^`]+`)/g).map((part, i) => {
    if (part.length > 4 && part.startsWith('**') && part.endsWith('**')) return <strong key={i}>{part.slice(2, -2)}</strong>;
    if (part.length > 2 && part.startsWith('*') && part.endsWith('*')) return <em key={i}>{part.slice(1, -1)}</em>;
    if (part.length > 2 && part.startsWith('`') && part.endsWith('`')) return <code key={i}>{part.slice(1, -1)}</code>;
    return part;
  });

const MarkdownText: React.FC<{ text: string }> = ({ text }) => {
  const lines = text.split('\n');
  const blocks: React.ReactNode[] = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i].trim();
    if (line.startsWith('|')) {
      const rows: string[][] = [];
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        const cells = lines[i].trim().replace(/^\||\|$/g, '').split('|').map((c) => c.trim());
        if (!cells.every((c) => /^:?-{2,}:?$/.test(c))) rows.push(cells);
        i++;
      }
      const [header, ...body] = rows;
      blocks.push(
        <table key={blocks.length} className="table table-sm" style={{ margin: '8px 0', fontSize: '13px' }}>
          <thead><tr>{header.map((c, j) => <th key={j}>{renderInline(c)}</th>)}</tr></thead>
          <tbody>{body.map((r, k) => <tr key={k}>{r.map((c, j) => <td key={j}>{renderInline(c)}</td>)}</tr>)}</tbody>
        </table>,
      );
      continue;
    }
    if (/^[-*] /.test(line)) {
      const items: string[] = [];
      while (i < lines.length && /^[-*] /.test(lines[i].trim())) {
        items.push(lines[i].trim().slice(2));
        i++;
      }
      blocks.push(<ul key={blocks.length} style={{ margin: '4px 0', paddingLeft: '20px' }}>{items.map((it, k) => <li key={k}>{renderInline(it)}</li>)}</ul>);
      continue;
    }
    if (line.startsWith('#')) {
      blocks.push(<div key={blocks.length} style={{ fontWeight: 700, margin: '8px 0 4px' }}>{renderInline(line.replace(/^#+\s*/, ''))}</div>);
    } else if (line) {
      blocks.push(<p key={blocks.length} style={{ margin: '4px 0' }}>{renderInline(line)}</p>);
    }
    i++;
  }
  return <>{blocks}</>;
};

export const AiAssistantPage: React.FC = () => {
  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [provider, setProvider] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    aiService.getStatus()
      .then((s) => { setEnabled(s.assistantEnabled); setProvider(s.provider); })
      .catch(() => setEnabled(false));
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const send = async (question: string) => {
    const text = question.trim();
    if (!text || loading) return;
    const history: ChatTurn[] = messages
      .filter((m) => !m.isError)
      .map(({ role, content }) => ({ role, content }));
    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setInput('');
    setLoading(true);
    try {
      const res = await aiService.ask(text, history);
      setMessages((prev) => [...prev, { role: 'assistant', content: res.answer, toolsUsed: res.toolsUsed }]);
    } catch (e) {
      setMessages((prev) => [...prev, { role: 'assistant', content: (e as Error).message, isError: true }]);
    } finally {
      setLoading(false);
    }
  };

  if (enabled === false) {
    return (
      <>
        <PageHeader subtitle="Ask questions about your revenue in plain English." />
        <div className="data-panel" style={{ padding: '32px', textAlign: 'center', color: '#4b5563' }}>
          <Bot size={36} style={{ marginBottom: '12px', color: '#9ca3af' }} />
          <h3 style={{ fontSize: '18px', marginBottom: '8px' }}>The AI assistant is not set up yet</h3>
          <p style={{ margin: 0 }}>
            Set the <code>GROQ_API_KEY</code> environment variable (free key from console.groq.com) or <code>ANTHROPIC_API_KEY</code> on the backend, then restart it.
          </p>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader subtitle={`Ask questions about your revenue in plain English. Answers use live data from RecurIQ${provider ? ` · Powered by ${provider}` : ''}.`} />

      <div className="data-panel" style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 220px)', minHeight: '480px' }}>
        <div style={{ flex: 1, overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {messages.length === 0 && (
            <div style={{ margin: 'auto', textAlign: 'center', maxWidth: '560px' }}>
              <Sparkles size={32} style={{ color: '#0f766e', marginBottom: '12px' }} />
              <h3 style={{ fontSize: '18px', marginBottom: '16px' }}>What would you like to know?</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    onClick={() => send(s)}
                    disabled={enabled === null}
                    style={{
                      textAlign: 'left', padding: '10px 14px', borderRadius: '10px', cursor: 'pointer',
                      border: '1px solid #e5e7eb', background: '#f9fafb', color: '#374151', fontSize: '14px',
                    }}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((m, idx) => (
            <div key={idx} style={{ display: 'flex', justifyContent: m.role === 'user' ? 'flex-end' : 'flex-start' }}>
              <div
                style={{
                  maxWidth: '80%', padding: '12px 16px', borderRadius: '14px', fontSize: '14px', lineHeight: 1.55,
                  background: m.role === 'user' ? '#111827' : m.isError ? '#fef2f2' : '#f3f4f6',
                  color: m.role === 'user' ? '#ffffff' : m.isError ? '#b91c1c' : '#111827',
                }}
              >
                {m.role === 'user' ? m.content : <MarkdownText text={m.content} />}
                {m.toolsUsed && m.toolsUsed.length > 0 && (
                  <div style={{ marginTop: '10px', display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap', fontSize: '12px', color: '#6b7280' }}>
                    <Database size={12} /> Data used:
                    {[...new Set(m.toolsUsed)].map((t) => (
                      <span key={t} style={{ background: '#ffffff', border: '1px solid #e5e7eb', borderRadius: '999px', padding: '1px 8px' }}>
                        {TOOL_LABELS[t] ?? t}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}

          {loading && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#6b7280', fontSize: '14px' }}>
              <LoaderCircle size={16} className="spin" style={{ animation: 'spin 1s linear infinite' }} />
              Looking at the numbers...
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        <form
          onSubmit={(e) => { e.preventDefault(); send(input); }}
          style={{ display: 'flex', gap: '10px', padding: '16px', borderTop: '1px solid #e5e7eb' }}
        >
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="e.g. Why did MRR change last month?"
            maxLength={2000}
            disabled={loading || enabled === null}
            className="form-control"
            style={{ flex: 1 }}
          />
          <button type="submit" className="btn-admin-primary" disabled={loading || !input.trim()}>
            <Send size={16} /> Ask
          </button>
        </form>
      </div>
      <style>{'@keyframes spin { to { transform: rotate(360deg); } }'}</style>
    </>
  );
};
