import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Layout from '../components/Layout'
import api from '../api/axios'

const STATUS_LABEL = {
  SAVED: 'Saved',
  RESUME_MATCHED: 'Matched',
  READY_TO_APPLY: 'Ready',
  APPLIED: 'Applied',
  HR_CONTACTED: 'HR Contacted',
  INTERVIEW_SCHEDULED: 'Interview',
  INTERVIEW_DONE: 'Done',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  CLOSED: 'Closed',
}

const STATUS_COLOR = {
  SAVED: 'bg-gray-100 text-gray-600',
  RESUME_MATCHED: 'bg-blue-100 text-blue-700',
  READY_TO_APPLY: 'bg-cyan-100 text-cyan-700',
  APPLIED: 'bg-indigo-100 text-indigo-700',
  HR_CONTACTED: 'bg-purple-100 text-purple-700',
  INTERVIEW_SCHEDULED: 'bg-amber-100 text-amber-700',
  INTERVIEW_DONE: 'bg-orange-100 text-orange-700',
  OFFER: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  CLOSED: 'bg-gray-100 text-gray-500',
}

function StatCard({ label, value, sub, color }) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
      <p className={`text-3xl font-bold mt-1 ${color || 'text-gray-900'}`}>{value}</p>
      {sub && <p className="text-xs text-gray-400 mt-1">{sub}</p>}
    </div>
  )
}

