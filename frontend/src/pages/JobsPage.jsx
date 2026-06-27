import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import ApplyModal from '../components/ApplyModal'
import api from '../api/axios'

const STATUS_COLORS = {
  SAVED: 'bg-gray-100 text-gray-600',
  RESUME_MATCHED: 'bg-blue-100 text-blue-700',
  READY_TO_APPLY: 'bg-indigo-100 text-indigo-700',
  APPLIED: 'bg-yellow-100 text-yellow-700',
  HR_CONTACTED: 'bg-purple-100 text-purple-700',
  INTERVIEW_SCHEDULED: 'bg-orange-100 text-orange-700',
  INTERVIEW_DONE: 'bg-teal-100 text-teal-700',
  OFFER: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-600',
  CLOSED: 'bg-gray-100 text-gray-400',
}

const ALL_STATUSES = [
  'SAVED', 'RESUME_MATCHED', 'READY_TO_APPLY', 'APPLIED', 'HR_CONTACTED',
  'INTERVIEW_SCHEDULED', 'INTERVIEW_DONE', 'OFFER', 'REJECTED', 'CLOSED',
]

const EMPTY_FORM = {
  title: '', company: '', location: '', platform: '',
  jobUrl: '', salary: '', jobType: '', description: '',
  postedDate: '', closingDate: '',
}

