export const ROLE_NAMES = {
  ADMIN: '超级管理员',
  DEV: '开发',
  OPS: '运维',
}

export const ROLE_TAG = {
  ADMIN: '',
  DEV: 'success',
  OPS: 'warning',
}

const KNOWN = {
  admin: 'ADMIN',
  dev1: 'DEV',
  dev2: 'DEV',
  ops1: 'OPS',
  ops2: 'OPS',
  张伟: 'DEV',
  李娜: 'DEV',
  王强: 'OPS',
  赵敏: 'OPS',
}

const NAME_MAP = {
  admin: '超级管理员',
  dev1: '张伟',
  dev2: '李娜',
  ops1: '王强',
  ops2: '赵敏',
}

export function rememberPerson(username, name) {
  if (username && name) NAME_MAP[username] = name
}

export function rememberPersons(list) {
  (list || []).forEach(item => rememberPerson(item.username, item.name))
}

export function personName(value) {
  if (!value) return ''
  const key = String(value)
  return NAME_MAP[key] || NAME_MAP[key.toLowerCase()] || key
}

export function personRole(name, apiRole) {
  if (apiRole) return apiRole
  if (!name) return ''
  if (KNOWN[name]) return KNOWN[name]
  const text = String(name)
  if (/^dev/i.test(text)) return 'DEV'
  if (/^ops/i.test(text)) return 'OPS'
  if (/^admin/i.test(text)) return 'ADMIN'
  return ''
}

export function personRoleName(name, apiRole) {
  return ROLE_NAMES[personRole(name, apiRole)] || ''
}

export function roleTag(name, apiRole) {
  return ROLE_TAG[personRole(name, apiRole)] || 'info'
}

export function isAdmin(user) {
  return !!(user && user.role === 'ADMIN')
}

export function canDev(user) {
  return !!(user && (user.role === 'DEV' || user.role === 'ADMIN'))
}

export function canOps(user) {
  return !!(user && (user.role === 'OPS' || user.role === 'ADMIN'))
}