function StatusBadge({ status }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLOR[status] || 'bg-gray-100 text-gray-600'}`}>
      {STATUS_LABEL[status] || status}
    </span>
  )
}

export default function DashboardPage() {
  const [jobs, setJobs] = useState([])
  const [hasResume, setHasResume] = useState(null)
  const [hasCoverLetter, setHasCoverLetter] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      api.get('/api/jobs'),
      api.get('/api/resumes'),
      api.get('/api/cover-letters'),
    ]).then(([jobsRes, resumesRes, clRes]) => {
      setJobs(jobsRes.data)
      setHasResume(resumesRes.data.length > 0)
      setHasCoverLetter(clRes.data.length > 0)
    }).finally(() => setLoading(false))
  }, [])

  const total = jobs.length
  const applied = jobs.filter(j => ['APPLIED','HR_CONTACTED','INTERVIEW_SCHEDULED','INTERVIEW_DONE','OFFER','REJECTED','CLOSED'].includes(j.applicationStatus)).length
  const interviews = jobs.filter(j => ['INTERVIEW_SCHEDULED','INTERVIEW_DONE'].includes(j.applicationStatus)).length
  const offers = jobs.filter(j => j.applicationStatus === 'OFFER').length

  // Pipeline stage counts
  const pipeline = [
    { label: 'Saved', statuses: ['SAVED'], color: 'bg-gray-400' },
    { label: 'Matched', statuses: ['RESUME_MATCHED', 'READY_TO_APPLY'], color: 'bg-blue-400' },
    { label: 'Applied', statuses: ['APPLIED', 'HR_CONTACTED'], color: 'bg-indigo-500' },
    { label: 'Interview', statuses: ['INTERVIEW_SCHEDULED', 'INTERVIEW_DONE'], color: 'bg-amber-400' },
    { label: 'Offer', statuses: ['OFFER'], color: 'bg-green-500' },
  ].map(stage => ({
    ...stage,
    count: jobs.filter(j => stage.statuses.includes(j.applicationStatus)).length,
  }))

  const rejected = jobs.filter(j => j.applicationStatus === 'REJECTED').length
  const closed = jobs.filter(j => j.applicationStatus === 'CLOSED').length

  // Recent 5 jobs by savedDate descending
  const recent = [...jobs]
    .sort((a, b) => new Date(b.savedDate) - new Date(a.savedDate))
    .slice(0, 5)

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center h-48">
          <div className="w-6 h-6 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-500 mt-1 text-sm">Your job search at a glance.</p>
        </div>

        {/* Setup checklist — only shown if something is missing */}
        {(!hasResume || !hasCoverLetter) && (
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 space-y-2">
            <p className="text-sm font-semibold text-amber-800">Complete your setup</p>
            <div className="space-y-1">
              {!hasResume && (
                <div className="flex items-center gap-2 text-sm text-amber-700">
                  <span className="text-amber-400">○</span>
                  <Link to="/resumes" className="underline hover:text-amber-900">Upload your resume</Link>
                  <span className="text-xs text-amber-500">— required for ATS matching</span>
                </div>
              )}
              {!hasCoverLetter && (
                <div className="flex items-center gap-2 text-sm text-amber-700">
                  <span className="text-amber-400">○</span>
                  <Link to="/cover-letters" className="underline hover:text-amber-900">Upload a cover letter</Link>
                  <span className="text-xs text-amber-500">— used as base template for AI generation</span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Stats row */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard label="Jobs Tracked" value={total} sub={total === 0 ? 'Search jobs to get started' : `${total} saved`} />
          <StatCard label="Applied" value={applied} color="text-indigo-600" sub={total > 0 ? `${Math.round(applied / total * 100)}% of tracked` : undefined} />
          <StatCard label="Interviews" value={interviews} color="text-amber-600" />
          <StatCard label="Offers" value={offers} color="text-green-600" />
        </div>

        {/* Pipeline */}
        {total > 0 && (
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Application Pipeline</h2>
            <div className="flex items-end gap-2">
              {pipeline.map(stage => (
                <div key={stage.label} className="flex-1 flex flex-col items-center gap-1.5">
                  <span className="text-xs font-bold text-gray-700">{stage.count}</span>
                  <div
                    className={`w-full rounded-t-lg ${stage.color} transition-all`}
                    style={{ height: `${Math.max(stage.count > 0 ? 12 : 4, stage.count * 16)}px`, minHeight: '4px' }}
                  />
                  <span className="text-xs text-gray-500 text-center">{stage.label}</span>
                </div>
              ))}
            </div>
            {(rejected > 0 || closed > 0) && (
              <div className="flex gap-4 mt-4 pt-3 border-t border-gray-100">
                {rejected > 0 && <span className="text-xs text-red-500">{rejected} Rejected</span>}
                {closed > 0 && <span className="text-xs text-gray-400">{closed} Closed</span>}
              </div>
            )}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Recent activity */}
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-gray-700">Recent Jobs</h2>
              <Link to="/jobs" className="text-xs text-blue-600 hover:text-blue-800">View all →</Link>
            </div>
            {recent.length === 0 ? (
              <div className="py-8 text-center">
                <p className="text-sm text-gray-400">No jobs saved yet.</p>
                <Link to="/jobs" className="text-sm text-blue-600 hover:text-blue-800 mt-1 inline-block">Search jobs →</Link>
              </div>
            ) : (
              <div className="space-y-3">
                {recent.map(job => (
                  <div key={job.id} className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-800 truncate">{job.title}</p>
                      <p className="text-xs text-gray-400 truncate">{job.company}{job.location ? ` · ${job.location}` : ''}</p>
                    </div>
                    <StatusBadge status={job.applicationStatus} />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Quick actions */}
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <h2 className="text-sm font-semibold text-gray-700 mb-4">Quick Actions</h2>
            <div className="space-y-2">
              <Link to="/jobs"
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors group">
                <div className="w-8 h-8 rounded-lg bg-blue-50 flex items-center justify-center text-blue-600 group-hover:bg-blue-100 transition-colors">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-700">Search Jobs</p>
                  <p className="text-xs text-gray-400">Find Australian jobs via Adzuna</p>
                </div>
              </Link>
              <Link to="/resumes"
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors group">
                <div className="w-8 h-8 rounded-lg bg-green-50 flex items-center justify-center text-green-600 group-hover:bg-green-100 transition-colors">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-700">{hasResume ? 'Manage Resume' : 'Upload Resume'}</p>
                  <p className="text-xs text-gray-400">{hasResume ? 'Replace or view your current resume' : 'Required for ATS matching'}</p>
                </div>
              </Link>
              <Link to="/cover-letters"
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors group">
                <div className="w-8 h-8 rounded-lg bg-purple-50 flex items-center justify-center text-purple-600 group-hover:bg-purple-100 transition-colors">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-700">{hasCoverLetter ? 'Manage Cover Letter' : 'Upload Cover Letter'}</p>
                  <p className="text-xs text-gray-400">{hasCoverLetter ? 'Replace or view your cover letter template' : 'Base template for AI generation'}</p>
                </div>
              </Link>
              <Link to="/jobs"
                className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 transition-colors group">
                <div className="w-8 h-8 rounded-lg bg-amber-50 flex items-center justify-center text-amber-600 group-hover:bg-amber-100 transition-colors">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-700">My Jobs</p>
                  <p className="text-xs text-gray-400">Track status, ATS results, and messages</p>
                </div>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  )
}