function StatusBadge({ status }) {
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_COLORS[status] ?? 'bg-gray-100 text-gray-500'}`}>
      {status?.replace(/_/g, ' ')}
    </span>
  )
}

function MatchScoreBadge({ score }) {
  if (score == null) return null
  const color = score >= 70 ? 'text-green-700 bg-green-50 border-green-200'
    : score >= 50 ? 'text-amber-700 bg-amber-50 border-amber-200'
    : 'text-red-700 bg-red-50 border-red-200'
  return (
    <span className={`inline-flex items-center border rounded-full px-2 py-0.5 text-xs font-bold ${color}`}>
      {score}% match
    </span>
  )
}

function JobForm({ initial = EMPTY_FORM, onSave, onCancel, saving }) {
  const [form, setForm] = useState(initial)
  const change = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  return (
    <form onSubmit={e => { e.preventDefault(); onSave(form) }} className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {[
          { name: 'title', label: 'Job Title *', placeholder: 'e.g. Backend Engineer' },
          { name: 'company', label: 'Company', placeholder: 'e.g. Acme Corp' },
          { name: 'location', label: 'Location', placeholder: 'e.g. Sydney, NSW' },
          { name: 'platform', label: 'Platform', placeholder: 'e.g. Seek, LinkedIn' },
          { name: 'salary', label: 'Salary', placeholder: 'e.g. 120,000 AUD' },
          { name: 'jobType', label: 'Job Type', placeholder: 'e.g. Full-time, Contract' },
        ].map(({ name, label, placeholder }) => (
          <div key={name}>
            <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
            <input
              name={name} value={form[name]} onChange={change} placeholder={placeholder}
              required={name === 'title'}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        ))}
      </div>
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Job URL</label>
        <input name="jobUrl" value={form.jobUrl} onChange={change} type="url"
          placeholder="https://seek.com.au/job/123456"
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        {[{ name: 'postedDate', label: 'Posted Date' }, { name: 'closingDate', label: 'Closing Date' }].map(({ name, label }) => (
          <div key={name}>
            <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
            <input type="date" name={name} value={form[name]} onChange={change}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        ))}
      </div>
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Job Description</label>
        <textarea name="description" value={form.description} onChange={change} rows={6}
          placeholder="Paste the full job description here…"
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
        />
      </div>
      <div className="flex gap-3">
        <button type="submit" disabled={saving}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors">
          {saving ? 'Saving…' : 'Save Job'}
        </button>
        <button type="button" onClick={onCancel}
          className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors">
          Cancel
        </button>
      </div>
    </form>
  )
}

function AtsMatchSection({ jobId, resumes, result, onResult }) {
  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleRunMatch = async () => {
    if (!selectedResumeId) return
    setLoading(true); setError(null)
    try {
      const r = await api.post(`/api/jobs/${jobId}/match`, { resumeId: Number(selectedResumeId) })
      onResult(r.data)
    } catch (e) {
      setError(e.response?.data?.error || 'ATS match failed. Please try again.')
    } finally { setLoading(false) }
  }

  return (
    <div className="border-t border-gray-100 px-4 pb-4 pt-3 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">ATS Match</h3>
        {result && (
          <span className={`text-xs font-bold px-2 py-0.5 rounded-full border ${
            result.atsScore >= 70 ? 'text-green-700 bg-green-50 border-green-200'
            : result.atsScore >= 50 ? 'text-amber-700 bg-amber-50 border-amber-200'
            : 'text-red-700 bg-red-50 border-red-200'
          }`}>
            {result.atsScore}%
          </span>
        )}
      </div>
      {resumes.length === 0 ? (
        <p className="text-xs text-gray-400">Upload a resume first to run an ATS match.</p>
      ) : (
        <div className="flex gap-2">
          <select value={selectedResumeId} onChange={e => setSelectedResumeId(e.target.value)}
            className="flex-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="">Select a resume…</option>
            {resumes.map(r => <option key={r.id} value={r.id}>{r.fileName}</option>)}
          </select>
          <button onClick={handleRunMatch} disabled={!selectedResumeId || loading}
            className="px-3 py-1.5 bg-blue-600 text-white text-xs font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors whitespace-nowrap">
            {loading ? 'Analysing…' : 'Run ATS Match'}
          </button>
        </div>
      )}
      {error && <p className="text-xs text-red-600">{error}</p>}
      {result && (
        <div className="space-y-2">
          <div className="grid grid-cols-2 gap-2">
            <div className="rounded-lg bg-green-50 border border-green-100 p-2.5">
              <p className="text-xs font-semibold text-green-700 mb-1">Matched Skills</p>
              <p className="text-xs text-green-800">{result.matchedSkills || '—'}</p>
            </div>
            <div className="rounded-lg bg-red-50 border border-red-100 p-2.5">
              <p className="text-xs font-semibold text-red-700 mb-1">Missing Skills</p>
              <p className="text-xs text-red-800">{result.missingSkills || '—'}</p>
            </div>
          </div>
          {[
            { label: 'Suggested Summary', value: result.suggestedSummary },
            { label: 'Suggested Skills', value: result.suggestedSkills },
            { label: 'Suggested Experience', value: result.suggestedExperience },
          ].map(({ label, value }) => value && (
            <div key={label} className="rounded-lg bg-gray-50 border border-gray-100 p-2.5">
              <p className="text-xs font-semibold text-gray-600 mb-1">{label}</p>
              <p className="text-xs text-gray-700 whitespace-pre-wrap">{value}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

const MESSAGE_TYPES = [
  { type: 'HR_EMAIL', label: 'HR Email' },
  { type: 'LINKEDIN', label: 'LinkedIn' },
  { type: 'FOLLOWUP', label: 'Follow-up' },
  { type: 'COVER_LETTER', label: 'Cover Letter' },
]

const TEMPLATE_LABELS = {
  HR_EMAIL: { field: 'defaultHrEmail', name: 'HR email template' },
  LINKEDIN: { field: 'defaultLinkedinMessage', name: 'LinkedIn InMail template' },
}

function MessagesSection({ jobId }) {
  const [messages, setMessages] = useState([])
  const [generating, setGenerating] = useState(null)
  const [error, setError] = useState(null)
  const [loaded, setLoaded] = useState(false)
  const [copiedId, setCopiedId] = useState(null)
  const [profile, setProfile] = useState(null)
  const [templateWarn, setTemplateWarn] = useState(null)

  useEffect(() => {
    api.get(`/api/jobs/${jobId}/messages`)
      .then(r => { setMessages(r.data); setLoaded(true) })
      .catch(() => setLoaded(true))
    api.get('/api/profile').then(r => setProfile(r.data)).catch(() => {})
  }, [jobId])

  const doGenerate = async (type) => {
    setTemplateWarn(null)
    setGenerating(type); setError(null)
    try {
      const r = await api.post(`/api/jobs/${jobId}/messages`, { type })
      setMessages(prev => [r.data, ...prev])
    } catch (e) {
      setError(e.response?.data?.error || 'Generation failed. Please try again.')
    } finally { setGenerating(null) }
  }

  const handleGenerate = (type) => {
    const meta = TEMPLATE_LABELS[type]
    if (meta && !profile?.[meta.field]?.trim()) {
      setTemplateWarn(type)
      return
    }
    doGenerate(type)
  }

  const handleCopy = (id, content) => {
    const blob = new Blob([content], { type: 'text/plain' })
    navigator.clipboard.write([new ClipboardItem({ 'text/plain': blob })])
    setCopiedId(id)
    setTimeout(() => setCopiedId(null), 2000)
  }

  return (
    <div className="border-t border-gray-100 px-4 pb-4 pt-3 space-y-3">
      <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Outreach & Cover Letter</h3>
      <div className="flex gap-2 flex-wrap">
        {MESSAGE_TYPES.map(({ type, label }) => (
          <button key={type} onClick={() => handleGenerate(type)}
            disabled={!!generating}
            className="px-3 py-1.5 bg-purple-600 text-white text-xs font-medium rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors">
            {generating === type ? 'Generating…' : `Generate ${label}`}
          </button>
        ))}
      </div>
      {templateWarn && (
        <div className="rounded-lg bg-amber-50 border border-amber-200 p-3 space-y-2">
          <p className="text-xs text-amber-800">
            <span className="font-semibold">No {TEMPLATE_LABELS[templateWarn].name} saved.</span>{' '}
            Add one in your Profile so AI Tracker can adapt your style for each job.
          </p>
          <div className="flex items-center gap-2 flex-wrap">
            <Link to="/profile"
              className="px-2.5 py-1 bg-amber-600 text-white text-xs font-medium rounded-md hover:bg-amber-700 transition-colors">
              Go to Profile →
            </Link>
            <button onClick={() => doGenerate(templateWarn)}
              className="px-2.5 py-1 border border-amber-300 text-amber-700 text-xs font-medium rounded-md hover:bg-amber-100 transition-colors">
              Generate anyway
            </button>
            <button onClick={() => setTemplateWarn(null)}
              className="text-xs text-amber-500 hover:text-amber-700 transition-colors">
              Dismiss
            </button>
          </div>
        </div>
      )}
      {error && <p className="text-xs text-red-600">{error}</p>}
      {loaded && messages.length > 0 && (
        <div className="space-y-3">
          {messages.map(msg => (
            <div key={msg.id} className="rounded-lg bg-purple-50 border border-purple-100 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-purple-700">
                  {MESSAGE_TYPES.find(t => t.type === msg.type)?.label ?? msg.type}
                </span>
                <button onClick={() => handleCopy(msg.id, msg.content)}
                  className="text-xs text-purple-600 hover:text-purple-800 transition-colors">
                  {copiedId === msg.id ? 'Copied!' : 'Copy'}
                </button>
              </div>
              <p className="text-xs text-gray-700 whitespace-pre-wrap">{msg.content}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function CompanyResearchSection({ jobId }) {
  const [research, setResearch] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    api.get(`/api/jobs/${jobId}/research`)
      .then(r => { setResearch(r.data); setLoaded(true) })
      .catch(() => setLoaded(true))
  }, [jobId])

  const runResearch = async () => {
    setLoading(true); setError(null)
    try {
      const r = await api.post(`/api/jobs/${jobId}/research`)
      setResearch(r.data)
    } catch (e) {
      setError(e.response?.data?.error || 'Research failed. Please try again.')
    } finally { setLoading(false) }
  }

  if (!loaded) return null

  return (
    <div className="border-t border-gray-100 px-4 pb-4 pt-3 space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Company Research</h3>
        <button onClick={runResearch} disabled={loading}
          className="px-3 py-1.5 bg-teal-600 text-white text-xs font-medium rounded-lg hover:bg-teal-700 disabled:opacity-50 transition-colors">
          {loading ? 'Researching…' : research ? '↻ Refresh' : 'Run Research'}
        </button>
      </div>
      {error && <p className="text-xs text-red-600">{error}</p>}
      {!research && !loading && (
        <p className="text-xs text-gray-400">
          Run company research before your interview — Tavily + Claude generates a briefing covering tech stack, culture, and tips.
        </p>
      )}
      {research && (
        <div className="rounded-lg bg-teal-50 border border-teal-100 p-3">
          <p className="text-xs text-teal-600 mb-2">
            {research.companyName} · {new Date(research.createdAt).toLocaleDateString('en-AU', { day: 'numeric', month: 'short' })}
          </p>
          <div className="text-xs text-gray-700 whitespace-pre-wrap leading-relaxed prose prose-xs max-w-none">
            {research.briefing}
          </div>
        </div>
      )}
    </div>
  )
}

const SOURCE_COLORS = {
  'seek.com.au': 'bg-blue-50 text-blue-700 border-blue-200',
  'linkedin.com': 'bg-sky-50 text-sky-700 border-sky-200',
  'indeed.com.au': 'bg-indigo-50 text-indigo-700 border-indigo-200',
  'jora.com': 'bg-purple-50 text-purple-700 border-purple-200',
  'careerone.com.au': 'bg-orange-50 text-orange-700 border-orange-200',
  'glassdoor.com.au': 'bg-green-50 text-green-700 border-green-200',
}

function SourceBadge({ source }) {
  const color = SOURCE_COLORS[source] || 'bg-gray-100 text-gray-600 border-gray-200'
  return (
    <span className={`inline-flex items-center border rounded-full px-2 py-0.5 text-xs font-medium ${color}`}>
      {source}
    </span>
  )
}

function getPageNumbers(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = []
  pages.push(1)
  if (current > 3) pages.push(null) // left ellipsis
  for (let p = Math.max(2, current - 1); p <= Math.min(total - 1, current + 1); p++) pages.push(p)
  if (current < total - 2) pages.push(null) // right ellipsis
  pages.push(total)
  return pages
}

function JobSearchTab({ onSaveJob }) {
  const [what, setWhat] = useState('')
  const [where, setWhere] = useState('')
  const [results, setResults] = useState([])
  const [totalCount, setTotalCount] = useState(0)
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [searched, setSearched] = useState(false)
  const [webResults, setWebResults] = useState([])
  const [webLoading, setWebLoading] = useState(false)
  const [webError, setWebError] = useState(null)
  const [saving, setSaving] = useState(null)
  const [savedJobs, setSavedJobs] = useState({})
  const [modalJob, setModalJob] = useState(null)

  const fetchPage = async (p, whatVal, whereVal) => {
    setLoading(true); setError(null)
    try {
      const r = await api.get('/api/job-search', {
        params: { what: whatVal.trim(), where: whereVal.trim(), results: 10, page: p }
      })
      setResults(r.data.results)
      setTotalCount(r.data.totalCount)
      setPage(r.data.page)
      setTotalPages(r.data.totalPages)
      setSearched(true)
    } catch (e) {
      setError(e.response?.data?.error || 'Search failed. Check your Adzuna API credentials.')
    } finally { setLoading(false) }
  }

  const fetchWebResults = async (whatVal, whereVal) => {
    setWebLoading(true); setWebError(null); setWebResults([])
    try {
      const r = await api.get('/api/job-search/web', {
        params: { what: whatVal.trim(), where: whereVal.trim(), results: 10 }
      })
      setWebResults(r.data)
    } catch (e) {
      setWebError(e.response?.data?.error || 'Web search failed.')
    } finally { setWebLoading(false) }
  }

  const handleSearch = async (e) => {
    e.preventDefault()
    if (!what.trim()) return
    setPage(1)
    setResults([])
    setWebResults([])
    fetchPage(1, what, where)
    fetchWebResults(what, where)
  }

  const handlePageChange = (p) => {
    if (p < 1 || p > totalPages || p === page) return
    window.scrollTo({ top: 0, behavior: 'smooth' })
    fetchPage(p, what, where)
  }

  const handleWantToApply = async (searchJob) => {
    setSaving(searchJob.externalId)
    try {
      const salary = searchJob.salaryMin && searchJob.salaryMax
        ? `${Math.round(searchJob.salaryMin).toLocaleString()} – ${Math.round(searchJob.salaryMax).toLocaleString()} AUD`
        : null
      const r = await api.post('/api/jobs', {
        title: searchJob.title,
        company: searchJob.company,
        location: searchJob.location,
        description: searchJob.description,
        jobUrl: searchJob.url,
        salary,
        jobType: searchJob.contractType,
        platform: 'Adzuna',
      })
      const savedJob = r.data
      setSavedJobs(prev => ({ ...prev, [searchJob.externalId]: { status: 'saved', job: savedJob } }))
      setModalJob(savedJob)
      onSaveJob()
    } catch {
      // silently ignore — user can try again
    } finally { setSaving(null) }
  }

  const handleModalApplied = () => {
    const entry = Object.entries(savedJobs).find(([, v]) => v.job?.id === modalJob?.id)
    if (entry) {
      setSavedJobs(prev => ({ ...prev, [entry[0]]: { ...prev[entry[0]], status: 'applied' } }))
    }
    setModalJob(null)
    onSaveJob()
  }


  const pageNumbers = getPageNumbers(page, totalPages)

  return (
    <div className="space-y-4">
      {modalJob && (
        <ApplyModal
          job={modalJob}
          onClose={() => setModalJob(null)}
          onApplied={handleModalApplied}
        />
      )}

      <form onSubmit={handleSearch} className="flex gap-3">
        <input value={what} onChange={e => setWhat(e.target.value)}
          placeholder="Job title or keywords (e.g. Java Developer)"
          className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <input value={where} onChange={e => setWhere(e.target.value)}
          placeholder="Location (e.g. Sydney)"
          className="w-40 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button type="submit" disabled={loading || !what.trim()}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors whitespace-nowrap">
          {loading ? 'Searching…' : 'Search Jobs'}
        </button>
      </form>

      <p className="text-xs text-gray-400">Match scores are calculated instantly against your uploaded resume.</p>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {searched && results.length === 0 && !loading && webResults.length === 0 && !webLoading && (
        <p className="text-sm text-gray-400 text-center py-8">No jobs found. Try different keywords.</p>
      )}

      {/* Adzuna results */}
      {results.length > 0 && (
        <div className="space-y-3">
          <p className="text-xs text-gray-500">
            {totalCount.toLocaleString()} total results · page {page} of {totalPages}
          </p>

          {results.map(job => {
            const savedState = savedJobs[job.externalId]
            return (
              <div key={job.externalId} className="bg-white rounded-xl border border-gray-200 p-4 space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-sm font-semibold text-gray-900">{job.title}</p>
                      {job.matchScore != null && <MatchScoreBadge score={job.matchScore} />}
                    </div>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {[job.company, job.location].filter(Boolean).join(' · ')}
                      {job.salaryMin && job.salaryMax && (
                        <span className="ml-2 text-green-700 font-medium">
                          ${Math.round(job.salaryMin / 1000)}k–${Math.round(job.salaryMax / 1000)}k
                        </span>
                      )}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {job.url && (
                      <a href={job.url} target="_blank" rel="noreferrer"
                        className="text-xs text-blue-600 hover:underline">View ↗</a>
                    )}
                    {savedState?.status === 'applied' ? (
                      <span className="px-3 py-1 text-xs font-medium text-green-700 bg-green-50 border border-green-200 rounded-lg">
                        Applied ✓
                      </span>
                    ) : savedState?.status === 'saved' ? (
                      <button onClick={() => setModalJob(savedState.job)}
                        className="px-3 py-1 bg-purple-600 text-white text-xs font-medium rounded-lg hover:bg-purple-700 transition-colors">
                        Open Application
                      </button>
                    ) : (
                      <button onClick={() => handleWantToApply(job)}
                        disabled={saving === job.externalId}
                        className="px-3 py-1 bg-blue-600 text-white text-xs font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors">
                        {saving === job.externalId ? 'Saving…' : 'Want to Apply'}
                      </button>
                    )}
                  </div>
                </div>
                {job.description && (
                  <p className="text-xs text-gray-500 line-clamp-3">{job.description}</p>
                )}
              </div>
            )
          })}

          {/* Adzuna Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-1 pt-2">
              <button
                onClick={() => handlePageChange(page - 1)}
                disabled={page === 1 || loading}
                className="px-3 py-1.5 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                ← Prev
              </button>

              {pageNumbers.map((p, i) =>
                p === null ? (
                  <span key={`ellipsis-${i}`} className="px-2 py-1.5 text-sm text-gray-400">…</span>
                ) : (
                  <button
                    key={p}
                    onClick={() => handlePageChange(p)}
                    disabled={loading}
                    className={`w-9 h-9 text-sm rounded-lg border transition-colors ${
                      p === page
                        ? 'bg-blue-600 text-white border-blue-600 font-semibold'
                        : 'border-gray-300 text-gray-600 hover:bg-gray-50'
                    }`}>
                    {p}
                  </button>
                )
              )}

              <button
                onClick={() => handlePageChange(page + 1)}
                disabled={page === totalPages || loading}
                className="px-3 py-1.5 text-sm rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                Next →
              </button>
            </div>
          )}
        </div>
      )}
      {/* Web Results — Tavily */}
      {searched && (webLoading || webResults.length > 0 || webError) && (
        <div className="space-y-3 pt-2">
          <div className="flex items-center gap-2">
            <div className="h-px flex-1 bg-gray-200" />
            <span className="text-xs font-semibold text-gray-400 uppercase tracking-wide">Web Results</span>
            <div className="h-px flex-1 bg-gray-200" />
          </div>

          {webLoading && (
            <div className="flex items-center gap-2 py-4 justify-center">
              <div className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
              <p className="text-xs text-gray-400">Searching Seek, LinkedIn, Indeed and more…</p>
            </div>
          )}

          {webError && <p className="text-xs text-red-500">{webError}</p>}

          {webResults.map(job => {
            const savedState = savedJobs[job.url]
            return (
              <div key={job.url} className="bg-white rounded-xl border border-gray-200 p-4 space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-sm font-semibold text-gray-900">{job.title}</p>
                      {job.matchScore != null && <MatchScoreBadge score={job.matchScore} />}
                      <SourceBadge source={job.source} />
                    </div>
                  </div>
                  <div className="shrink-0">
                    <a href={job.url} target="_blank" rel="noreferrer"
                      className="px-3 py-1 bg-gray-100 text-gray-700 text-xs font-medium rounded-lg hover:bg-gray-200 transition-colors whitespace-nowrap">
                      Browse on {job.source} ↗
                    </a>
                  </div>
                </div>
                {job.snippet && (
                  <p className="text-xs text-gray-500 line-clamp-3">{job.snippet}</p>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default function JobsPage() {
  const [tab, setTab] = useState('my-jobs')
  const [jobs, setJobs] = useState([])
  const [resumes, setResumes] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editingJob, setEditingJob] = useState(null)
  const [expandedId, setExpandedId] = useState(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [matchResults, setMatchResults] = useState({})

  const load = useCallback(() =>
    api.get('/api/jobs').then(r => setJobs(r.data)).catch(() => setError('Failed to load jobs.')), [])

  useEffect(() => {
    Promise.all([
      load(),
      api.get('/api/resumes').then(r => setResumes(r.data)).catch(() => {}),
    ]).finally(() => setLoading(false))
  }, [load])

  const handleExpand = (job) => {
    setExpandedId(expandedId === job.id ? null : job.id)
  }

  const handleCreate = async (form) => {
    setSaving(true); setError(null)
    try { await api.post('/api/jobs', form); await load(); setShowForm(false) }
    catch (e) { setError(e.response?.data?.error || 'Failed to save job.') }
    finally { setSaving(false) }
  }

  const handleUpdate = async (form) => {
    setSaving(true); setError(null)
    try { await api.put(`/api/jobs/${editingJob.id}`, form); await load(); setEditingJob(null) }
    catch (e) { setError(e.response?.data?.error || 'Failed to update job.') }
    finally { setSaving(false) }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this job and its application record?')) return
    try {
      await api.delete(`/api/jobs/${id}`)
      setJobs(j => j.filter(x => x.id !== id))
      if (expandedId === id) setExpandedId(null)
    } catch { setError('Failed to delete job.') }
  }

  const handleStatusUpdate = async (jobId, status) => {
    try {
      await api.put(`/api/jobs/${jobId}/status`, { status })
      await load()
    } catch {}
  }

  if (loading) return <Layout><div className="text-sm text-gray-400 py-20 text-center">Loading…</div></Layout>

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Jobs</h1>
            <p className="text-gray-500 mt-1 text-sm">{jobs.length} saved job{jobs.length !== 1 ? 's' : ''}</p>
          </div>
          {tab === 'my-jobs' && !showForm && !editingJob && (
            <button onClick={() => setShowForm(true)}
              className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors">
              + Add Job
            </button>
          )}
        </div>

        <div className="flex gap-1 border-b border-gray-200">
          {[{ id: 'my-jobs', label: 'My Jobs' }, { id: 'search', label: 'Search Jobs' }].map(t => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                tab === t.id ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}>
              {t.label}
            </button>
          ))}
        </div>

        {tab === 'search' && <JobSearchTab onSaveJob={load} />}

        {tab === 'my-jobs' && (
          <>
            {error && <p className="text-sm text-red-600">{error}</p>}

            {showForm && (
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h2 className="text-base font-semibold text-gray-800 mb-4">Add New Job</h2>
                <JobForm onSave={handleCreate} onCancel={() => setShowForm(false)} saving={saving} />
              </div>
            )}

            {jobs.length === 0 && !showForm ? (
              <div className="bg-white rounded-xl border border-gray-200 p-10 text-center">
                <p className="text-gray-400 text-sm">No jobs saved yet. Click <strong>+ Add Job</strong> or use <strong>Search Jobs</strong> to find roles.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {jobs.map(job => (
                  <div key={job.id} className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                    {editingJob?.id === job.id ? (
                      <div className="p-6">
                        <h2 className="text-base font-semibold text-gray-800 mb-4">Edit Job</h2>
                        <JobForm
                          initial={{
                            title: job.title ?? '', company: job.company ?? '', location: job.location ?? '',
                            platform: job.platform ?? '', jobUrl: job.jobUrl ?? '', salary: job.salary ?? '',
                            jobType: job.jobType ?? '', description: job.description ?? '',
                            postedDate: job.postedDate ?? '', closingDate: job.closingDate ?? '',
                          }}
                          onSave={handleUpdate} onCancel={() => setEditingJob(null)} saving={saving}
                        />
                      </div>
                    ) : (
                      <>
                        <div className="flex items-start justify-between p-4 cursor-pointer hover:bg-gray-50 transition-colors"
                          onClick={() => handleExpand(job)}>
                          <div className="space-y-1 flex-1 min-w-0 pr-4">
                            <div className="flex items-center gap-2 flex-wrap">
                              <p className="text-sm font-semibold text-gray-900">{job.title}</p>
                              <StatusBadge status={job.applicationStatus} />
                              {matchResults[job.id] && (
                                <span className={`text-xs font-bold px-2 py-0.5 rounded-full border ${
                                  matchResults[job.id].atsScore >= 70 ? 'text-green-700 bg-green-50 border-green-200'
                                  : matchResults[job.id].atsScore >= 50 ? 'text-amber-700 bg-amber-50 border-amber-200'
                                  : 'text-red-700 bg-red-50 border-red-200'
                                }`}>ATS {matchResults[job.id].atsScore}%</span>
                              )}
                            </div>
                            <p className="text-xs text-gray-500">
                              {[job.company, job.location, job.platform].filter(Boolean).join(' · ')}
                            </p>
                          </div>
                          <span className="text-gray-400 text-xs mt-0.5 shrink-0">
                            {expandedId === job.id ? '▲' : '▼'}
                          </span>
                        </div>

                        {expandedId === job.id && (
                          <>
                            <div className="border-t border-gray-100 px-4 pb-4 pt-3 space-y-3">
                              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-xs text-gray-600">
                                {job.salary && <span><strong>Salary:</strong> {job.salary}</span>}
                                {job.jobType && <span><strong>Type:</strong> {job.jobType}</span>}
                                {job.postedDate && <span><strong>Posted:</strong> {job.postedDate}</span>}
                                {job.closingDate && <span><strong>Closes:</strong> {job.closingDate}</span>}
                                {job.jobUrl && (
                                  <a href={job.jobUrl} target="_blank" rel="noreferrer"
                                    onClick={e => e.stopPropagation()}
                                    className="text-blue-600 hover:underline col-span-2">
                                    View Job Posting ↗
                                  </a>
                                )}
                              </div>
                              {job.description && (
                                <p className="text-xs text-gray-500 whitespace-pre-wrap line-clamp-6">{job.description}</p>
                              )}

                              {/* Status update */}
                              <div className="flex items-center gap-3 pt-1 flex-wrap">
                                <div className="flex items-center gap-2">
                                  <label className="text-xs text-gray-500">Status:</label>
                                  <select
                                    value={job.applicationStatus ?? ''}
                                    onClick={e => e.stopPropagation()}
                                    onChange={e => { e.stopPropagation(); handleStatusUpdate(job.id, e.target.value) }}
                                    className="text-xs rounded-lg border border-gray-300 px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                                  >
                                    {ALL_STATUSES.map(s => (
                                      <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
                                    ))}
                                  </select>
                                </div>
                                <button onClick={() => setEditingJob(job)}
                                  className="text-xs text-blue-600 hover:text-blue-800 transition-colors">Edit</button>
                                <button onClick={() => handleDelete(job.id)}
                                  className="text-xs text-red-500 hover:text-red-700 transition-colors">Delete</button>
                              </div>
                            </div>

                            <AtsMatchSection
                              jobId={job.id}
                              resumes={resumes}
                              result={matchResults[job.id]}
                              onResult={(result) => {
                                setMatchResults(prev => ({ ...prev, [job.id]: result }))
                                load()
                              }}
                            />

                            <MessagesSection jobId={job.id} />

                            <CompanyResearchSection jobId={job.id} />
                          </>
                        )}
                      </>
                    )}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </Layout>
  )
}
