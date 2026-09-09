<script setup>
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="footer">
    <div class="container">
      <div class="grid grid-3" style="gap: 32px;">
        <div>
          <div class="brand" style="color: #fff; margin-bottom: 8px">
            <span class="brand-mark">R</span>
            <span>ReNova</span>
          </div>
          <p style="opacity:.7; font-size: 14px">{{ t('brand.tagline') }}</p>
        </div>
        <div>
          <h4 style="color:#fff; font-size:14px; margin-bottom: 12px;">ReNova</h4>
          <div class="stack-sm" style="font-size:14px">
            <div><RouterLink :to="{ name: 'browse' }">{{ t('common.browse') }}</RouterLink></div>
            <div><RouterLink :to="{ name: 'post-listing' }">{{ t('common.sell') }}</RouterLink></div>
            <div><RouterLink :to="{ name: 'orders' }">{{ t('common.orders') }}</RouterLink></div>
          </div>
        </div>
        <div>
          <h4 style="color:#fff; font-size:14px; margin-bottom: 12px;">Demo</h4>
          <p style="font-size:13px; opacity:.7">ReNova is a portfolio demo. Payments and shipping are simulated. Create an account to try it.</p>
        </div>
      </div>
      <div class="divider" style="border-color:#3a3633"></div>
      <p style="font-size: 13px; opacity: .6; margin: 0">© {{ year }} ReNova — Built for portfolio demonstration.</p>
    </div>
  </footer>
</template>
