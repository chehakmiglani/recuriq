import React, { useEffect, useState } from 'react';
import * as CustomerService from '../../services/customer/customerService';
import {
  HelpCircle,
  MessageCircle,
  ChevronDown,
  ChevronUp,
  Send,
  Loader,
  Mail,
  AlertCircle
} from 'lucide-react';
import './SupportPage.css';

interface FAQ {
  question: string;
  answer: string;
  category: string;
}

export const SupportPage: React.FC = () => {
  const [faqs, setFaqs] = useState<FAQ[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedFaq, setExpandedFaq] = useState<number | null>(null);
  const [message, setMessage] = useState('');
  const [subject, setSubject] = useState('');
  const [category, setCategory] = useState('GENERAL');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);

  useEffect(() => {
    loadFAQs();
  }, []);

  const loadFAQs = async () => {
    try {
      const data = await CustomerService.getFAQs();
      setFaqs(data);
    } catch (error) {
      console.error('Failed to load FAQs:', error);
      setFaqs([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject.trim() || !message.trim()) return;

    setSending(true);
    try {
      await CustomerService.sendSupportMessage({
        subject,
        message,
        category
      });
      setSent(true);
      setSubject('');
      setMessage('');
      setTimeout(() => setSent(false), 5000);
    } catch (error) {
      alert('Failed to send message. Please try again.');
    } finally {
      setSending(false);
    }
  };

  const categories = [
    { value: 'GENERAL', label: 'General Inquiry' },
    { value: 'BILLING', label: 'Billing Issue' },
    { value: 'TECHNICAL', label: 'Technical Support' },
    { value: 'ACCOUNT', label: 'Account Management' },
    { value: 'FEATURE', label: 'Feature Request' }
  ];

  const groupedFAQs = faqs.reduce((acc, faq) => {
    if (!acc[faq.category]) acc[faq.category] = [];
    acc[faq.category].push(faq);
    return acc;
  }, {} as Record<string, FAQ[]>);

  if (loading) {
    return (
      <div className="support-loading">
        <div className="spinner" style={{ width: 40, height: 40, borderColor: '#d1d5db' }}></div>
        <p className="loading-text">Loading support center...</p>
      </div>
    );
  }

  return (
    <div className="support-page">
      {/* FAQs Section */}

      {/* FAQs Section */}
      <div className="support-card">
        <div className="card-header">
          <HelpCircle size={24} className="header-icon" />
          <h2 className="card-title">Frequently Asked Questions</h2>
        </div>
        
        {faqs.length > 0 ? (
          Object.entries(groupedFAQs).map(([category, categoryFaqs]) => (
            <div key={category} className="faq-category">
              <h3 className="category-title">{category}</h3>
              <div className="faq-list">
                {categoryFaqs.map((faq) => {
                  const faqIndex = faqs.indexOf(faq);
                  return (
                    <div key={faqIndex} className={`faq-item ${expandedFaq === faqIndex ? 'expanded' : ''}`}>
                      <button
                        className="faq-question"
                        onClick={() => setExpandedFaq(expandedFaq === faqIndex ? null : faqIndex)}
                      >
                        <span className="question-text">{faq.question}</span>
                        {expandedFaq === faqIndex ? (
                          <ChevronUp size={20} className="chevron" />
                        ) : (
                          <ChevronDown size={20} className="chevron" />
                        )}
                      </button>
                      {expandedFaq === faqIndex && (
                        <div className="faq-answer">
                          <p>{faq.answer}</p>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))
        ) : (
          <div className="empty-faqs">
            <AlertCircle size={40} />
            <p>No FAQs available at the moment</p>
          </div>
        )}
      </div>

      {/* Contact Form */}
      <div className="support-card">
        <div className="card-header">
          <MessageCircle size={24} className="header-icon" />
          <h2 className="card-title">Contact Support</h2>
        </div>

        {sent ? (
          <div className="success-message">
            <div className="success-icon">
              <Mail size={48} />
            </div>
            <h3>Message Sent!</h3>
            <p>Thank you for reaching out. Our support team will get back to you within 24 hours.</p>
          </div>
        ) : (
          <form onSubmit={handleSendMessage} className="support-form">
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Category</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="form-select"
                >
                  {categories.map((cat) => (
                    <option key={cat.value} value={cat.value}>
                      {cat.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Subject</label>
                <input
                  type="text"
                  className="form-input"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  placeholder="Brief description of your issue"
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Message</label>
              <textarea
                className="form-textarea"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="Describe your issue in detail..."
                rows={5}
                required
              />
            </div>

            <button
              type="submit"
              disabled={sending || !subject.trim() || !message.trim()}
              className="btn-primary btn-send"
            >
              {sending ? (
                <>
                  <Loader size={20} className="btn-spinner" /> Sending...
                </>
              ) : (
                <>
                  <Send size={20} /> Send Message
                </>
              )}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
