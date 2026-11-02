// 开发环境由 .env.development 指定后端地址。
// 生产构建未配置时，按当前页面的主机名访问 9090，避免写死 localhost。
export function apiBaseUrl() {
  const configured = (process.env.VUE_APP_API_BASE_URL || '').trim()
  if (configured) {
    return configured.replace(/\/$/, '')
  }
  if (typeof window === 'undefined') {
    return ''
  }
  return window.location.protocol + '//' + window.location.hostname + ':9090'
}

export function wsBaseUrl() {
  return apiBaseUrl().replace(/^http/i, 'ws')
}
