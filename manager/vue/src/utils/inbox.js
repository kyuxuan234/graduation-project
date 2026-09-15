import { personName } from './person'

function item(partial) {
  return {
    tone: 'cyan',
    notifyType: 'info',
    title: '待办',
    text: '',
    path: '',
    query: {},
    ...partial
  }
}

export function buildInbox({ user, deploys = [], alarms = [] }) {
  const role = user && user.role
  const username = user && user.username
  const items = []

  if (role === 'OPS') {
    deploys.filter(row => row.status === 'PENDING').forEach(row => {
      items.push(item({
        key: 'deploy-pending-' + row.id,
        tone: 'warn',
        notifyType: 'warning',
        title: '待你审批',
        text: (personName(row.applicant) || '开发') + ' 提交了' + (row.appName || '应用') + '部署单 ' + row.deployNo + '，请审批。',
        path: '/deploy',
        query: { status: 'PENDING' }
      }))
    })
    deploys.filter(row => row.status === 'APPROVED').forEach(row => {
      items.push(item({
        key: 'deploy-approved-' + row.id,
        tone: 'cyan',
        notifyType: 'info',
        title: '待你执行',
        text: (row.appName || '应用') + '部署单 ' + row.deployNo + ' 已批准，请开始部署。',
        path: '/deploy',
        query: { status: 'APPROVED' }
      }))
    })
    deploys.filter(row => row.status === 'DEPLOYING').forEach(row => {
      items.push(item({
        key: 'deploy-doing-' + row.id,
        tone: 'warn',
        notifyType: 'warning',
        title: '部署中',
        text: (row.appName || '应用') + '部署单 ' + row.deployNo + ' 正在部署，请回填结果。',
        path: '/deploy',
        query: { status: 'DEPLOYING' }
      }))
    })
    alarms.filter(row => row.status === 'UNHANDLED').forEach(row => {
      items.push(item({
        key: 'alarm-open-' + row.id,
        tone: 'danger',
        notifyType: 'error',
        title: '待你接手',
        text: (row.appName || '应用') + '告警 ' + row.alarmNo + ' 未处理，请接手。',
        path: '/alarm',
        query: { status: 'UNHANDLED' }
      }))
    })
    alarms.filter(row => row.status === 'HANDLING' && row.handler === username).forEach(row => {
      items.push(item({
        key: 'alarm-mine-' + row.id,
        tone: 'warn',
        notifyType: 'warning',
        title: '待你解决',
        text: '你接手的' + (row.appName || '应用') + '告警 ' + row.alarmNo + ' 仍在处理中。',
        path: '/alarm',
        query: { status: 'HANDLING' }
      }))
    })
  } else if (role === 'DEV') {
    deploys.filter(row => row.applicant === username).forEach(row => {
      if (row.status === 'PENDING') {
        items.push(item({
          key: 'my-pending-' + row.id,
          title: '审批中',
          text: '你的部署单 ' + row.deployNo + '（' + (row.appName || '应用') + '）等待运维审批。',
          path: '/deploy',
          query: { status: 'PENDING' }
        }))
      } else if (row.status === 'APPROVED') {
        items.push(item({
          key: 'my-approved-' + row.id,
          tone: 'warn',
          notifyType: 'success',
          title: '已批准',
          text: '你的部署单 ' + row.deployNo + ' 已批准，等待运维执行。',
          path: '/deploy',
          query: { status: 'APPROVED' }
        }))
      } else if (row.status === 'DEPLOYING') {
        items.push(item({
          key: 'my-doing-' + row.id,
          title: '部署中',
          text: '运维正在部署你的单 ' + row.deployNo + '。',
          path: '/deploy',
          query: { status: 'DEPLOYING' }
        }))
      } else if (row.status === 'REJECTED') {
        items.push(item({
          key: 'my-reject-' + row.id,
          tone: 'danger',
          notifyType: 'error',
          title: '已驳回',
          text: '你的部署单 ' + row.deployNo + ' 已被驳回，请重新申请。',
          path: '/deploy',
          query: { status: 'REJECTED' }
        }))
      } else if (row.status === 'FAILED') {
        items.push(item({
          key: 'my-fail-' + row.id,
          tone: 'danger',
          notifyType: 'error',
          title: '发布失败',
          text: '你的部署单 ' + row.deployNo + ' 发布失败。',
          path: '/deploy',
          query: { status: 'FAILED' }
        }))
      }
    })
  }
  return sortInbox(items)
}

export function sortInbox(items) {
  const order = { danger: 0, warn: 1, cyan: 2, ok: 3 }
  return (items || []).slice().sort((a, b) => (order[a.tone] || 9) - (order[b.tone] || 9))
}
