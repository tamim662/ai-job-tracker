const apiKeyInput = document.getElementById('apiKey')
const backendUrlInput = document.getElementById('backendUrl')
const saveBtn = document.getElementById('saveBtn')
const saveJobBtn = document.getElementById('saveJobBtn')
const statusEl = document.getElementById('status')

function showStatus(msg, isError = false) {
  statusEl.textContent = msg
  statusEl.className = isError ? 'error' : ''
  statusEl.style.display = 'block'
  if (!isError) setTimeout(() => { statusEl.style.display = 'none' }, 3000)
}

// Load saved settings
chrome.storage.local.get(['apiKey', 'backendUrl'], (data) => {
  if (data.apiKey) apiKeyInput.value = data.apiKey
  backendUrlInput.value = data.backendUrl || 'http://localhost:8080'
})

// Save settings
saveBtn.addEventListener('click', () => {
  const key = apiKeyInput.value.trim()
  const url = backendUrlInput.value.trim() || 'http://localhost:8080'
  if (!key) { showStatus('Please enter your API key.', true); return }
  chrome.storage.local.set({ apiKey: key, backendUrl: url }, () => {
    showStatus('Settings saved!')
  })
})

// Save job from current tab
saveJobBtn.addEventListener('click', async () => {
  const { apiKey, backendUrl } = await chrome.storage.local.get(['apiKey', 'backendUrl'])
  if (!apiKey) { showStatus('Set your API key first.', true); return }

  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })

  let result
  try {
    result = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: scrapeJobFromPage,
    })
  } catch {
    showStatus('Cannot scrape this page. Make sure you are on a job listing.', true)
    return
  }

  const jobData = result?.[0]?.result
  if (!jobData || !jobData.title) {
    showStatus('Could not find job details on this page.', true)
    return
  }

  saveJobBtn.textContent = 'Saving…'
  saveJobBtn.disabled = true

  try {
    const base = (backendUrl || 'http://localhost:8080').replace(/\/$/, '')
    const res = await fetch(`${base}/api/jobs/external`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Api-Key': apiKey },
      body: JSON.stringify(jobData),
    })
    if (res.ok) {
      showStatus('Job saved to AI Job Tracker!')
    } else {
      const err = await res.json().catch(() => ({}))
      showStatus(err.error || `Error ${res.status}`, true)
    }
  } catch {
    showStatus('Could not reach backend. Is it running?', true)
  } finally {
    saveJobBtn.textContent = 'Save This Job →'
    saveJobBtn.disabled = false
  }
})

