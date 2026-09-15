<template>
  <span class="os-logo" :title="kindLabel">
    <!-- Ubuntu -->
    <svg v-if="kind === 'ubuntu'" viewBox="0 0 32 32" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#E95420"/>
      <circle cx="16" cy="16" r="6.2" fill="none" stroke="#fff" stroke-width="2.2"/>
      <circle cx="16" cy="5.6" r="3.1" fill="#fff"/>
      <circle cx="7" cy="21.2" r="3.1" fill="#fff"/>
      <circle cx="25" cy="21.2" r="3.1" fill="#fff"/>
    </svg>
    <!-- CentOS -->
    <svg v-else-if="kind === 'centos'" viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="6" fill="#F5F6F8"/>
      <polygon points="16,2 21.2,9.6 16,12.4 10.8,9.6" fill="#932279"/>
      <polygon points="30,16 22.4,21.2 19.6,16 22.4,10.8" fill="#262577"/>
      <polygon points="16,30 10.8,22.4 16,19.6 21.2,22.4" fill="#4A7729"/>
      <polygon points="2,16 9.6,10.8 12.4,16 9.6,21.2" fill="#1A87B1"/>
    </svg>
    <!-- Windows -->
    <svg v-else-if="kind === 'windows'" viewBox="0 0 32 32" aria-hidden="true">
      <path fill="#00A4EF" d="M3 6.4l12.4-1.7v13H3V6.4zm13.6-1.85L29 2.6v15.1H16.6V4.55zM3 18.7h12.4v13.1L3 30.1V18.7zm13.6 0H29V32l-12.4-1.9V18.7z"/>
    </svg>
    <!-- Debian -->
    <svg v-else-if="kind === 'debian'" viewBox="0 0 32 32" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#A80030"/>
      <path fill="#fff" d="M17.8 6.2c3.6.4 6.4 3.2 6.6 6.8.2 2.6-1 4.9-3.2 6.3l.9 1.7c3.2-1.9 5-5.2 4.7-8.7C26.4 7.4 22.4 3.8 17.4 3.4c-5.6-.4-10.6 3.4-11.2 9-.4 3.6 1.1 7 4 9.1l1.1-1.6c-2.3-1.6-3.5-4.3-3.2-7.2.5-4.3 4.4-7.3 8.7-6.9.4 0 .7 0 1 0z"/>
    </svg>
    <!-- Red Hat / RHEL -->
    <svg v-else-if="kind === 'redhat'" viewBox="0 0 32 32" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#EE0000"/>
      <path fill="#fff" d="M7 18.6c1.8-3.8 5.6-6.4 9.8-6.2 2.2.1 3.6.9 4.6 1.4 1.4.7 2.2 1 3.4.4.4-.2.8 0 .9.4.2.6-.2 1.1-.8 1.3-1.8.7-3.3.2-4.8-.5-1-.5-2.2-1.1-3.8-1.1-3.2-.1-6.2 2-7.6 5-.3.6-.9.6-1.3.3-.5-.3-.6-.8-.4-1zM8.6 21.4c.8-1.5 2.3-2.6 4-2.8 1.8-.2 3.2.6 4.6 1.4 1.4.8 2.8 1.5 4.6 1.3 1.4-.2 2.6-1 3.4-2.1.4-.5 1-.4 1.3 0 .4.4.3 1-.1 1.4-1.1 1.5-2.8 2.6-4.7 2.8-2.2.3-4-.6-5.5-1.5-1.3-.7-2.4-1.3-3.8-1.1-1.1.1-2.1.8-2.6 1.8-.3.5-.9.6-1.3.2-.4-.3-.4-.9-.1-1.4z"/>
    </svg>
    <!-- macOS -->
    <svg v-else-if="kind === 'macos'" viewBox="0 0 32 32" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#111"/>
      <path fill="#fff" d="M20.6 9.4c-1 .1-2.2.7-2.9 1.5-.6.7-1.2 1.8-1 2.9 1.1.1 2.3-.6 3-1.4.7-.8 1.2-1.9 0-3zm-1.1 4.8c-1.6-.1-3 .9-3.8.9s-2-.9-3.3-.8c-1.7.1-3.3 1-4.2 2.5-1.8 3.1-.5 7.7 1.3 10.2.9 1.2 1.9 2.6 3.3 2.5 1.3 0 1.8-.8 3.4-.8s2 .8 3.4.8 2.2-1.3 3.1-2.6c1-.1 1.8-2.4 2.5-3.6-2.1-.8-2.5-3.9-.4-5.1-1.2-1.5-3-2.1-5.3-2z"/>
    </svg>
    <!-- generic Linux -->
    <svg v-else viewBox="0 0 32 32" aria-hidden="true">
      <circle cx="16" cy="16" r="16" fill="#333"/>
      <ellipse cx="16" cy="13.2" rx="6.2" ry="7" fill="#F6D258"/>
      <ellipse cx="13.6" cy="12.4" rx="1.3" ry="1.6" fill="#222"/>
      <ellipse cx="18.4" cy="12.4" rx="1.3" ry="1.6" fill="#222"/>
      <ellipse cx="16" cy="16.6" rx="2.1" ry="1.3" fill="#222"/>
      <path fill="#F6D258" d="M10.6 20.4c1.4 3.4 3.2 5.2 5.4 5.2s4-1.8 5.4-5.2c-1.6 1.1-3.4 1.7-5.4 1.7s-3.8-.6-5.4-1.7z"/>
    </svg>
  </span>
</template>

<script>
export default {
  name: 'OsLogo',
  props: {
    os: { type: String, default: '' }
  },
  computed: {
    kind() {
      const s = (this.os || '').toLowerCase()
      if (s.includes('ubuntu')) return 'ubuntu'
      if (s.includes('centos')) return 'centos'
      if (s.includes('windows') || s.includes('win server') || s.includes('win10') || s.includes('win11')) return 'windows'
      if (s.includes('debian')) return 'debian'
      if (s.includes('redhat') || s.includes('red hat') || s.includes('rhel')) return 'redhat'
      if (s.includes('macos') || s.includes('mac os') || s.includes('darwin')) return 'macos'
      return 'linux'
    },
    kindLabel() {
      return this.os || 'Linux'
    }
  }
}
</script>

<style scoped>
.os-logo {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 6px;
  vertical-align: middle;
}
.os-logo svg {
  width: 18px;
  height: 18px;
  display: block;
  border-radius: 4px;
}
</style>
