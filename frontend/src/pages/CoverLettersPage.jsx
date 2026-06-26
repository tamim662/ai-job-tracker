import { useEffect, useRef, useState } from 'react'
import Layout from '../components/Layout'
import api from '../api/axios'

export default function CoverLettersPage() {
  const [coverLetter, setCoverLetter] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState(null)
  const inputRef = useRef(null)

  const load = () =>
    api.get('/api/cover-letters')
      .then(res => setCoverLetter(res.data[0] ?? null))
      .catch(() => setError('Failed to load cover letter.'))

  useEffect(() => { load() }, [])

  const handleFiles = async (files) => {
    const file = files[0]
    if (!file) return
    const ext = file.name.split('.').pop().toLowerCase()
    if (!['pdf', 'docx'].includes(ext)) {
      setError('Only PDF and DOCX files are supported.')
      return
    }
    setUploading(true)
    setError(null)
    const form = new FormData()
    form.append('file', file)
    try {
      await api.post('/api/cover-letters', form)
      await load()
    } catch (e) {
      setError(e.response?.data?.error || 'Upload failed.')
    } finally {
      setUploading(false)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  const handleDelete = async () => {
    if (!coverLetter || !window.confirm('Remove this cover letter?')) return
    try {
      await api.delete(`/api/cover-letters/${coverLetter.id}`)
      setCoverLetter(null)
    } catch {
      setError('Failed to delete cover letter.')
    }
  }

  const handleDrop = (e) => {
    e.preventDefault()
    handleFiles(e.dataTransfer.files)
  }

  return (
    <Layout>
      <div className="max-w-2xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Cover Letter</h1>
          <p className="text-gray-500 mt-1 text-sm">
            One cover letter at a time. When generating cover letters for specific jobs, Claude uses this as a
            base template — preserving your personal details, visa status, and writing style.
          </p>
        </div>

        {coverLetter ? (
          <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-purple-50 border border-purple-100 flex items-center justify-center shrink-0">
                  <span className="text-xs font-bold text-purple-600">
                    {coverLetter.fileName.split('.').pop().toUpperCase()}
                  </span>
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-800">{coverLetter.fileName}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    Uploaded {new Date(coverLetter.createdAt).toLocaleDateString('en-AU', { day: 'numeric', month: 'short', year: 'numeric' })}
                    {coverLetter.parsedText ? ` · ${coverLetter.parsedText.split(/\s+/).length} words extracted` : ''}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <button onClick={() => inputRef.current?.click()}
                  className="px-3 py-1.5 text-xs font-medium text-purple-600 border border-purple-200 rounded-lg hover:bg-purple-50 transition-colors">
                  Replace
                </button>
                <button onClick={handleDelete}
                  className="text-xs text-red-500 hover:text-red-700 transition-colors">
                  Remove
                </button>
              </div>
            </div>

            {coverLetter.parsedText && (
              <div className="rounded-lg bg-gray-50 border border-gray-100 p-3">
                <p className="text-xs font-medium text-gray-500 mb-1">Preview (first 300 characters)</p>
                <p className="text-xs text-gray-600 leading-relaxed">
                  {coverLetter.parsedText.slice(0, 300)}{coverLetter.parsedText.length > 300 ? '…' : ''}
                </p>
              </div>
            )}
          </div>
        ) : (
          <div
            onDrop={handleDrop}
            onDragOver={e => e.preventDefault()}
            onClick={() => inputRef.current?.click()}
            className="border-2 border-dashed border-gray-300 rounded-xl p-10 text-center cursor-pointer hover:border-purple-400 hover:bg-purple-50 transition-colors"
          >
            {uploading ? (
              <p className="text-sm text-purple-600 font-medium">Uploading and parsing…</p>
            ) : (
              <>
                <p className="text-sm font-medium text-gray-700">Drop your cover letter here or click to browse</p>
                <p className="text-xs text-gray-400 mt-1">PDF or DOCX · max 20 MB</p>
              </>
            )}
          </div>
        )}

        <input
          ref={inputRef}
          type="file"
          accept=".pdf,.docx"
          className="hidden"
          onChange={e => handleFiles(e.target.files)}
        />

        {uploading && coverLetter && (
          <p className="text-sm text-purple-600 font-medium text-center">Uploading and replacing…</p>
        )}

        {error && <p className="text-sm text-red-600">{error}</p>}
      </div>
    </Layout>
  )
}
