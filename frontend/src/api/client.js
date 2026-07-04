import axios from 'axios'
import { readStorageItem, removeStorageItem } from '../utils/browserStorage'
import { DEMO_MODE, demoAdapter } from './demo'

const TOKEN_KEY = 'renova.token'
const DEFAULT_ERROR_MESSAGE = 'The request could not be completed.'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

// No backend on GitHub Pages: serve seeded data from an in-browser adapter.
if (DEMO_MODE) {
  apiClient.defaults.adapter = demoAdapter
}

apiClient.interceptors.request.use((config) => {
  const token = getStoredAuthToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else if (config.headers.Authorization) {
    delete config.headers.Authorization
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearStoredAuthToken()
      // Let the app reset in-memory auth state (Pinia) and redirect if needed.
      // Using an event avoids a client <-> store import cycle.
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('renova:unauthorized'))
      }
    }
    return Promise.reject(error)
  }
)

export function getStoredAuthToken() {
  return readStorageItem(TOKEN_KEY)
}

export function clearStoredAuthToken() {
  removeStorageItem(TOKEN_KEY)
}

export function unwrap(response) {
  return response.data?.data ?? response.data
}

export function apiError(error, fallback = DEFAULT_ERROR_MESSAGE) {
  if (!error.response) {
    return 'Cannot reach the server. Check that the backend is running.'
  }

  const data = error.response.data
  const validationMessages = validationErrorMessages(data?.errors)
  if (validationMessages.length) {
    return validationMessages.join('; ')
  }

  return data?.message || fallback
}

function validationErrorMessages(errors) {
  if (Array.isArray(errors)) {
    return errors.map((error) => fieldErrorMessage(error.field, error.message)).filter(Boolean)
  }

  if (errors && typeof errors === 'object') {
    return Object.entries(errors)
      .map(([field, message]) => fieldErrorMessage(field, message))
      .filter(Boolean)
  }

  return []
}

function fieldErrorMessage(field, message) {
  if (!message) return field || ''
  if (!field) return message
  return `${humanizeField(field)}: ${message}`
}

function humanizeField(field) {
  return String(field)
    .replace(/\[(\d+)\]/g, (_, index) => ` ${Number(index) + 1}`)
    .replace(/\./g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export { TOKEN_KEY }
export default apiClient