// Injected into the page — must be a standalone function (no closures over outer scope)
function scrapeJobFromPage() {
  const url = window.location.href
  const getText = (sel) => document.querySelector(sel)?.textContent?.trim() || null

  // ── JSON-LD structured data (most reliable across all sites) ──────────────
  const jsonLd = (() => {
    try {
      const scripts = document.querySelectorAll('script[type="application/ld+json"]')
      for (const s of scripts) {
        const d = JSON.parse(s.textContent)
        // Handle both direct JobPosting and @graph arrays
        const posting = d['@type'] === 'JobPosting' ? d
          : Array.isArray(d['@graph']) ? d['@graph'].find(x => x['@type'] === 'JobPosting')
          : null
        if (posting) return posting
      }
    } catch {}
    return null
  })()

  // ── Parse document.title helper (format: "Title at Company | Site") ───────
  const parseDocTitle = () => {
    // Strip leading notification count "(1) " before parsing
    const clean = document.title.replace(/^\(\d+\)\s*/, '')
    const m = clean.match(/^(.+?)\s+at\s+(.+?)\s*[|–—-]/)
    return m ? { title: m[1].trim(), company: m[2].trim() } : {}
  }

  // ── Strip HTML tags from a string ─────────────────────────────────────────
  const stripHtml = (html) => html ? html.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim() : null

  // ── Seek ──────────────────────────────────────────────────────────────────
  if (url.includes('seek.com.au')) {
    const title = getText('h1[data-automation="job-detail-title"]') || getText('h1')
    const company = getText('[data-automation="advertiser-name"]')
    const location = getText('[data-automation="job-detail-location"]')
    const salary = getText('[data-automation="job-detail-salary"]')
    const jobType = getText('[data-automation="job-detail-work-type"]')
    const descEl = document.querySelector('[data-automation="jobAdDetails"]') || document.querySelector('.job-description')
    const description = descEl?.textContent?.trim() || null
    return { title, company, location, salary, jobType, description, jobUrl: url, platform: 'Seek' }
  }

  // ── Indeed ────────────────────────────────────────────────────────────────
  if (url.includes('indeed.com')) {
    // JSON-LD first
    if (jsonLd) {
      return {
        title: jsonLd.title,
        company: jsonLd.hiringOrganization?.name,
        location: jsonLd.jobLocation?.address?.addressLocality || jsonLd.jobLocation?.address?.addressRegion,
        salary: jsonLd.baseSalary?.value?.value || null,
        description: stripHtml(jsonLd.description),
        jobUrl: url,
        platform: 'Indeed',
      }
    }
    const title = getText('h1[data-testid="jobsearch-JobInfoHeader-title"]')
      || getText('[class*="JobInfoHeader-title"]') || getText('h1')
    const company = getText('[data-testid="inlineHeader-companyName"] a')
      || getText('[data-testid="inlineHeader-companyName"]') || getText('[class*="companyName"]')
    const location = getText('[data-testid="job-location"]')
      || getText('[data-testid="inlineHeader-companyLocation"]') || getText('[class*="companyLocation"]')
    const salary = getText('[id*="salaryInfoAndJobType"]') || getText('[class*="salary"]')
    const descEl = document.querySelector('#jobDescriptionText')
      || document.querySelector('[id*="jobDescription"]') || document.querySelector('[class*="jobDescription"]')
    const description = descEl?.textContent?.trim() || null
    return { title, company, location, salary, description, jobUrl: url, platform: 'Indeed' }
  }

  // ── LinkedIn ──────────────────────────────────────────────────────────────
  if (url.includes('linkedin.com')) {
    // 1. JSON-LD structured data — most reliable
    if (jsonLd) {
      return {
        title: jsonLd.title,
        company: jsonLd.hiringOrganization?.name,
        location: jsonLd.jobLocation?.address?.addressLocality
          || jsonLd.jobLocation?.address?.addressRegion
          || jsonLd.jobLocation?.address?.addressCountry,
        description: stripHtml(jsonLd.description),
        jobUrl: url,
        platform: 'LinkedIn',
      }
    }

    // 2. document.title: "Senior Engineer at Acme | LinkedIn"
    const fromTitle = parseDocTitle()

    // 3. Open Graph meta tags
    const ogTitle = document.querySelector('meta[property="og:title"]')?.getAttribute('content') || ''
    const ogMatch = ogTitle.match(/^(.+?)\s+at\s+(.+?)$/)
    const titleFromOg = ogMatch ? ogMatch[1].trim() : null
    const companyFromOg = ogMatch ? ogMatch[2].trim() : null
    const descFromOg = document.querySelector('meta[property="og:description"]')?.getAttribute('content') || null
    const descFromTwitter = document.querySelector('meta[name="twitter:description"]')?.getAttribute('content') || null

    // 4. DOM selectors — broad fallbacks using attribute contains
    const title = fromTitle.title || titleFromOg
      || getText('[class*="job-title"] h1') || getText('[class*="top-card"] h1') || getText('h1')

    const company = fromTitle.company || companyFromOg
      || getText('[class*="company-name"] a') || getText('[class*="company-name"]')
      || getText('[class*="topcard__org"]')

    const location = getText('[class*="primary-description"] [class*="bullet"]')
      || getText('[class*="topcard__flavor--bullet"]')
      || getText('[class*="workplace-type"]')

    // 5. Description — DOM first (full text), then meta as fallback
    const descEl = document.querySelector('[class*="description__content"] [class*="html-content"]')
      || document.querySelector('[class*="description-content__text"]')
      || document.querySelector('[class*="description__content"]')
      || document.querySelector('[class*="description__text"]')
    const description = descEl?.textContent?.trim() || descFromOg || descFromTwitter

    return { title, company, location, description, jobUrl: url, platform: 'LinkedIn' }
  }

  // ── Generic fallback ──────────────────────────────────────────────────────
  const fromTitle = parseDocTitle()
  return {
    title: fromTitle.title || getText('h1'),
    company: fromTitle.company || null,
    jobUrl: url,
  }
}
