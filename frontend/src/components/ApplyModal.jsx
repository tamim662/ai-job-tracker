import { useEffect, useState } from 'react'
import api from '../api/axios'

function ScoreBadge({ score }) {
  const color = score >= 70 ? 'text-green-700 bg-green-50 border-green-200'
    : score >= 50 ? 'text-amber-700 bg-amber-50 border-amber-200'
    : 'text-red-700 bg-red-50 border-red-200'
  return (
    <span className={`inline-flex items-center border rounded-full px-2.5 py-0.5 text-xs font-bold ${color}`}>
      ATS {score}%
    </span>
  )
}

export default function ApplyModal({ job, onClose, onApplied }) {
  const [atsStep, setAtsStep] = useState('loading') // 'loading' | 'done'
  const [atsResult, setAtsResult] = useState(null)
  const [atsError, setAtsError] = useState(null)
  const [path, setPath] = useState(null)           // null | 'contact' | 'direct'
  const [directStep, setDirectStep] = useState(null) // null | 'cover-letter' | 'skip'
  const [generated, setGenerated] = useState(null)
  const [generating, setGenerating] = useState(false)
  const [genError, setGenError] = useState(null)
  const [markingApplied, setMarkingApplied] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    api.get('/api/resumes')
      .then(r => {
        const resume = r.data[0]
        if (!resume) {
          setAtsError('No resume found. Upload a resume first to run ATS analysis.')
          setAtsStep('done')
          return Promise.resolve(null)
        }
        return api.post(`/api/jobs/${job.id}/match`, { resumeId: resume.id })
      })
      .then(r => {
        if (r) setAtsResult(r.data)
        setAtsStep('done')
      })
      .catch(() => {
        setAtsError('ATS analysis failed. You can still proceed.')
        setAtsStep('done')
      })
  }, [job.id])

  const handleGenerate = async (type) => {
    setGenerating(true)
    setGenError(null)
    try {
      const r = await api.post(`/api/jobs/${job.id}/messages`, { type })
      setGenerated({ type, content: r.data.content })
    } catch (e) {
      setGenError(e.response?.data?.error || 'Generation failed. Please try again.')
    } finally {
      setGenerating(false)
    }
  }

  const handleMarkApplied = async () => {
    setMarkingApplied(true)
    try {
      await api.put(`/api/jobs/${job.id}/status`, { status: 'APPLIED' })
      onApplied()
      onClose()
    } catch {
      setMarkingApplied(false)
    }
  }

  const handleCopy = (text) => {
    const blob = new Blob([text], { type: 'text/plain' })
    navigator.clipboard.write([new ClipboardItem({ 'text/plain': blob })])
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const goBackFromDirect = () => {
    setPath(null)
    setDirectStep(null)
    setGenerated(null)
    setGenError(null)
  }

  return (
    <div
      className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="bg-white rounded-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto shadow-2xl">
        {/* Header */}
        <div className="flex items-start justify-between p-6 border-b border-gray-100 sticky top-0 bg-white rounded-t-2xl z-10">
          <div>
            <h2 className="text-xl font-bold text-gray-900">{job.title}</h2>
            {job.company && <p className="text-sm text-gray-500 mt-0.5">{job.company}{job.location ? ` · ${job.location}` : ''}</p>}
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none ml-4 mt-0.5">×</button>
        </div>

        <div className="p-6 space-y-5">
          {/* ATS Analysis */}
          <div className="rounded-xl border border-gray-200 p-4 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-gray-700">ATS Match Analysis</h3>
              {atsResult && <ScoreBadge score={atsResult.atsScore} />}
            </div>

            {atsStep === 'loading' && (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
                <p className="text-xs text-gray-400">Analysing your resume against this job…</p>
              </div>
            )}

            {atsStep === 'done' && atsError && (
              <p className="text-xs text-amber-600">{atsError}</p>
            )}

            {atsStep === 'done' && atsResult && (
              <div className="space-y-2">
                <div className="grid grid-cols-2 gap-2">
                  <div className="rounded-lg bg-green-50 border border-green-100 p-2.5">
                    <p className="text-xs font-semibold text-green-700 mb-1">Matched Skills</p>
                    <p className="text-xs text-green-800">{atsResult.matchedSkills || '—'}</p>
                  </div>
                  <div className="rounded-lg bg-red-50 border border-red-100 p-2.5">
                    <p className="text-xs font-semibold text-red-700 mb-1">Missing Skills</p>
                    <p className="text-xs text-red-800">{atsResult.missingSkills || '—'}</p>
                  </div>
                </div>
                {atsResult.suggestedSummary && (
                  <div className="rounded-lg bg-blue-50 border border-blue-100 p-2.5">
                    <p className="text-xs font-semibold text-blue-700 mb-1">Suggested Summary</p>
                    <p className="text-xs text-blue-800 whitespace-pre-wrap">{atsResult.suggestedSummary}</p>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Path selection */}
          {atsStep === 'done' && !path && (
            <div className="space-y-3">
              <p className="text-sm font-medium text-gray-700">How do you want to proceed?</p>
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => setPath('contact')}
                  className="rounded-xl border-2 border-purple-200 bg-purple-50 hover:bg-purple-100 p-4 text-left transition-colors"
                >
                  <p className="text-sm font-semibold text-purple-700">Contact Hiring Manager</p>
                  <p className="text-xs text-purple-500 mt-1">Generate an HR email or LinkedIn message</p>
                </button>
                <button
                  onClick={() => setPath('direct')}
                  className="rounded-xl border-2 border-blue-200 bg-blue-50 hover:bg-blue-100 p-4 text-left transition-colors"
                >
                  <p className="text-sm font-semibold text-blue-700">Apply Directly</p>
                  <p className="text-xs text-blue-500 mt-1">Submit your application via the job URL</p>
                </button>
              </div>
            </div>
          )}

          {/* Contact Hiring Manager */}
          {path === 'contact' && (
            <div className="space-y-4 rounded-xl border border-purple-100 bg-purple-50/30 p-4">
              <div className="flex items-center gap-2">
                <button onClick={() => { setPath(null); setGenerated(null); setGenError(null) }}
                  className="text-xs text-gray-400 hover:text-gray-600">← Back</button>
                <h3 className="text-sm font-semibold text-gray-700">Contact Hiring Manager</h3>
              </div>

              {!generated && (
                <div className="flex gap-3">
                  <button onClick={() => handleGenerate('HR_EMAIL')} disabled={generating}
                    className="flex-1 py-3 rounded-xl border-2 border-purple-200 bg-white hover:bg-purple-50 text-sm font-medium text-purple-700 disabled:opacity-50 transition-colors">
                    {generating ? 'Generating…' : 'Generate HR Email'}
                  </button>
                  <button onClick={() => handleGenerate('LINKEDIN')} disabled={generating}
                    className="flex-1 py-3 rounded-xl border-2 border-blue-200 bg-white hover:bg-blue-50 text-sm font-medium text-blue-700 disabled:opacity-50 transition-colors">
                    {generating ? 'Generating…' : 'LinkedIn Message'}
                  </button>
                </div>
              )}

              {genError && <p className="text-xs text-red-600">{genError}</p>}

              {generated && (
                <div className="rounded-xl border border-purple-200 bg-white p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-purple-700">
                      {generated.type === 'HR_EMAIL' ? 'HR Email' : 'LinkedIn Message'}
                    </span>
                    <div className="flex gap-3">
                      <button onClick={() => handleCopy(generated.content)}
                        className="text-xs text-purple-600 hover:text-purple-800 font-medium">
                        {copied ? 'Copied!' : 'Copy'}
                      </button>
                      <button onClick={() => { setGenerated(null); setGenError(null) }}
                        className="text-xs text-gray-400 hover:text-gray-600">
                        Try another
                      </button>
                    </div>
                  </div>
                  <p className="text-xs text-gray-700 whitespace-pre-wrap leading-relaxed">{generated.content}</p>
                </div>
              )}
            </div>
          )}

          {/* Apply Directly */}
          {path === 'direct' && (
            <div className="space-y-4 rounded-xl border border-blue-100 bg-blue-50/30 p-4">
              <div className="flex items-center gap-2">
                <button onClick={goBackFromDirect} className="text-xs text-gray-400 hover:text-gray-600">← Back</button>
                <h3 className="text-sm font-semibold text-gray-700">Apply Directly</h3>
              </div>

              {/* Step 1: choose cover letter or skip */}
              {!directStep && (
                <div className="space-y-3">
                  <p className="text-sm text-gray-600">Would you like to generate a cover letter before applying?</p>
                  <div className="grid grid-cols-2 gap-3">
                    <button
                      onClick={() => { setDirectStep('cover-letter'); handleGenerate('COVER_LETTER') }}
                      className="py-3 rounded-xl border-2 border-indigo-200 bg-white hover:bg-indigo-50 text-sm font-medium text-indigo-700 transition-colors"
                    >
                      Generate Cover Letter
                    </button>
                    <button
                      onClick={() => setDirectStep('skip')}
                      className="py-3 rounded-xl border-2 border-gray-200 bg-white hover:bg-gray-50 text-sm font-medium text-gray-600 transition-colors"
                    >
                      Skip
                    </button>
                  </div>
                </div>
              )}

              {/* Step 2a: cover letter generating / generated */}
              {directStep === 'cover-letter' && (
                <div className="space-y-3">
                  {generating && (
                    <div className="flex items-center gap-2 py-2">
                      <div className="w-4 h-4 border-2 border-indigo-400 border-t-transparent rounded-full animate-spin" />
                      <p className="text-xs text-gray-400">Generating cover letter…</p>
                    </div>
                  )}
                  {genError && <p className="text-xs text-red-600">{genError}</p>}
                  {generated && (
                    <div className="rounded-xl border border-indigo-200 bg-white p-4 space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-indigo-700">Cover Letter</span>
                        <div className="flex gap-3">
                          <button onClick={() => handleCopy(generated.content)}
                            className="text-xs text-indigo-600 hover:text-indigo-800 font-medium">
                            {copied ? 'Copied!' : 'Copy'}
                          </button>
                          <button
                            onClick={() => { setGenerated(null); setGenError(null); handleGenerate('COVER_LETTER') }}
                            className="text-xs text-gray-400 hover:text-gray-600">
                            Regenerate
                          </button>
                        </div>
                      </div>
                      <p className="text-xs text-gray-700 whitespace-pre-wrap leading-relaxed">{generated.content}</p>
                    </div>
                  )}
                  {/* Open job URL button — show once generated or if error */}
                  {(generated || genError) && job.jobUrl && (
                    <a href={job.jobUrl} target="_blank" rel="noreferrer"
                      className="block w-full py-3 bg-blue-600 text-white text-sm font-semibold rounded-xl hover:bg-blue-700 transition-colors text-center">
                      Open Job Application ↗
                    </a>
                  )}
                </div>
              )}

              {/* Step 2b: skip — just show open button */}
              {directStep === 'skip' && (
                <div className="space-y-3">
                  {job.jobUrl ? (
                    <a href={job.jobUrl} target="_blank" rel="noreferrer"
                      className="block w-full py-3 bg-blue-600 text-white text-sm font-semibold rounded-xl hover:bg-blue-700 transition-colors text-center">
                      Open Job Application ↗
                    </a>
                  ) : (
                    <p className="text-sm text-gray-400 text-center py-4">No application URL saved for this job.</p>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Mark as Applied — common button, always visible once ATS is done */}
          {atsStep === 'done' && (
            <div className="flex items-center justify-between border-t border-gray-100 pt-4">
              <button onClick={onClose} className="text-sm text-gray-400 hover:text-gray-600">
                Close without applying
              </button>
              <button
                onClick={handleMarkApplied}
                disabled={markingApplied}
                className="px-6 py-2.5 bg-green-600 text-white text-sm font-semibold rounded-xl hover:bg-green-700 disabled:opacity-50 transition-colors"
              >
                {markingApplied ? 'Marking…' : 'Mark as Applied ✓'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
