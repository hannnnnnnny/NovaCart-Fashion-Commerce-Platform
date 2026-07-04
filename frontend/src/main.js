import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import { useAuthStore } from './stores/auth'
import './assets/styles.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(i18n)
app.use(router)
app.mount('#app')

const auth = useAuthStore(pinia)

// A server-side 401 means our token is gone/expired. Reset the in-memory
// session (localStorage is already cleared by the axios interceptor) so the
// header, guards and pages stop believing we're logged in — and bounce to
// login only when the current page actually needs auth.
window.addEventListener('renova:unauthorized', () => {
  if (!auth.isAuthenticated) return
  auth.logout()
  const current = router.currentRoute.value
  if (current.meta?.requiresAuth) {
    router.replace({ name: 'login', query: { redirect: current.fullPath } })
  }
})

// Proactively validate a persisted token on boot: refreshes the cached user,
// or clears a stale session (its 401 flows through the handler above).
auth.refresh()
