import { useEffect, useState } from 'react'
import Layout from '../components/Layout'
import api from '../api/axios'

const FIELDS = [
  { key: 'name', label: 'Full Name', type: 'text', placeholder: 'Jane Doe' },
  { key: 'email', label: 'Email', type: 'email', placeholder: 'jane@example.com' },
  { key: 'phone', label: 'Phone', type: 'text', placeholder: '+61 400 000 000' },
  { key: 'linkedinUrl', label: 'LinkedIn URL', type: 'url', placeholder: 'https://linkedin.com/in/yourname' },
  { key: 'githubUrl', label: 'GitHub URL', type: 'url', placeholder: 'https://github.com/yourname' },
  { key: 'salaryExpectation', label: 'Salary Expectation', type: 'text', placeholder: '120,000–150,000 AUD' },
  { key: 'availability', label: 'Availability', type: 'text', placeholder: '2 weeks notice / Immediate' },
]

const TEXTAREAS = [
  { key: 'targetRoles', label: 'Target Roles', placeholder: 'Software Engineer, Backend Developer, …' },
  { key: 'preferredLocations', label: 'Preferred Locations', placeholder: 'Sydney, Melbourne, Remote' },
  { key: 'visaNote', label: 'Visa / Work Rights', placeholder: 'Australian PR, no sponsorship required' },
]

const TEMPLATES = [
  {
    key: 'defaultHrEmail',
    label: 'Default HR Email Template',
    placeholder: 'Write your default outreach email here. Claude will adapt this for each specific job — preserving your tone and structure while updating the role and company details.\n\nSubject: [Role] Application — [Your Name]\n\nDear Hiring Manager,\n\n...',
    rows: 8,
    hint: 'Claude adapts this for each job. Include your usual intro, key strengths, and sign-off.',
  },
  {
    key: 'defaultLinkedinMessage',
    label: 'Default LinkedIn Message Template',
    placeholder: "Write your default LinkedIn connection message here. Claude will adapt it per job.\n\nExample: Came across your posting for [role] at [company] — your work on X caught my attention. I'd love to connect and learn more about the team.",
    rows: 4,
    hint: 'Max 280 characters when sent. Claude will keep it within that limit.',
  },
]

const EMPTY = {
  name: '', email: '', phone: '', linkedinUrl: '', githubUrl: '',
  targetRoles: '', preferredLocations: '', visaNote: '',
  salaryExpectation: '', availability: '',
  defaultHrEmail: '', defaultLinkedinMessage: '',
}

export default function ProfilePage() {
  const [form, setForm] = useState(EMPTY)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.get('/api/profile')
      .then(res => setForm({ ...EMPTY, ...res.data }))
      .catch(() => setError('Failed to load profile.'))
      .finally(() => setLoading(false))
  }, [])

  const handleChange = (e) => {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
    setSaved(false)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const res = await api.put('/api/profile', form)
      setForm({ ...EMPTY, ...res.data })
      setSaved(true)
    } catch {
      setError('Failed to save profile.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center py-20 text-gray-400 text-sm">Loading…</div>
      </Layout>
    )
  }

  return (
    <Layout>
      <div className="max-w-2xl space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Profile</h1>
          <p className="text-gray-500 mt-1 text-sm">
            This information is used by AI features to personalise resumes, cover letters, and outreach.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-gray-200 p-6 space-y-5">
          {FIELDS.map(({ key, label, type, placeholder }) => (
            <div key={key}>
              <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
              <input
                type={type}
                name={key}
                value={form[key] ?? ''}
                onChange={handleChange}
                placeholder={placeholder}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          ))}

          {TEXTAREAS.map(({ key, label, placeholder }) => (
            <div key={key}>
              <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
              <textarea
                name={key}
                value={form[key] ?? ''}
                onChange={handleChange}
                placeholder={placeholder}
                rows={2}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
              />
            </div>
          ))}

          <div className="border-t border-gray-100 pt-5 space-y-5">
            <div>
              <h2 className="text-sm font-semibold text-gray-700">Message Templates</h2>
              <p className="text-xs text-gray-400 mt-0.5">Claude uses these as a base when generating outreach for specific jobs — same approach as the cover letter template.</p>
            </div>
            {TEMPLATES.map(({ key, label, placeholder, rows, hint }) => (
              <div key={key}>
                <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                {hint && <p className="text-xs text-gray-400 mb-1.5">{hint}</p>}
                <textarea
                  name={key}
                  value={form[key] ?? ''}
                  onChange={handleChange}
                  placeholder={placeholder}
                  rows={rows}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-y"
                />
              </div>
            ))}
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex items-center gap-3 pt-1">
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {saving ? 'Saving…' : 'Save Profile'}
            </button>
            {saved && <span className="text-sm text-green-600 font-medium">Saved!</span>}
          </div>
        </form>
      </div>
    </Layout>
  )
}
