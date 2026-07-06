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

  // ── Seek ──────────────────────────────────────────────────────────────────
  if (url.includes('seek.com.au')) {
    const title = document.querySelector('h1[data-automation="job-detail-title"]')?.innerText?.trim()
      || document.querySelector('h1')?.innerText?.trim()

    const company = document.querySelector('[data-automation="advertiser-name"]')?.innerText?.trim()
      || document.querySelector('[data-automation="job-detail-work-type"]')?.closest('section')
         ?.querySelector('span')?.innerText?.trim()

    const location = document.querySelector('[data-automation="job-detail-location"]')?.innerText?.trim()

    const salary = document.querySelector('[data-automation="job-detail-salary"]')?.innerText?.trim()

    const jobType = document.querySelector('[data-automation="job-detail-work-type"]')?.innerText?.trim()

    const descEl = document.querySelector('[data-automation="jobAdDetails"]')
      || document.querySelector('.job-description')
    const description = descEl?.innerText?.trim()

    return { title, company, location, salary, jobType, description, jobUrl: url, platform: 'Seek' }
  }

  // ── Indeed ────────────────────────────────────────────────────────────────
  if (url.includes('indeed.com')) {
    const getText = (sel) => document.querySelector(sel)?.textContent?.trim()

    const title = getText('h1[data-testid="jobsearch-JobInfoHeader-title"]')
      || getText('h1.jobsearch-JobInfoHeader-title')
      || getText('[class*="JobInfoHeader-title"]')
      || getText('h1')

    const company = getText('[data-testid="inlineHeader-companyName"] a')
      || getText('[data-testid="inlineHeader-companyName"]')
      || getText('[class*="companyName"] a')
      || getText('[class*="companyName"]')

    const location = getText('[data-testid="job-location"]')
      || getText('[data-testid="inlineHeader-companyLocation"]')
      || getText('[class*="companyLocation"]')

    const salary = getText('[id*="salaryInfoAndJobType"]')
      || getText('[data-testid="attribute_snippet_testid"]')
      || getText('[class*="salary"]')

    const descEl = document.querySelector('#jobDescriptionText')
      || document.querySelector('[id*="jobDescription"]')
      || document.querySelector('[class*="jobDescription"]')
    const description = descEl?.textContent?.trim()

    return { title, company, location, salary, description, jobUrl: url, platform: 'Indeed' }
  }

  // ── LinkedIn ──────────────────────────────────────────────────────────────
  if (url.includes('linkedin.com')) {
    const getText = (sel) => document.querySelector(sel)?.textContent?.trim()

    // Title — LinkedIn uses various class combos; try specific then broad
    const title = getText('.job-details-jobs-unified-top-card__job-title h1')
      || getText('h1.job-details-jobs-unified-top-card__job-title')
      || getText('.jobs-unified-top-card__job-title h1')
      || getText('h1.t-24')
      || getText('h1[class*="job"]')
      || getText('.top-card-layout__title')
      || getText('[class*="job-title"] h1')
      || getText('[class*="job-title"]')
      || getText('h1')

    // Company
    const company = getText('.job-details-jobs-unified-top-card__company-name a')
      || getText('.job-details-jobs-unified-top-card__company-name')
      || getText('.jobs-unified-top-card__company-name a')
      || getText('.topcard__org-name-link')
      || getText('[class*="company-name"] a')
      || getText('[class*="company-name"]')

    // Location — usually the first bullet after company
    const location = getText('.job-details-jobs-unified-top-card__primary-description-without-tagline')
      || getText('.job-details-jobs-unified-top-card__bullet')
      || getText('.jobs-unified-top-card__bullet')
      || getText('.topcard__flavor--bullet')
      || getText('[class*="workplace-type"]')

    // Description
    const descEl = document.querySelector('.jobs-description__content .jobs-box__html-content')
      || document.querySelector('.jobs-description-content__text')
      || document.querySelector('.jobs-description__content')
      || document.querySelector('.description__text--rich')
      || document.querySelector('[class*="description__content"]')
      || document.querySelector('[class*="job-description"]')
    const description = descEl?.textContent?.trim()

    return { title, company, location, description, jobUrl: url, platform: 'LinkedIn' }
  }

  // ── Generic fallback ──────────────────────────────────────────────────────
  const title = document.querySelector('h1')?.innerText?.trim()
  return { title, jobUrl: url }
}
