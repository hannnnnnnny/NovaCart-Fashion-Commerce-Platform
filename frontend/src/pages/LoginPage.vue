<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { apiError } from '../api/client'
import { DEMO_MODE } from '../api/demo'

const { t } = useI18n()
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const toast = useToastStore()

const email = ref('')
const password = ref('')
const submitting = ref(false)

async function submit() {
  submitting.value = true
  try {
    await auth.login({ email: email.value.trim(), password: password.value })
    toast.success(t('auth.signIn'))
    const redirect = route.query.redirect ? String(route.query.redirect) : '/'
    router.push(redirect)
  } catch (err) { toast.error(apiError(err)) } finally { submitting.value = false }
}

function tryDemo(account) {
  email.value = account
  password.value = 'DemoPassword1!'
}
</script>

<template>
  <main class="auth-shell">
    <div class="auth-card">
      <h1 style="margin-bottom: 8px">{{ t('auth.signIn') }}</h1>
      <p class="muted" style="margin-bottom: 24px">{{ t('auth.signInSubtitle') }}</p>
      <form @submit.prevent="submit">
        <div class="field">
          <label class="label">{{ t('auth.email') }}</label>
          <input class="input" type="email" v-model="email" required />
        </div>
        <div class="field">
          <label class="label">{{ t('auth.password') }}</label>
          <input class="input" type="password" v-model="password" required />
        </div>
        <button class="btn btn-primary btn-lg btn-block" :disabled="submitting" type="submit">{{ submitting ? t('common.loading') : t('common.login') }}</button>
      </form>

      <template v-if="DEMO_MODE">
        <div class="divider"></div>
        <p class="muted text-center" style="font-size: 13px; margin-bottom: 8px">Demo accounts (password <code>DemoPassword1!</code>)</p>
        <div class="row-wrap" style="justify-content: center; gap: 6px">
          <button class="btn btn-ghost btn-sm" type="button" @click="tryDemo('ava@renova.local')">ava@renova.local</button>
          <button class="btn btn-ghost btn-sm" type="button" @click="tryDemo('liam@renova.local')">liam@renova.local</button>
        </div>
      </template>

      <p class="text-center muted" style="margin-top: 20px; font-size: 13px">{{ t('auth.noAccount') }} <RouterLink :to="{ name: 'signup' }" class="bold" style="color: var(--primary-strong)">{{ t('auth.createAccount') }}</RouterLink></p>
    </div>
  </main>
</template>
