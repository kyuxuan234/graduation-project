import defaultAvatar from '@/assets/imgs/admin-avatar.png'

const UUID_FILE = /\/files\/[a-f0-9-]{32,36}\.[a-z0-9]+$/i

export function resolveAvatar(url) {
  if (url && UUID_FILE.test(url)) {
    return url
  }
  return defaultAvatar
}

export { defaultAvatar }
